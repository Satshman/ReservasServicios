package co.reservas.adapters.in.web.error;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private final GlobalExceptionHandler manejador =
            new GlobalExceptionHandler(new FabricaErrores(Clock.fixed(AHORA, ZoneOffset.UTC)));

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @SuppressWarnings("unused")
    void metodoDePrueba(String valor) {
        // Solo aporta un MethodParameter para construir excepciones de validación.
    }

    private static MethodParameter parametro() throws NoSuchMethodException {
        return new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDePrueba", String.class), 0);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CodigoError.class)
    @DisplayName("Dado cualquier código de error del contrato, cuando se traduce a HTTP, entonces tiene un estado 4xx o 5xx")
    void todosLosCodigosTienenEstado(CodigoError codigo) {
        // Arrange - Act
        HttpStatus estado = CodigosHttp.estado(codigo);

        // Assert
        assertThat(estado.isError()).isTrue();
    }

    @Test
    @DisplayName("Dado una excepción de negocio con detalles, cuando se maneja, entonces responde su estado con el cuerpo uniforme y el traceId del MDC")
    void excepcionDeNegocio() {
        // Arrange
        MDC.put(FabricaErrores.CLAVE_TRACE_ID, "traza-123");
        ExcepcionNegocio excepcion = new ExcepcionNegocio(CodigoError.RECURSO_NO_DISPONIBLE, "Ocupado",
                List.of(Map.of("tipo", "RECURSO")));

        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.negocio(excepcion);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse(CodigoError.RECURSO_NO_DISPONIBLE, "Ocupado",
                List.of(Map.of("tipo", "RECURSO")), "traza-123", AHORA));
    }

    @Test
    @DisplayName("Dado errores de Bean Validation en el cuerpo, cuando se maneja, entonces responde 400 VALIDACION_FALLIDA con campos ordenados")
    void validacionDelCuerpo() throws Exception {
        // Arrange
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "registro");
        resultado.addError(new FieldError("registro", "password", "tamaño inválido"));
        resultado.addError(new FieldError("registro", "email", "formato inválido"));
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(parametro(), resultado);

        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.validacion(excepcion);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().details()).containsExactly(new DetalleCampo("email", "formato inválido"),
                new DetalleCampo("password", "tamaño inválido"));
    }

    @Test
    @DisplayName("Dado errores de validación de parámetros o un parámetro faltante, cuando se manejan, entonces responden 400 VALIDACION_FALLIDA con el nombre del parámetro")
    void validacionDeParametros() throws Exception {
        // Arrange
        ParameterValidationResult resultado = mock(ParameterValidationResult.class);
        when(resultado.getMethodParameter()).thenReturn(parametro());
        when(resultado.getResolvableErrors())
                .thenReturn(List.of(new DefaultMessageSourceResolvable(null, null, "debe ser positivo")));
        HandlerMethodValidationException excepcion = mock(HandlerMethodValidationException.class);
        when(excepcion.getParameterValidationResults()).thenReturn(List.of(resultado));

        // Act
        ResponseEntity<ErrorResponse> porValidacion = manejador.validacionParametros(excepcion);
        ResponseEntity<ErrorResponse> porFaltante =
                manejador.parametroFaltante(new MissingServletRequestParameterException("desde", "LocalDate"));

        // Assert
        assertThat(porValidacion.getBody().errorCode()).isEqualTo(CodigoError.VALIDACION_FALLIDA);
        assertThat(porValidacion.getBody().details()).hasSize(1);
        assertThat(porFaltante.getBody().details()).containsExactly(new DetalleCampo("desde", "Es obligatorio"));
    }

    @Test
    @DisplayName("Dado errores de la capa HTTP, cuando se manejan, entonces usan 400, 404, 405, 415 y 500 sin exponer detalles internos")
    void erroresHttp() {
        // Arrange - Act
        ResponseEntity<ErrorResponse> ilegible = manejador.solicitudInvalida(
                new HttpMessageNotReadableException("JSON inválido", new MockHttpInputMessage(new byte[0])));
        ResponseEntity<ErrorResponse> ruta = manejador.rutaNoEncontrada(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/nada", "nada"));
        ResponseEntity<ErrorResponse> metodo = manejador.metodoNoPermitido(
                new HttpRequestMethodNotSupportedException("PATCH"));
        ResponseEntity<ErrorResponse> tipo = manejador.tipoNoSoportado(
                new HttpMediaTypeNotSupportedException("text/plain"));
        ResponseEntity<ErrorResponse> interno = manejador.inesperado(new IllegalStateException("detalle secreto"));

        // Assert
        assertThat(ilegible.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ilegible.getBody().errorCode()).isEqualTo(CodigoError.SOLICITUD_INVALIDA);
        assertThat(ruta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(metodo.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(tipo.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(interno.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(interno.getBody().message()).doesNotContain("detalle secreto");
    }
}

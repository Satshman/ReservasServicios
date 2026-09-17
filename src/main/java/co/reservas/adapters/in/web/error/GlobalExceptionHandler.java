package co.reservas.adapters.in.web.error;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DATOS_INVALIDOS = "La solicitud contiene datos inválidos.";

    private final FabricaErrores errores;

    public GlobalExceptionHandler(FabricaErrores errores) {
        this.errores = errores;
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<ErrorResponse> negocio(ExcepcionNegocio e) {
        return responder(e.getCodigo(), e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        List<DetalleCampo> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new DetalleCampo(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(DetalleCampo::campo).thenComparing(DetalleCampo::mensaje))
                .toList();
        return responder(CodigoError.VALIDACION_FALLIDA, DATOS_INVALIDOS, detalles);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException e) {
        List<DetalleCampo> detalles = e.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> new DetalleCampo(resultado.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return responder(CodigoError.VALIDACION_FALLIDA, DATOS_INVALIDOS, detalles);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(CodigoError.VALIDACION_FALLIDA, DATOS_INVALIDOS,
                List.of(new DetalleCampo(e.getParameterName(), "Es obligatorio")));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(Exception e) {
        return responder(CodigoError.SOLICITUD_INVALIDA,
                "La solicitud no se pudo interpretar. Revisa el formato JSON y los tipos de datos.", List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(errores.crear(CodigoError.SOLICITUD_INVALIDA, "El tipo de contenido no es compatible.",
                        List.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaNoEncontrada(NoResourceFoundException e) {
        return responder(CodigoError.RUTA_NO_ENCONTRADA, "La ruta solicitada no existe.", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return responder(CodigoError.METODO_NO_PERMITIDO, "El método HTTP no está permitido para esta ruta.",
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(CodigoError.ERROR_INTERNO, "Ocurrió un error interno. Intenta de nuevo más tarde.",
                List.of());
    }

    private ResponseEntity<ErrorResponse> responder(CodigoError codigo, String mensaje, List<?> detalles) {
        return ResponseEntity.status(CodigosHttp.estado(codigo)).body(errores.crear(codigo, mensaje, detalles));
    }
}

package co.reservas.infrastructure.observabilidad;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import io.micrometer.common.KeyValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Observabilidad - etiqueta codigo_error y acceso de Prometheus")
class ConvencionMetricasHttpTest {

    private final ConvencionMetricasHttp convencion = new ConvencionMetricasHttp();

    private static ServerRequestObservationContext contexto(MockHttpServletRequest solicitud) {
        return new ServerRequestObservationContext(solicitud, new MockHttpServletResponse());
    }

    private String etiqueta(ServerRequestObservationContext contexto) {
        return convencion.getLowCardinalityKeyValues(contexto).stream()
                .filter(kv -> kv.getKey().equals(ConvencionMetricasHttp.ETIQUETA))
                .map(KeyValue::getValue)
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Dado una regla de negocio que rechazó la petición, cuando se etiqueta, entonces usa su CodigoError")
    void usaElCodigoDeLaExcepcionDeNegocio() {
        // Arrange
        ServerRequestObservationContext contexto = contexto(new MockHttpServletRequest("POST", "/api/v1/reservas"));
        contexto.setError(new ExcepcionNegocio(CodigoError.TURNO_SIN_CUPO, "sin cupo"));

        // Act - Assert
        assertThat(etiqueta(contexto)).isEqualTo("TURNO_SIN_CUPO");
    }

    @Test
    @DisplayName("Dado una excepción resuelta por el manejador global, cuando se etiqueta, entonces la lee del atributo de la petición")
    void leeLaExcepcionResueltaDelAtributo() {
        // Arrange
        MockHttpServletRequest solicitud = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        solicitud.setAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE,
                new ExcepcionNegocio(CodigoError.CUENTA_BLOQUEADA, "bloqueada"));

        // Act - Assert
        assertThat(etiqueta(contexto(solicitud))).isEqualTo("CUENTA_BLOQUEADA");
    }

    @Test
    @DisplayName("Dado una petición exitosa o un error técnico, cuando se etiqueta, entonces usa NINGUNO")
    void sinCodigoDeNegocio() {
        // Arrange
        ServerRequestObservationContext exitosa = contexto(new MockHttpServletRequest("GET", "/api/v1/servicios"));
        ServerRequestObservationContext tecnico = contexto(new MockHttpServletRequest("GET", "/api/v1/servicios"));
        tecnico.setError(new IllegalStateException("fallo"));

        // Act - Assert
        assertThat(etiqueta(exitosa)).isEqualTo(ConvencionMetricasHttp.SIN_CODIGO);
        assertThat(etiqueta(tecnico)).isEqualTo(ConvencionMetricasHttp.SIN_CODIGO);
    }

    @Test
    @DisplayName("Dado el puerto de gestión publicado, cuando llega una petición, entonces solo se permite /actuator/prometheus por ese puerto")
    void matcherSoloAceptaPrometheusPorElPuertoDeGestion() {
        // Arrange
        MockEnvironment entorno = new MockEnvironment().withProperty(SeguridadMetricasConfig.PROPIEDAD_PUERTO_GESTION,
                "8091");
        RequestMatcher matcher = SeguridadMetricasConfig.prometheusPorPuertoDeGestion(entorno);
        MockHttpServletRequest porGestion = new MockHttpServletRequest("GET", "/actuator/prometheus");
        porGestion.setLocalPort(8091);
        MockHttpServletRequest conContexto = new MockHttpServletRequest("GET", "/app/actuator/prometheus");
        conContexto.setContextPath("/app");
        conContexto.setLocalPort(8091);
        MockHttpServletRequest porApi = new MockHttpServletRequest("GET", "/actuator/prometheus");
        porApi.setLocalPort(8080);
        MockHttpServletRequest otraRuta = new MockHttpServletRequest("GET", "/actuator/env");
        otraRuta.setLocalPort(8091);

        // Act - Assert
        assertThat(matcher.matches(porGestion)).isTrue();
        assertThat(matcher.matches(conContexto)).isTrue();
        assertThat(matcher.matches(porApi)).isFalse();
        assertThat(matcher.matches(otraRuta)).isFalse();
        assertThat(SeguridadMetricasConfig.prometheusPorPuertoDeGestion(new StandardEnvironment())
                .matches(porGestion)).isFalse();
    }
}

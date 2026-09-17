package co.reservas.infrastructure;

import co.reservas.infrastructure.web.TraceIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InfraestructuraTest {

    @Test
    @DisplayName("Dado un secreto JWT de menos de 32 bytes, cuando se construye la clave, entonces el arranque falla")
    void secretoCorto() {
        // Arrange - Act - Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> co.reservas.infrastructure.security.SecurityConfigAcceso.clave("corto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Dado una cabecera X-Trace-Id con caracteres no permitidos, cuando pasa por el filtro, entonces se genera un traceId nuevo y se limpia el MDC al terminar")
    void traceIdInvalidoSeReemplaza() throws Exception {
        // Arrange
        TraceIdFilter filtro = new TraceIdFilter();
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/v1/servicios");
        solicitud.addHeader(TraceIdFilter.CABECERA, "malo\nlog-inyectado");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicReference<String> enMdc = new AtomicReference<>();

        // Act
        filtro.doFilter(solicitud, respuesta, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                enMdc.set(MDC.get(TraceIdFilter.CLAVE_MDC));
            }
        });

        // Assert
        String generado = respuesta.getHeader(TraceIdFilter.CABECERA);
        assertThat(generado).matches("[a-f0-9]{32}");
        assertThat(enMdc.get()).isEqualTo(generado);
        assertThat(MDC.get(TraceIdFilter.CLAVE_MDC)).isNull();
    }
}

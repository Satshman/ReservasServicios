package co.reservas.infrastructure.observabilidad;

import co.reservas.infrastructure.ReservasServiciosApplication;
import co.reservas.soporte.ConfiguracionPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranca la aplicación con el perfil {@code observabilidad} en puertos reales (API y gestión aleatorios) para probar
 * lo que hace Prometheus: leer {@code /actuator/prometheus} por el puerto de gestión sin token.
 */
@SpringBootTest(classes = ReservasServiciosApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.defaults.metrics.export.enabled=true",
                "management.prometheus.metrics.export.enabled=true"})
@ActiveProfiles({"test", "observabilidad"})
@Import(ConfiguracionPruebas.class)
@DisplayName("Observabilidad - métricas para Prometheus en el puerto de gestión")
class ObservabilidadIntegracionTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int puertoApi;

    @LocalManagementPort
    private int puertoGestion;

    private HttpResponse<String> get(int puerto, String ruta) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void loginFallido() throws Exception {
        String cuerpo = "{\"email\":\"nadie@prueba.co\",\"password\":\"clave-incorrecta\"}";
        HttpResponse<String> respuesta = http.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + puertoApi + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Dado el puerto de gestión, cuando Prometheus lee las métricas sin token, entonces recibe el formato de exposición con la etiqueta de la aplicación")
    void prometheusLeeLasMetricasSinToken() throws Exception {
        // Arrange - Act
        HttpResponse<String> respuesta = get(puertoGestion, "/actuator/prometheus");

        // Assert
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body())
                .contains("jvm_memory_used_bytes")
                .contains("hikaricp_connections")
                .contains("application=\"reservas-servicios\"");
    }

    @Test
    @DisplayName("Dado un inicio de sesión rechazado, cuando se leen las métricas, entonces la petición aparece con codigo_error=CREDENCIALES_INVALIDAS")
    void metricaHttpLlevaElCodigoDeError() throws Exception {
        // Arrange
        loginFallido();

        // Act
        String metricas = get(puertoGestion, "/actuator/prometheus").body();

        // Assert
        assertThat(metricas).containsPattern(
                "http_server_requests_seconds_count\\{[^}]*codigo_error=\"CREDENCIALES_INVALIDAS\"[^}]*"
                        + "uri=\"/api/v1/auth/login\"[^}]*}");
        assertThat(metricas).contains("http_server_requests_seconds_bucket");
    }

    @Test
    @DisplayName("Dado el puerto de la API, cuando alguien pide las métricas, entonces no se exponen")
    void metricasNoSeExponenPorElPuertoDeLaApi() throws Exception {
        // Arrange - Act
        HttpResponse<String> respuesta = get(puertoApi, "/actuator/prometheus");

        // Assert
        assertThat(respuesta.statusCode()).isIn(401, 404);
        assertThat(respuesta.body()).doesNotContain("jvm_memory_used_bytes");
    }

    @Test
    @DisplayName("Dado el puerto de gestión, cuando se consulta la salud, entonces responde UP")
    void saludEnElPuertoDeGestion() throws Exception {
        // Arrange - Act
        HttpResponse<String> respuesta = get(puertoGestion, "/actuator/health");

        // Assert
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }
}

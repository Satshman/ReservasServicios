package co.reservas.adapters.in.web.auth;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sesiones - rotación y revocación de tokens de refresco (ADR-0006)")
class SesionTokenIntegracionTest extends PruebaIntegracion {

    private JsonNode sesionDeClienteNuevo() throws Exception {
        String email = emailUnico("cliente");
        registrarCliente(email).andExpect(status().isCreated());
        verificarCuenta(email);
        return leer(login(email, PASSWORD).andExpect(status().isOk()).andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions refrescar(String refreshToken) throws Exception {
        return enviar(post(V1 + "/auth/refresh"), Map.of("refreshToken", refreshToken), null);
    }

    @Nested
    @DisplayName("Rotación")
    class Rotacion {

        @Test
        @DisplayName("Dado un refresh token vigente, cuando se rota, entonces recibe un par nuevo y el token anterior deja de servir")
        void rotaYInvalidaElAnterior() throws Exception {
            // Arrange
            String refreshOriginal = sesionDeClienteNuevo().get("refreshToken").asString();

            // Act
            JsonNode rotada = leer(refrescar(refreshOriginal)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andReturn());

            // Assert
            String refreshNuevo = rotada.get("refreshToken").asString();
            assertThat(refreshNuevo).isNotBlank().isNotEqualTo(refreshOriginal);
            assertThat(rotada.get("accessToken").asString()).isNotBlank();
            refrescar(refreshOriginal)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("Dado un refresh token ya rotado, cuando alguien lo reutiliza, entonces se revocan todas las sesiones del usuario")
        void reutilizacionRevocaTodasLasSesiones() throws Exception {
            // Arrange
            String refreshOriginal = sesionDeClienteNuevo().get("refreshToken").asString();
            String refreshLegitimo = leer(refrescar(refreshOriginal).andExpect(status().isOk()).andReturn())
                    .get("refreshToken").asString();

            // Act
            refrescar(refreshOriginal).andExpect(status().isUnauthorized());

            // Assert
            refrescar(refreshLegitimo)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("Dado un refresh token inexistente o vacío, cuando se rota, entonces recibe 401 o 400 con el cuerpo uniforme")
        void tokenInexistenteOVacio() throws Exception {
            // Arrange - Act - Assert
            refrescar("token-que-no-existe")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
            refrescar("")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
        }
    }

    @Nested
    @DisplayName("Cierre de sesión")
    class CierreSesion {

        @Test
        @DisplayName("Dado una sesión abierta, cuando cierra sesión, entonces el refresh token queda revocado")
        void logoutRevocaElToken() throws Exception {
            // Arrange
            String refresh = sesionDeClienteNuevo().get("refreshToken").asString();

            // Act
            enviar(post(V1 + "/auth/logout"), Map.of("refreshToken", refresh), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());

            // Assert
            refrescar(refresh).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Dado un token ya revocado o desconocido, cuando cierra sesión otra vez, entonces responde 200 sin revelar nada")
        void logoutEsIdempotente() throws Exception {
            // Arrange
            String refresh = sesionDeClienteNuevo().get("refreshToken").asString();
            enviar(post(V1 + "/auth/logout"), Map.of("refreshToken", refresh), null).andExpect(status().isOk());

            // Act - Assert
            enviar(post(V1 + "/auth/logout"), Map.of("refreshToken", refresh), null).andExpect(status().isOk());
            enviar(post(V1 + "/auth/logout"), Map.of("refreshToken", "desconocido"), null).andExpect(status().isOk());
        }
    }
}

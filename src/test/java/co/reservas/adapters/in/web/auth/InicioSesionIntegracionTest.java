package co.reservas.adapters.in.web.auth;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-03 - Verificación de cuenta e inicio de sesión")
class InicioSesionIntegracionTest extends PruebaIntegracion {

    @Nested
    @DisplayName("Inicio de sesión")
    class InicioSesion {

        @Test
        @DisplayName("Dado un cliente verificado, cuando inicia sesión con credenciales correctas, entonces recibe el token, su rol y la ruta de su panel")
        void loginExitosoCliente() throws Exception {
            // Arrange
            String email = emailUnico("login");
            registrarCliente(email);
            verificarCuenta(email);

            // Act - Assert
            login(email, PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(1800))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.redirectTo").value("/panel/cliente"));
        }

        @Test
        @DisplayName("Dado un proveedor verificado, cuando inicia sesión, entonces es dirigido al panel de proveedor")
        void loginExitosoProveedor() throws Exception {
            // Arrange
            String email = emailUnico("login-proveedor");
            registrarProveedor(email, List.of(servicio("Asesoría", 45, 1)));
            verificarCuenta(email);

            // Act - Assert
            login(email, PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("PROVEEDOR"))
                    .andExpect(jsonPath("$.redirectTo").value("/panel/proveedor"));
        }

        @Test
        @DisplayName("Dado una contraseña incorrecta o un correo inexistente, cuando inicia sesión, entonces recibe el mismo 401 CREDENCIALES_INVALIDAS")
        void credencialesInvalidasGenericas() throws Exception {
            // Arrange
            String email = emailUnico("clave-mala");
            registrarCliente(email);
            verificarCuenta(email);

            // Act
            String conClaveMala = login(email, "otra-clave-incorrecta")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"))
                    .andReturn().getResponse().getContentAsString();
            String conCorreoInexistente = login(emailUnico("nadie"), PASSWORD)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"))
                    .andReturn().getResponse().getContentAsString();

            // Assert
            assertThat(jsonMapper.readTree(conClaveMala).get("message"))
                    .isEqualTo(jsonMapper.readTree(conCorreoInexistente).get("message"));
        }

        @Test
        @DisplayName("Dado cinco intentos fallidos, cuando inicia sesión con la contraseña correcta, entonces recibe 423 CUENTA_BLOQUEADA hasta que vence el bloqueo")
        void bloqueoTrasCincoFallos() throws Exception {
            // Arrange
            String email = emailUnico("bloqueo");
            registrarCliente(email);
            verificarCuenta(email);
            for (int intento = 0; intento < 5; intento++) {
                login(email, "clave-incorrecta-" + intento).andExpect(status().isUnauthorized());
            }

            // Act - Assert
            login(email, PASSWORD)
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.errorCode").value("CUENTA_BLOQUEADA"));
            login(email, "clave-incorrecta-durante-bloqueo")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
            reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
            login(email, PASSWORD).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Dado una cuenta no verificada, cuando inicia sesión con la contraseña correcta, entonces recibe 403 CUENTA_NO_VERIFICADA")
        void cuentaNoVerificada() throws Exception {
            // Arrange
            String email = emailUnico("sin-verificar");
            registrarCliente(email);

            // Act - Assert
            login(email, PASSWORD)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("CUENTA_NO_VERIFICADA"));
        }

        @Test
        @DisplayName("Dado un cliente autenticado, cuando invoca un endpoint de proveedor, entonces recibe 403 ACCESO_DENEGADO")
        void rbacPorEndpoint() throws Exception {
            // Arrange
            String token = clienteConSesion();

            // Act - Assert
            enviar(get(V1 + "/recursos"), null, token)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(get(V1 + "/reservas/mias"), null, "token-invalido")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
        }

        @Test
        @DisplayName("Dado un token emitido hace más de 30 minutos, cuando se usa, entonces recibe 401 NO_AUTENTICADO")
        void tokenExpirado() throws Exception {
            // Arrange
            String token = clienteConSesion();
            reloj.avanzar(Duration.ofMinutes(31));

            // Act - Assert
            enviar(get(V1 + "/reservas/mias"), null, token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
        }
    }

    @Nested
    @DisplayName("Verificación de cuenta")
    class Verificacion {

        @Test
        @DisplayName("Dado un token de verificación válido, cuando se usa dos veces, entonces la primera activa la cuenta y la segunda recibe 400 TOKEN_VERIFICACION_INVALIDO")
        void tokenDeUnSoloUso() throws Exception {
            // Arrange
            String email = emailUnico("verificacion");
            registrarCliente(email);
            String token = notificaciones.ultimoPara(email).orElseThrow().token();

            // Act - Assert
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("TOKEN_VERIFICACION_INVALIDO"));
            login(email, PASSWORD).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Dado un token inexistente o vencido, cuando se verifica, entonces recibe 400 TOKEN_VERIFICACION_INVALIDO")
        void tokenInvalidoOVencido() throws Exception {
            // Arrange
            String email = emailUnico("vencido");
            registrarCliente(email);
            String token = notificaciones.ultimoPara(email).orElseThrow().token();

            // Act - Assert
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", "token-que-no-existe"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("TOKEN_VERIFICACION_INVALIDO"));
            reloj.avanzar(Duration.ofHours(24).plusSeconds(1));
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("TOKEN_VERIFICACION_INVALIDO"));
        }

        @Test
        @DisplayName("Dado una cuenta pendiente, cuando solicita reenvío, entonces recibe 202, un nuevo enlace y el anterior deja de servir")
        void reenvioInvalidaTokensAnteriores() throws Exception {
            // Arrange
            String email = emailUnico("reenvio");
            registrarCliente(email);
            String tokenAnterior = notificaciones.ultimoPara(email).orElseThrow().token();

            // Act
            enviar(post(V1 + "/auth/verificacion/reenvio"), Map.of("email", email), null)
                    .andExpect(status().isAccepted());

            // Assert
            String tokenNuevo = notificaciones.ultimoPara(email).orElseThrow().token();
            assertThat(tokenNuevo).isNotEqualTo(tokenAnterior);
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", tokenAnterior))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(V1 + "/auth/verificacion").param("token", tokenNuevo))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Dado un correo no registrado, cuando solicita reenvío, entonces recibe 202 sin revelar que no existe")
        void reenvioCorreoInexistente() throws Exception {
            // Arrange
            String email = emailUnico("fantasma");

            // Act - Assert
            enviar(post(V1 + "/auth/verificacion/reenvio"), Map.of("email", email), null)
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());
            assertThat(notificaciones.ultimoPara(email)).isEmpty();
        }
    }
}

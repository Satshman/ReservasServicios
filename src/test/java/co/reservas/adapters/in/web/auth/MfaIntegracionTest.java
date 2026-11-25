package co.reservas.adapters.in.web.auth;

import co.reservas.application.port.in.auth.AprovisionarAdministradorUseCase;
import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MFA para administradores - inicio de sesión en dos pasos con código por correo")
class MfaIntegracionTest extends PruebaIntegracion {

    @Autowired
    private AprovisionarAdministradorUseCase aprovisionador;

    private String adminNuevo() {
        String email = emailUnico("admin");
        assertThat(aprovisionador.aprovisionar(email, "Administradora de Prueba", PASSWORD)).isTrue();
        return email;
    }

    private ResultActions solicitar(String email, String password) throws Exception {
        return enviar(post(V1 + "/auth/mfa/solicitud"), Map.of("email", email, "password", password), null);
    }

    private ResultActions verificar(String email, String codigo) throws Exception {
        return enviar(post(V1 + "/auth/mfa/verificacion"), Map.of("email", email, "codigo", codigo), null);
    }

    private String codigoRecibido(String email) {
        return notificaciones.ultimoCodigoMfaPara(email).orElseThrow();
    }

    /** Un código de 6 dígitos distinto del recibido. */
    private static String codigoIncorrecto(String recibido) {
        return recibido.equals("000000") ? "111111" : "000000";
    }

    @Nested
    @DisplayName("Flujo completo")
    class Flujo {

        @Test
        @DisplayName("Dado un administrador, cuando solicita el código y lo verifica, entonces recibe tokens con rol ADMIN y el código no sirve otra vez")
        void solicitaYVerifica() throws Exception {
            // Arrange
            String email = adminNuevo();

            // Act
            solicitar(email, PASSWORD)
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());
            String codigo = codigoRecibido(email);

            // Assert
            assertThat(codigo).matches("\\d{6}");
            verificar(email, codigo)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("ADMIN"))
                    .andExpect(jsonPath("$.redirectTo").value("/panel/admin"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
            verificar(email, codigo)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("Dado un administrador, cuando intenta el inicio de sesión normal con la contraseña correcta, entonces recibe 403 y debe usar MFA")
        void loginNormalRechazaAdministradores() throws Exception {
            // Arrange
            String email = adminNuevo();

            // Act - Assert
            login(email, PASSWORD)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            login(email, "clave-incorrecta-123")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("Dado una sesión de administrador, cuando rota su refresh token, entonces conserva el rol ADMIN")
        void sesionAdminSeRota() throws Exception {
            // Arrange
            String email = adminNuevo();
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
            String refresh = leer(verificar(email, codigoRecibido(email)).andExpect(status().isOk()).andReturn())
                    .get("refreshToken").asString();

            // Act - Assert
            enviar(post(V1 + "/auth/refresh"), Map.of("refreshToken", refresh), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @DisplayName("Dado dos solicitudes seguidas, cuando verifica el primer código, entonces ya no sirve y solo vale el último")
        void soloValeElUltimoCodigo() throws Exception {
            // Arrange
            String email = adminNuevo();
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
            String primero = codigoRecibido(email);
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
            String segundo = codigoRecibido(email);

            // Act - Assert
            if (!primero.equals(segundo)) {
                verificar(email, primero)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
            }
            verificar(email, segundo)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("ADMIN"));
        }

        @Test
        @DisplayName("Dado un código vencido (más de 5 minutos), cuando se verifica, entonces recibe 401")
        void codigoVencido() throws Exception {
            // Arrange
            String email = adminNuevo();
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
            String codigo = codigoRecibido(email);
            reloj.avanzar(Duration.ofMinutes(6));

            // Act - Assert
            verificar(email, codigo)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
        }
    }

    @Nested
    @DisplayName("Protección contra fuerza bruta y cuentas no administradoras")
    class Proteccion {

        @Test
        @DisplayName("Dado cinco códigos incorrectos, cuando verifica el código correcto, entonces la cuenta queda bloqueada con 423")
        void codigosIncorrectosBloquean() throws Exception {
            // Arrange
            String email = adminNuevo();
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
            String codigo = codigoRecibido(email);
            for (int intento = 0; intento < 5; intento++) {
                verificar(email, codigoIncorrecto(codigo)).andExpect(status().isUnauthorized());
            }

            // Act - Assert
            verificar(email, codigo)
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.errorCode").value("CUENTA_BLOQUEADA"));
            solicitar(email, PASSWORD).andExpect(status().isLocked());
        }

        @Test
        @DisplayName("Dado cinco contraseñas incorrectas en la solicitud MFA, cuando usa la correcta, entonces recibe 423 y no se envía código")
        void contrasenasIncorrectasBloquean() throws Exception {
            // Arrange
            String email = adminNuevo();
            for (int intento = 0; intento < 5; intento++) {
                solicitar(email, "clave-incorrecta-" + intento)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.errorCode").value("CREDENCIALES_INVALIDAS"));
            }

            // Act - Assert
            solicitar(email, PASSWORD)
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.errorCode").value("CUENTA_BLOQUEADA"));
            assertThat(notificaciones.ultimoCodigoMfaPara(email)).isEmpty();
        }

        @Test
        @DisplayName("Dado un cliente, cuando solicita MFA con su contraseña correcta, entonces recibe 403 ACCESO_DENEGADO")
        void clienteNoUsaMfa() throws Exception {
            // Arrange
            String email = emailUnico("cliente");
            registrarCliente(email).andExpect(status().isCreated());
            verificarCuenta(email);

            // Act - Assert
            solicitar(email, PASSWORD)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            verificar(email, "123456").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Dado un código con formato inválido o un correo desconocido, cuando se verifica, entonces recibe 400 o 401")
        void validaciones() throws Exception {
            // Arrange
            String email = adminNuevo();

            // Act - Assert
            verificar(email, "12ab")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
            verificar(emailUnico("nadie"), "123456").andExpect(status().isUnauthorized());
            solicitar(emailUnico("nadie"), PASSWORD).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Dado un administrador ya aprovisionado, cuando se aprovisiona otra vez el mismo correo, entonces no se duplica ni se cambia")
        void aprovisionamientoIdempotente() throws Exception {
            // Arrange
            String email = adminNuevo();

            // Act
            boolean creado = aprovisionador.aprovisionar(email, "Otro nombre", "otra-clave-distinta-123");

            // Assert
            assertThat(creado).isFalse();
            solicitar(email, PASSWORD).andExpect(status().isAccepted());
        }
    }
}

package co.reservas.adapters.in.web.auth;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-01 / HU-02 - Registro de usuarios y proveedores")
class RegistroUsuarioIntegracionTest extends PruebaIntegracion {

    @Autowired
    private JdbcTemplate jdbc;

    @Nested
    @DisplayName("HU-01 - Registro de cliente")
    class RegistroCliente {

        @Test
        @DisplayName("Dado un visitante con datos válidos, cuando se registra como cliente, entonces la cuenta queda pendiente de verificación y recibe el correo")
        void registroValido() throws Exception {
            // Arrange
            String email = emailUnico("Nuevo.Cliente");

            // Act - Assert
            enviar(post(V1 + "/auth/registro"), Map.of("nombreCompleto", "  Ana Gómez ", "email", "  " + email + " ",
                    "password", PASSWORD, "rol", "CLIENTE"), null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"))
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());
            assertThat(notificaciones.ultimoPara(email.toLowerCase())).isPresent();
            Integer clientes = jdbc.queryForObject("select count(*) from tbl_clientes c join tbl_usuarios u "
                    + "on u.id = c.id_usuario where u.email = ?", Integer.class, email.toLowerCase());
            assertThat(clientes).isEqualTo(1);
        }

        @Test
        @DisplayName("Dado un correo ya registrado, cuando otro visitante se registra con el mismo correo en otra capitalización, entonces recibe 409 EMAIL_YA_REGISTRADO")
        void correoDuplicado() throws Exception {
            // Arrange
            String email = emailUnico("duplicado");
            registrarCliente(email).andExpect(status().isCreated());

            // Act - Assert
            registrarCliente(email.toUpperCase())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("EMAIL_YA_REGISTRADO"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }

        @Test
        @DisplayName("Dado un payload con campos inválidos, cuando se registra, entonces recibe 400 VALIDACION_FALLIDA con el detalle de cada campo")
        void payloadInvalido() throws Exception {
            // Arrange
            Map<String, Object> cuerpo = Map.of("nombreCompleto", "A", "email", "no-es-correo",
                    "password", "corta", "rol", "ADMIN");

            // Act - Assert
            enviar(post(V1 + "/auth/registro"), cuerpo, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.details[*].campo", hasItem("email")))
                    .andExpect(jsonPath("$.details[*].campo", hasItem("password")))
                    .andExpect(jsonPath("$.details[*].campo", hasItem("rol")))
                    .andExpect(jsonPath("$.details[*].campo", hasItem("nombreCompleto")));
        }

        @Test
        @DisplayName("Dado un JSON mal formado, cuando se registra, entonces recibe 400 SOLICITUD_INVALIDA")
        void jsonMalFormado() throws Exception {
            // Arrange - Act - Assert
            mockMvc.perform(post(V1 + "/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SOLICITUD_INVALIDA"));
        }

        @Test
        @DisplayName("Dado un cliente que envía servicios, cuando se registra, entonces recibe 400 VALIDACION_FALLIDA")
        void clienteConServicios() throws Exception {
            // Arrange
            Map<String, Object> cuerpo = Map.of("nombreCompleto", "Cliente", "email", emailUnico("c"),
                    "password", PASSWORD, "rol", "CLIENTE", "servicios", List.of(servicio("Consulta", 30, 1)));

            // Act - Assert
            enviar(post(V1 + "/auth/registro"), cuerpo, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.details[0].campo").value("servicios"));
        }

        @Test
        @DisplayName("Dado que el servidor de correo falla, cuando se registra, entonces el registro se conserva y puede pedir reenvío")
        void falloDeCorreoNoRevierteRegistro() throws Exception {
            // Arrange
            String email = emailUnico("sin-correo");
            notificaciones.simularFallo(true);

            // Act
            registrarCliente(email).andExpect(status().isCreated());
            notificaciones.simularFallo(false);
            enviar(post(V1 + "/auth/verificacion/reenvio"), Map.of("email", email), null)
                    .andExpect(status().isAccepted());

            // Assert
            assertThat(notificaciones.cantidadPara(email)).isEqualTo(1);
            verificarCuenta(email);
            login(email, PASSWORD).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("HU-02 - Registro de proveedor")
    class RegistroProveedor {

        @Test
        @DisplayName("Dado un proveedor con servicios, cuando se registra, entonces se crean la cuenta y sus servicios activos")
        void proveedorConServicios() throws Exception {
            // Arrange
            String email = emailUnico("proveedor");
            String nombreServicio = "Odontología " + email;

            // Act
            registrarProveedor(email, List.of(servicio(nombreServicio, 30, 1), servicio("Ortodoncia " + email, 60, 2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rol").value("PROVEEDOR"))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));

            // Assert
            mockMvc.perform(get(V1 + "/servicios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.nombre == '" + nombreServicio + "')].duracionMinutos").value(30))
                    .andExpect(jsonPath("$[?(@.nombre == '" + nombreServicio + "')].estado").value("ACTIVO"))
                    .andExpect(jsonPath("$[?(@.nombre == '" + nombreServicio + "')].zonaHoraria")
                            .value("America/Bogota"));
        }

        @Test
        @DisplayName("Dado un proveedor sin servicios, cuando se registra, entonces recibe 400 SERVICIO_REQUERIDO")
        void proveedorSinServicios() throws Exception {
            // Arrange
            String email = emailUnico("sin-servicios");

            // Act - Assert
            registrarProveedor(email, List.of())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_REQUERIDO"));
            assertThat(notificaciones.ultimoPara(email)).isEmpty();
        }

        @Test
        @DisplayName("Dado un servicio con una categoría inexistente, cuando el proveedor se registra, entonces recibe 404 CATEGORIA_NO_ENCONTRADA y no se crea nada")
        void categoriaInexistente() throws Exception {
            // Arrange
            String email = emailUnico("categoria");
            Map<String, Object> servicioInvalido = Map.of("nombre", "Masaje", "idCategoria", 9999,
                    "duracionMinutos", 30);

            // Act
            registrarProveedor(email, List.of(servicioInvalido))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("CATEGORIA_NO_ENCONTRADA"))
                    .andExpect(jsonPath("$.details[0].campo").value("servicios[0].idCategoria"));

            // Assert
            Integer usuarios = jdbc.queryForObject("select count(*) from tbl_usuarios where email = ?",
                    Integer.class, email);
            assertThat(usuarios).isZero();
        }

        @Test
        @DisplayName("Dado una zona horaria inválida o nombres de servicio repetidos, cuando el proveedor se registra, entonces recibe 400 VALIDACION_FALLIDA")
        void zonaInvalidaYNombresRepetidos() throws Exception {
            // Arrange
            Map<String, Object> zonaInvalida = Map.of("nombreCompleto", "Proveedor", "email", emailUnico("zona"),
                    "password", PASSWORD, "rol", "PROVEEDOR", "zonaHoraria", "Marte/Olympus",
                    "servicios", List.of(servicio("Clase", 60, 10)));

            // Act - Assert
            enviar(post(V1 + "/auth/registro"), zonaInvalida, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].campo").value("zonaHoraria"));
            registrarProveedor(emailUnico("repetidos"), List.of(servicio("Clase", 60, 10), servicio("Clase", 30, 1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].campo").value("servicios[1].nombre"));
        }
    }
}

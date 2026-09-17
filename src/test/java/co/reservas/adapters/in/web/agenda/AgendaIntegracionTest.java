package co.reservas.adapters.in.web.agenda;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-04 / HU-05 / HU-06 - Agenda del servicio")
class AgendaIntegracionTest extends PruebaIntegracion {

    /** Lunes siguiente al reloj de pruebas, en America/Bogota. */
    private static final String LUNES_10_00 = "2026-10-12T10:00:00-05:00";

    private static Map<String, Object> bloque(Object dias, String inicio, String fin) {
        return Map.of("diasSemana", dias, "horaInicio", inicio, "horaFin", fin);
    }

    private static Map<String, Object> edicion(int dia, String inicio, String fin) {
        return Map.of("diaSemana", dia, "horaInicio", inicio, "horaFin", fin);
    }

    private int primerHorario(ProveedorDePrueba proveedor) throws Exception {
        return leer(mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio() + "/horarios")).andReturn())
                .get(0).get("id").asInt();
    }

    @Nested
    @DisplayName("HU-04 - Crear agenda")
    class CrearAgenda {

        @Test
        @DisplayName("Dado un proveedor dueño del servicio, cuando crea un bloque para varios días, entonces se crea un bloque por día y la agenda es pública")
        void creaBloquesPorDia() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));

            // Act
            enviar(post(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"),
                    bloque(List.of(3, 1), "08:00", "12:00"), proveedor.token())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].diaSemana").value(1))
                    .andExpect(jsonPath("$[1].diaSemana").value(3));

            // Assert
            mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].horaInicio").value("08:00:00"))
                    .andExpect(jsonPath("$[0].horaFin").value("12:00:00"));
        }

        @Test
        @DisplayName("Dado un bloque existente, cuando crea otro que se solapa el mismo día, entonces recibe 409 HORARIO_SOLAPADO y no se crea ninguno")
        void solape() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "08:00", "12:00");

            // Act - Assert
            enviar(post(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"),
                    bloque(List.of(2, 1), "11:30", "13:00"), proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_SOLAPADO"))
                    .andExpect(jsonPath("$.details[0].diaSemana").value(1));
            mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"))
                    .andExpect(jsonPath("$.length()").value(1));
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "12:00", "14:00");
        }

        @Test
        @DisplayName("Dado un servicio de otro proveedor, cuando intenta crear un bloque, entonces recibe 403 ACCESO_DENEGADO")
        void noPropietario() throws Exception {
            // Arrange
            ProveedorDePrueba duenio = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Otro", 30, 1)));

            // Act - Assert
            enviar(post(V1 + "/servicios/" + duenio.idServicio() + "/horarios"),
                    bloque(List.of(1), "08:00", "12:00"), otro.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
        }

        @Test
        @DisplayName("Dado un servicio inexistente, un cliente o una petición sin token, cuando crea un bloque, entonces recibe 404, 403 y 401 respectivamente")
        void existenciaYRoles() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            String tokenCliente = clienteConSesion();
            Map<String, Object> cuerpo = bloque(List.of(1), "08:00", "12:00");

            // Act - Assert
            enviar(post(V1 + "/servicios/999999/horarios"), cuerpo, proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_ENCONTRADO"));
            enviar(post(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"), cuerpo, tokenCliente)
                    .andExpect(status().isForbidden());
            enviar(post(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"), cuerpo, null)
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get(V1 + "/servicios/999999/horarios")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Dado un bloque más corto que la duración del servicio o con inicio posterior al fin, cuando se crea, entonces recibe 422 BLOQUE_MENOR_A_DURACION o 400 VALIDACION_FALLIDA")
        void formaDelBloque() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Terapia", 60, 1)));
            String ruta = V1 + "/servicios/" + proveedor.idServicio() + "/horarios";

            // Act - Assert
            enviar(post(ruta), bloque(List.of(1), "08:00", "08:45"), proveedor.token())
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("BLOQUE_MENOR_A_DURACION"));
            enviar(post(ruta), bloque(List.of(1), "12:00", "08:00"), proveedor.token())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.details[0].campo").value("horaInicio"));
            enviar(post(ruta), bloque(List.of(8), "08:00", "12:00"), proveedor.token())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
            enviar(post(ruta), bloque(List.of(1), "8 am", "12:00"), proveedor.token())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SOLICITUD_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("HU-05 / HU-06 - Editar y eliminar agenda")
    class EditarEliminarAgenda {

        private ProveedorDePrueba proveedorConReserva() throws Exception {
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "08:00", "12:00");
            reservar(clienteConSesion(), proveedor.idServicio(), LUNES_10_00).andExpect(status().isCreated());
            return proveedor;
        }

        @Test
        @DisplayName("Dado una reserva confirmada que ya no cabe, cuando edita el bloque sin confirmar, entonces recibe 409 HORARIO_CON_RESERVAS; al confirmar se aplica y la reserva se mantiene")
        void editarConReservaAfectada() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReserva();
            int idHorario = primerHorario(proveedor);

            // Act - Assert
            enviar(put(V1 + "/horarios/" + idHorario), edicion(1, "08:00", "10:00"), proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_CON_RESERVAS"))
                    .andExpect(jsonPath("$.details[0].idReserva").isNumber())
                    .andExpect(jsonPath("$.details[0].fechaHoraInicio").value(LUNES_10_00))
                    .andExpect(jsonPath("$.details[0].fechaHoraFin").value("2026-10-12T10:30:00-05:00"));
            enviar(put(V1 + "/horarios/" + idHorario).param("confirmar", "true"), edicion(1, "08:00", "10:00"),
                    proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horario.horaFin").value("10:00:00"))
                    .andExpect(jsonPath("$.reservasAfectadas.length()").value(1));
            enviar(get(V1 + "/servicios/" + proveedor.idServicio() + "/reservas"), null, proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
        }

        @Test
        @DisplayName("Dado un cambio que mantiene la reserva dentro del bloque, cuando edita, entonces se aplica sin pedir confirmación")
        void editarSinAfectar() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReserva();
            int idHorario = primerHorario(proveedor);

            // Act - Assert
            enviar(put(V1 + "/horarios/" + idHorario), edicion(1, "09:00", "11:00"), proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horario.horaInicio").value("09:00:00"))
                    .andExpect(jsonPath("$.reservasAfectadas.length()").value(0));
        }

        @Test
        @DisplayName("Dado una reserva confirmada en el bloque, cuando lo elimina sin confirmar, entonces recibe 409; al confirmar se elimina y la reserva se mantiene")
        void eliminarConReserva() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReserva();
            int idHorario = primerHorario(proveedor);

            // Act - Assert
            enviar(delete(V1 + "/horarios/" + idHorario), null, proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_CON_RESERVAS"));
            enviar(delete(V1 + "/horarios/" + idHorario).param("confirmar", "true"), null, proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservasAfectadas[0].fechaHoraInicio").value(LUNES_10_00));
            mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio() + "/horarios"))
                    .andExpect(jsonPath("$.length()").value(0));
            enviar(get(V1 + "/servicios/" + proveedor.idServicio() + "/reservas"), null, proveedor.token())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Dado un bloque sin reservas, cuando lo elimina, entonces responde 200 con la lista vacía")
        void eliminarSinReservas() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(2), "08:00", "12:00");
            int idHorario = primerHorario(proveedor);

            // Act - Assert
            enviar(delete(V1 + "/horarios/" + idHorario), null, proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservasAfectadas.length()").value(0));
        }

        @Test
        @DisplayName("Dado un bloque de otro proveedor, un horario inexistente o un solape con otro bloque, cuando edita, entonces recibe 403, 404 y 409")
        void validacionesAlEditar() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "08:00", "10:00");
            crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "14:00", "16:00");
            int idHorario = primerHorario(proveedor);
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Otro", 30, 1)));

            // Act - Assert
            enviar(put(V1 + "/horarios/" + idHorario), edicion(1, "08:00", "09:00"), otro.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(delete(V1 + "/horarios/999999"), null, proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_NO_ENCONTRADO"));
            enviar(put(V1 + "/horarios/" + idHorario), edicion(1, "09:00", "15:00"), proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_SOLAPADO"));
            enviar(put(V1 + "/horarios/" + idHorario), edicion(1, "07:00", "09:59"), proveedor.token())
                    .andExpect(status().isOk());
        }
    }
}

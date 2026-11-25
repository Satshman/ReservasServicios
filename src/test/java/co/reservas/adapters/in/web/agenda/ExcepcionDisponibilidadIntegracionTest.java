package co.reservas.adapters.in.web.agenda;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Excepciones de disponibilidad - días o franjas en que un servicio no atiende")
class ExcepcionDisponibilidadIntegracionTest extends PruebaIntegracion {

    /** El reloj de pruebas marca el lunes 2026-10-05 a las 08:00 en America/Bogota. */
    private static final String PROXIMO_LUNES = "2026-10-12";

    private ProveedorDePrueba proveedorConAgendaLunes() throws Exception {
        ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 60, 1)));
        crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "08:00", "12:00");
        return proveedor;
    }

    private ResultActions crearExcepcion(String token, int idServicio, String horaInicio, String horaFin)
            throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("fecha", PROXIMO_LUNES);
        cuerpo.put("motivo", "Capacitación del personal");
        if (horaInicio != null) {
            cuerpo.put("horaInicio", horaInicio);
            cuerpo.put("horaFin", horaFin);
        }
        return enviar(post(V1 + "/servicios/" + idServicio + "/excepciones"), cuerpo, token);
    }

    private ResultActions disponibilidad(int idServicio) throws Exception {
        return mockMvc.perform(get(V1 + "/servicios/" + idServicio + "/disponibilidad")
                .param("desde", PROXIMO_LUNES).param("hasta", PROXIMO_LUNES));
    }

    @Nested
    @DisplayName("Registro y efecto sobre la agenda")
    class Registro {

        @Test
        @DisplayName("Dado un proveedor con agenda, cuando bloquea una franja, entonces se lista públicamente y esos turnos desaparecen de la disponibilidad")
        void franjaBloqueadaSaleDeLaDisponibilidad() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgendaLunes();
            disponibilidad(proveedor.idServicio()).andExpect(jsonPath("$.turnos.length()").value(4));

            // Act
            crearExcepcion(proveedor.token(), proveedor.idServicio(), "08:00", "10:00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.motivo").value("Capacitación del personal"));

            // Assert
            mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio() + "/excepciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].fecha").value(PROXIMO_LUNES));
            disponibilidad(proveedor.idServicio()).andExpect(jsonPath("$.turnos.length()").value(2));
        }

        @Test
        @DisplayName("Dado un día completo bloqueado, cuando un cliente reserva ese día, entonces recibe 422 HORARIO_NO_DISPONIBLE")
        void diaBloqueadoRechazaReservas() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgendaLunes();
            crearExcepcion(proveedor.token(), proveedor.idServicio(), null, null).andExpect(status().isCreated());
            String cliente = clienteConSesion();

            // Act - Assert
            reservar(cliente, proveedor.idServicio(), PROXIMO_LUNES + "T09:00:00-05:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_NO_DISPONIBLE"));
        }

        @Test
        @DisplayName("Dado una franja con la hora de fin antes de la de inicio, cuando se registra, entonces recibe 400 VALIDACION_FALLIDA")
        void franjaInvertidaEsRechazada() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgendaLunes();

            // Act - Assert
            crearExcepcion(proveedor.token(), proveedor.idServicio(), "11:00", "09:00")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
        }
    }

    @Nested
    @DisplayName("Propiedad y eliminación")
    class Eliminacion {

        @Test
        @DisplayName("Dado otro proveedor, cuando intenta crear o eliminar una excepción ajena, entonces recibe 403 ACCESO_DENEGADO")
        void otroProveedorNoPuedeGestionarla() throws Exception {
            // Arrange
            ProveedorDePrueba duenio = proveedorConAgendaLunes();
            ProveedorDePrueba ajeno = proveedorConServicios(List.of(servicio("Otro", 60, 1)));
            int idExcepcion = leer(crearExcepcion(duenio.token(), duenio.idServicio(), null, null)
                    .andExpect(status().isCreated()).andReturn()).get("id").asInt();

            // Act - Assert
            crearExcepcion(ajeno.token(), duenio.idServicio(), null, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(delete(V1 + "/excepciones/" + idExcepcion), null, ajeno.token())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Dado un cliente, cuando intenta crear una excepción, entonces recibe 403 por RBAC")
        void clienteNoPuedeCrearExcepciones() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgendaLunes();

            // Act - Assert
            crearExcepcion(clienteConSesion(), proveedor.idServicio(), null, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
        }

        @Test
        @DisplayName("Dado una excepción propia, cuando el proveedor la elimina, entonces la agenda recupera sus turnos y un segundo intento recibe 404")
        void eliminarDevuelveLosTurnos() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgendaLunes();
            int idExcepcion = leer(crearExcepcion(proveedor.token(), proveedor.idServicio(), null, null)
                    .andExpect(status().isCreated()).andReturn()).get("id").asInt();
            disponibilidad(proveedor.idServicio()).andExpect(jsonPath("$.turnos.length()").value(0));

            // Act
            enviar(delete(V1 + "/excepciones/" + idExcepcion), null, proveedor.token())
                    .andExpect(status().isNoContent());

            // Assert
            disponibilidad(proveedor.idServicio()).andExpect(jsonPath("$.turnos.length()").value(4));
            enviar(delete(V1 + "/excepciones/" + idExcepcion), null, proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_NO_ENCONTRADO"));
        }
    }
}

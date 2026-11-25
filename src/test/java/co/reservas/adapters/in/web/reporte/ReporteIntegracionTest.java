package co.reservas.adapters.in.web.reporte;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-12 / HU-13 - Reportes de ocupación y de desempeño de servicios")
class ReporteIntegracionTest extends PruebaIntegracion {

    /** El reloj de pruebas marca el lunes 2026-10-05 a las 08:00 en America/Bogota. */
    private static final String PROXIMO_LUNES = "2026-10-12";

    /**
     * Servicio de 60 minutos con capacidad 2 y agenda los lunes de 08:00 a 12:00: 4 turnos y 8 cupos ese día. Un
     * cliente reserva 09:00 y otro reserva 10:00 y luego cancela.
     */
    private ProveedorDePrueba proveedorConReservas() throws Exception {
        ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Terapia", 60, 2),
                servicio("Valoración", 30, 1)));
        crearHorario(proveedor.token(), proveedor.idServicio(), List.of(1), "08:00", "12:00");
        reservar(clienteConSesion(), proveedor.idServicio(), PROXIMO_LUNES + "T09:00:00-05:00")
                .andExpect(status().isCreated());
        String otroCliente = clienteConSesion();
        int idCancelada = leer(reservar(otroCliente, proveedor.idServicio(), PROXIMO_LUNES + "T10:00:00-05:00")
                .andExpect(status().isCreated()).andReturn()).get("id").asInt();
        enviar(post(V1 + "/reservas/" + idCancelada + "/cancelacion"), null, proveedor.token())
                .andExpect(status().isOk());
        return proveedor;
    }

    private ResultActions ocupacion(String token, int idServicio, String desde, String hasta, String desglose)
            throws Exception {
        return mockMvc.perform(get(V1 + "/reportes/ocupacion")
                .header("Authorization", "Bearer " + token)
                .param("idServicio", String.valueOf(idServicio))
                .param("desde", desde).param("hasta", hasta).param("desglose", desglose));
    }

    @Nested
    @DisplayName("Ocupación (HU-12)")
    class Ocupacion {

        @Test
        @DisplayName("Dado reservas confirmadas y canceladas, cuando el proveedor consulta por día, entonces solo cuentan las confirmadas sobre la capacidad de la agenda")
        void ocupacionPorDia() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReservas();

            // Act - Assert
            ocupacion(proveedor.token(), proveedor.idServicio(), PROXIMO_LUNES, "2026-10-13", "DIA")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReservasConfirmadas").value(1))
                    .andExpect(jsonPath("$.totalCapacidadDisponible").value(8))
                    .andExpect(jsonPath("$.porcentajeOcupacionGlobal").value(12.5))
                    .andExpect(jsonPath("$.porDia.length()").value(2))
                    .andExpect(jsonPath("$.porDia[0].reservasConfirmadas").value(1))
                    .andExpect(jsonPath("$.porDia[1].capacidadDisponible").value(0))
                    .andExpect(jsonPath("$.porBloqueHorario.length()").value(0));
        }

        @Test
        @DisplayName("Dado el desglose por bloque horario, cuando el proveedor consulta, entonces recibe un renglón por bloque de la agenda")
        void ocupacionPorBloque() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReservas();

            // Act - Assert
            ocupacion(proveedor.token(), proveedor.idServicio(), PROXIMO_LUNES, PROXIMO_LUNES, "BLOQUE_HORARIO")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.porBloqueHorario.length()").value(1))
                    .andExpect(jsonPath("$.porBloqueHorario[0].diaSemana").value("MONDAY"))
                    .andExpect(jsonPath("$.porBloqueHorario[0].porcentajeOcupacion").value(12.5))
                    .andExpect(jsonPath("$.porDia.length()").value(0));
        }

        @Test
        @DisplayName("Dado un rango invertido, un servicio ajeno o un cliente, cuando se consulta, entonces recibe 400, 403 y 403")
        void validacionesYPropiedad() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReservas();
            ProveedorDePrueba ajeno = proveedorConServicios(List.of(servicio("Ajeno", 60, 1)));

            // Act - Assert
            ocupacion(proveedor.token(), proveedor.idServicio(), "2026-10-13", PROXIMO_LUNES, "DIA")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
            ocupacion(ajeno.token(), proveedor.idServicio(), PROXIMO_LUNES, PROXIMO_LUNES, "DIA")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            ocupacion(clienteConSesion(), proveedor.idServicio(), PROXIMO_LUNES, PROXIMO_LUNES, "DIA")
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Desempeño de servicios (HU-13)")
    class Desempenio {

        @Test
        @DisplayName("Dado reservas en un servicio, cuando el proveedor consulta, entonces ve totales, canceladas, tasa y el más solicitado")
        void desempenioPorServicio() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReservas();

            // Act - Assert
            mockMvc.perform(get(V1 + "/reportes/servicios").header("Authorization", "Bearer " + proveedor.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idProveedor").value(proveedor.idProveedor()))
                    .andExpect(jsonPath("$.servicios.length()").value(2))
                    .andExpect(jsonPath("$.servicios[0].idServicio").value(proveedor.idServicio()))
                    .andExpect(jsonPath("$.servicios[0].totalReservas").value(2))
                    .andExpect(jsonPath("$.servicios[0].reservasCanceladas").value(1))
                    .andExpect(jsonPath("$.servicios[0].tasaCancelacion").value(50.0))
                    .andExpect(jsonPath("$.servicioMasSolicitado.idServicio").value(proveedor.idServicio()));
        }

        @Test
        @DisplayName("Dado el id de otro proveedor, cuando se pide su reporte, entonces recibe 403 ACCESO_DENEGADO")
        void reporteDeOtroProveedor() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConReservas();
            ProveedorDePrueba ajeno = proveedorConServicios(List.of(servicio("Ajeno", 60, 1)));

            // Act - Assert
            mockMvc.perform(get(V1 + "/reportes/servicios").header("Authorization", "Bearer " + ajeno.token())
                            .param("idProveedor", String.valueOf(proveedor.idProveedor())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
        }
    }
}

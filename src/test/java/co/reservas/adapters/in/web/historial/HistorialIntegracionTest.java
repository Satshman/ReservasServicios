package co.reservas.adapters.in.web.historial;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-10 / HU-11 / HU-14 - Historial de reservas y de servicios")
class HistorialIntegracionTest extends PruebaIntegracion {

    /** El reloj de pruebas marca el lunes 2026-10-05 a las 08:00 en America/Bogota. */
    private static final String LUNES_12 = "2026-10-12";
    private static final String LUNES_19 = "2026-10-19";

    @Autowired
    private JdbcTemplate jdbc;

    private static String inicio(String fecha, String hora) {
        return fecha + "T" + hora + ":00-05:00";
    }

    private record ClienteDePrueba(String token, int idUsuario) {
    }

    private ClienteDePrueba clienteConDatos() throws Exception {
        String email = emailUnico("cliente");
        registrarCliente(email).andExpect(status().isCreated());
        verificarCuenta(email);
        return new ClienteDePrueba(tokenDe(email), idUsuarioDe(email));
    }

    private int idUsuarioDe(String email) {
        return jdbc.queryForObject("select id from tbl_usuarios where email = ?", Integer.class, email);
    }

    private ProveedorDePrueba proveedorConAgenda(List<Map<String, Object>> servicios) throws Exception {
        ProveedorDePrueba proveedor = proveedorConServicios(servicios);
        for (Integer idServicio : proveedor.idsServicios()) {
            crearHorario(proveedor.token(), idServicio, List.of(1), "08:00", "12:00");
        }
        return proveedor;
    }

    private int reservarYObtenerId(String token, int idServicio, String fechaHoraInicio) throws Exception {
        MvcResult resultado = reservar(token, idServicio, fechaHoraInicio).andExpect(status().isCreated()).andReturn();
        return leer(resultado).get("id").asInt();
    }

    private void cancelar(String token, int idReserva) throws Exception {
        enviar(post(V1 + "/reservas/" + idReserva + "/cancelacion"), null, token).andExpect(status().isOk());
    }

    private ResultActions historialReservas(String token, String consulta) throws Exception {
        return enviar(get(V1 + "/reservas/historial" + consulta), null, token);
    }

    private ResultActions historialServicios(String token, String consulta) throws Exception {
        return enviar(get(V1 + "/servicios/historial" + consulta), null, token);
    }

    @Nested
    @DisplayName("HU-10 - Historial de reservas del cliente")
    class HistorialCliente {

        @Test
        @DisplayName("Dado un cliente con una reserva confirmada y otra cancelada, cuando consulta su historial, entonces las ve de la más reciente a la más antigua con sus cambios de estado, quién los hizo y cuándo")
        void historialConCambios() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            ClienteDePrueba cliente = clienteConDatos();
            int primera = reservarYObtenerId(cliente.token(), proveedor.idServicio(), inicio(LUNES_12, "09:00"));
            int segunda = reservarYObtenerId(cliente.token(), proveedor.idServicio(), inicio(LUNES_19, "09:00"));
            reloj.avanzar(Duration.ofMinutes(10));
            cancelar(cliente.token(), segunda);

            // Act - Assert
            historialReservas(cliente.token(), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].idReserva").value(segunda))
                    .andExpect(jsonPath("$[0].estado").value("CANCELADA"))
                    .andExpect(jsonPath("$[0].nombreServicio").value(startsWith("Consulta")))
                    .andExpect(jsonPath("$[0].fechaHoraInicio").value(inicio(LUNES_19, "09:00")))
                    .andExpect(jsonPath("$[0].cambios.length()").value(2))
                    .andExpect(jsonPath("$[0].cambios[0].estadoAnterior").value(nullValue()))
                    .andExpect(jsonPath("$[0].cambios[0].estadoNuevo").value("CONFIRMADA"))
                    .andExpect(jsonPath("$[0].cambios[1].estadoAnterior").value("CONFIRMADA"))
                    .andExpect(jsonPath("$[0].cambios[1].estadoNuevo").value("CANCELADA"))
                    .andExpect(jsonPath("$[0].cambios[1].realizadoPor.idUsuario").value(cliente.idUsuario()))
                    .andExpect(jsonPath("$[0].cambios[1].realizadoPor.nombre").value("Cliente de Prueba"))
                    .andExpect(jsonPath("$[0].cambios[1].realizadoPor.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$[0].cambios[1].fechaCambio").value(containsString("2026-10-05T13:10")))
                    .andExpect(jsonPath("$[1].idReserva").value(primera))
                    .andExpect(jsonPath("$[1].estado").value("CONFIRMADA"))
                    .andExpect(jsonPath("$[1].cambios.length()").value(1));
        }

        @Test
        @DisplayName("Dado un cliente con varias reservas, cuando filtra por estado o por rango de fechas, entonces solo ve las que cumplen el filtro")
        void filtros() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            ClienteDePrueba cliente = clienteConDatos();
            int primera = reservarYObtenerId(cliente.token(), proveedor.idServicio(), inicio(LUNES_12, "10:00"));
            int segunda = reservarYObtenerId(cliente.token(), proveedor.idServicio(), inicio(LUNES_19, "10:00"));
            cancelar(cliente.token(), segunda);

            // Act - Assert
            historialReservas(cliente.token(), "?estado=CANCELADA")
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(segunda));
            historialReservas(cliente.token(), "?desde=" + LUNES_12 + "&hasta=2026-10-18")
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(primera));
            historialReservas(cliente.token(), "?desde=" + LUNES_19)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(segunda));
            historialReservas(cliente.token(), "?desde=" + LUNES_19 + "&hasta=" + LUNES_12)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.details[0].campo").value("hasta"));
            historialReservas(cliente.token(), "?estado=PERDIDA")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SOLICITUD_INVALIDA"));
        }

        @Test
        @DisplayName("Dado la reserva de otro cliente, cuando un cliente consulta su historial, entonces recibe 403 ACCESO_DENEGADO y no la ve en su propio historial")
        void reservaDeOtroCliente() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            ClienteDePrueba duenio = clienteConDatos();
            ClienteDePrueba intruso = clienteConDatos();
            int idReserva = reservarYObtenerId(duenio.token(), proveedor.idServicio(), inicio(LUNES_12, "11:00"));

            // Act - Assert
            enviar(get(V1 + "/reservas/" + idReserva + "/historial"), null, intruso.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            historialReservas(intruso.token(), "").andExpect(jsonPath("$.length()").value(0));
            enviar(get(V1 + "/reservas/" + idReserva + "/historial"), null, duenio.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idReserva").value(idReserva))
                    .andExpect(jsonPath("$.cambios[0].realizadoPor.idUsuario").value(duenio.idUsuario()));
            enviar(get(V1 + "/reservas/999999/historial"), null, duenio.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("RESERVA_NO_ENCONTRADA"));
            historialReservas(null, "").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Dado un cliente sin reservas, cuando consulta su historial, entonces recibe una lista vacía sin error")
        void sinReservas() throws Exception {
            // Arrange
            String token = clienteConSesion();

            // Act - Assert
            historialReservas(token, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("HU-11 - Historial de cambios de los servicios del proveedor")
    class HistorialServicios {

        @Test
        @DisplayName("Dado un servicio recién creado, cuando el proveedor consulta su historial, entonces solo ve el evento de creación")
        void servicioRecienCreado() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));

            // Act - Assert
            historialServicios(proveedor.token(), "?idServicio=" + proveedor.idServicio())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].tipoCambio").value("CREACION"))
                    .andExpect(jsonPath("$[0].idServicio").value(proveedor.idServicio()))
                    .andExpect(jsonPath("$[0].valorAnterior").value(nullValue()))
                    .andExpect(jsonPath("$[0].valorNuevo").value(startsWith("Consulta")))
                    .andExpect(jsonPath("$[0].valorNuevo").value(endsWith(" · 30 min · capacidad 1")))
                    .andExpect(jsonPath("$[0].realizadoPor.nombre").value("Proveedor de Prueba"))
                    .andExpect(jsonPath("$[0].realizadoPor.rol").value("PROVEEDOR"))
                    .andExpect(jsonPath("$[0].fechaCambio").isNotEmpty());
        }

        @Test
        @DisplayName("Dado cambios de agenda, recursos y estado, cuando el proveedor consulta el historial del servicio, entonces los ve en orden cronológico con qué cambió")
        void cambiosCronologicos() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            int idServicio = proveedor.idServicio();
            crearHorario(proveedor.token(), idServicio, List.of(1), "08:00", "12:00");
            int idHorario = leer(mockMvc.perform(get(V1 + "/servicios/" + idServicio + "/horarios")).andReturn())
                    .get(0).get("id").asInt();
            reloj.avanzar(Duration.ofMinutes(5));
            enviar(put(V1 + "/horarios/" + idHorario),
                    Map.of("diaSemana", 2, "horaInicio", "09:00", "horaFin", "13:00"), proveedor.token())
                    .andExpect(status().isOk());
            int idRecurso = crearRecurso(proveedor.token(), "Sala historial");
            asignarRecursos(proveedor.token(), idServicio, List.of(idRecurso));
            asignarRecursos(proveedor.token(), idServicio, List.of(idRecurso));
            enviar(put(V1 + "/servicios/" + idServicio + "/estado"), Map.of("estado", "INACTIVO"), proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("INACTIVO"));
            enviar(put(V1 + "/servicios/" + idServicio + "/estado"), Map.of("estado", "INACTIVO"), proveedor.token())
                    .andExpect(status().isOk());
            enviar(delete(V1 + "/horarios/" + idHorario), null, proveedor.token()).andExpect(status().isOk());

            // Act - Assert
            historialServicios(proveedor.token(), "?idServicio=" + idServicio)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(6))
                    .andExpect(jsonPath("$[0].tipoCambio").value("CREACION"))
                    .andExpect(jsonPath("$[1].tipoCambio").value("HORARIO_CREADO"))
                    .andExpect(jsonPath("$[1].valorNuevo").value("Lunes 08:00-12:00"))
                    .andExpect(jsonPath("$[2].tipoCambio").value("HORARIO_EDITADO"))
                    .andExpect(jsonPath("$[2].valorAnterior").value("Lunes 08:00-12:00"))
                    .andExpect(jsonPath("$[2].valorNuevo").value("Martes 09:00-13:00"))
                    .andExpect(jsonPath("$[2].fechaCambio").value(containsString("2026-10-05T13:05")))
                    .andExpect(jsonPath("$[3].tipoCambio").value("RECURSOS_ASIGNADOS"))
                    .andExpect(jsonPath("$[3].valorAnterior").value("Sin recursos"))
                    .andExpect(jsonPath("$[3].valorNuevo").value("Sala historial"))
                    .andExpect(jsonPath("$[4].tipoCambio").value("ESTADO_CAMBIADO"))
                    .andExpect(jsonPath("$[4].valorAnterior").value("ACTIVO"))
                    .andExpect(jsonPath("$[4].valorNuevo").value("INACTIVO"))
                    .andExpect(jsonPath("$[5].tipoCambio").value("HORARIO_ELIMINADO"))
                    .andExpect(jsonPath("$[5].valorAnterior").value("Martes 09:00-13:00"))
                    .andExpect(jsonPath("$[5].valorNuevo").value(nullValue()));
        }

        @Test
        @DisplayName("Dado un servicio inactivo, cuando un cliente intenta reservarlo, entonces recibe 422 SERVICIO_NO_DISPONIBLE")
        void servicioInactivoNoAceptaReservas() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            enviar(put(V1 + "/servicios/" + proveedor.idServicio() + "/estado"), Map.of("estado", "INACTIVO"),
                    proveedor.token()).andExpect(status().isOk());

            // Act - Assert
            reservar(clienteConSesion(), proveedor.idServicio(), inicio(LUNES_12, "08:00"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_DISPONIBLE"));
            enviar(put(V1 + "/servicios/" + proveedor.idServicio() + "/estado"), Map.of("estado", "ACTIVO"),
                    proveedor.token()).andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Dado un proveedor con varios servicios, cuando filtra por uno, entonces solo ve el historial de ese servicio")
        void filtroPorServicio() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(
                    List.of(servicio("Consulta", 30, 1), servicio("Terapia", 60, 1)));
            int terapia = proveedor.idsServicios().get(1);

            // Act - Assert
            historialServicios(proveedor.token(), "")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].idServicio").value(proveedor.idServicio()))
                    .andExpect(jsonPath("$[1].idServicio").value(terapia));
            historialServicios(proveedor.token(), "?idServicio=" + terapia)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombreServicio").value(startsWith("Terapia")));
        }

        @Test
        @DisplayName("Dado un servicio de otro proveedor, cuando consulta su historial o cambia su estado, entonces recibe 403 ACCESO_DENEGADO")
        void servicioAjeno() throws Exception {
            // Arrange
            ProveedorDePrueba duenio = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));

            // Act - Assert
            historialServicios(otro.token(), "?idServicio=" + duenio.idServicio())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(put(V1 + "/servicios/" + duenio.idServicio() + "/estado"), Map.of("estado", "INACTIVO"),
                    otro.token()).andExpect(status().isForbidden());
            historialServicios(otro.token(), "?idServicio=999999")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_ENCONTRADO"));
            historialServicios(clienteConSesion(), "").andExpect(status().isForbidden());
            historialServicios(null, "").andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("HU-14 - Historial de reservas del negocio")
    class HistorialNegocio {

        @Test
        @DisplayName("Dado reservas de varios clientes en mis servicios, una cancelada por mí, cuando consulto el historial, entonces las veo de la más reciente a la más antigua con sus cambios y autores, sin las de otros negocios")
        void historialDelNegocio() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(
                    List.of(servicio("Consulta", 30, 1), servicio("Terapia", 60, 1)));
            ProveedorDePrueba otroNegocio = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            ClienteDePrueba ana = clienteConDatos();
            ClienteDePrueba beto = clienteConDatos();
            int deAna = reservarYObtenerId(ana.token(), proveedor.idServicio(), inicio(LUNES_12, "08:00"));
            int deBeto = reservarYObtenerId(beto.token(), proveedor.idsServicios().get(1), inicio(LUNES_19, "10:00"));
            reservarYObtenerId(ana.token(), otroNegocio.idServicio(), inicio(LUNES_19, "08:00"));
            reloj.avanzar(Duration.ofMinutes(20));
            cancelar(proveedor.token(), deBeto);

            // Act - Assert
            historialReservas(proveedor.token(), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].idReserva").value(deBeto))
                    .andExpect(jsonPath("$[0].estado").value("CANCELADA"))
                    .andExpect(jsonPath("$[0].cambios[0].realizadoPor.idUsuario").value(beto.idUsuario()))
                    .andExpect(jsonPath("$[0].cambios[1].estadoNuevo").value("CANCELADA"))
                    .andExpect(jsonPath("$[0].cambios[1].realizadoPor.rol").value("PROVEEDOR"))
                    .andExpect(jsonPath("$[0].cambios[1].fechaCambio").value(containsString("2026-10-05T13:20")))
                    .andExpect(jsonPath("$[1].idReserva").value(deAna))
                    .andExpect(jsonPath("$[1].estado").value("CONFIRMADA"));
            historialReservas(beto.token(), "")
                    .andExpect(jsonPath("$[0].cambios[1].realizadoPor.rol").value("PROVEEDOR"));
        }

        @Test
        @DisplayName("Dado reservas en varios servicios, cuando filtro por servicio, estado o rango de fechas, entonces solo veo las que cumplen el filtro")
        void filtros() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(
                    List.of(servicio("Consulta", 30, 1), servicio("Terapia", 60, 1)));
            int terapia = proveedor.idsServicios().get(1);
            int consulta = reservarYObtenerId(clienteConSesion(), proveedor.idServicio(), inicio(LUNES_12, "08:30"));
            int sesion = reservarYObtenerId(clienteConSesion(), terapia, inicio(LUNES_19, "09:00"));
            cancelar(proveedor.token(), sesion);

            // Act - Assert
            historialReservas(proveedor.token(), "?idServicio=" + terapia)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(sesion));
            historialReservas(proveedor.token(), "?estado=CONFIRMADA")
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(consulta));
            historialReservas(proveedor.token(), "?desde=" + LUNES_19 + "&hasta=" + LUNES_19)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idReserva").value(sesion));
            historialReservas(proveedor.token(), "?idServicio=" + terapia + "&estado=CONFIRMADA")
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Dado un servicio o una reserva de otro proveedor, cuando consulto su historial, entonces recibo 403 ACCESO_DENEGADO")
        void serviciosAjenos() throws Exception {
            // Arrange
            ProveedorDePrueba duenio = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            int idReserva = reservarYObtenerId(clienteConSesion(), duenio.idServicio(), inicio(LUNES_12, "09:30"));

            // Act - Assert
            historialReservas(otro.token(), "?idServicio=" + duenio.idServicio())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(get(V1 + "/reservas/" + idReserva + "/historial"), null, otro.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(get(V1 + "/reservas/" + idReserva + "/historial"), null, duenio.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idReserva").value(idReserva));
            historialReservas(otro.token(), "?idServicio=999999")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("Dado un proveedor sin reservas en sus servicios, cuando consulta el historial, entonces recibe una lista vacía sin error")
        void sinReservas() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));

            // Act - Assert
            historialReservas(proveedor.token(), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}

package co.reservas.adapters.in.web.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reserva.CrearReservaComando;
import co.reservas.application.port.in.reserva.CrearReservaUseCase;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Rol;
import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HU-07 / HU-09 - Reservas y disponibilidad de recursos")
class ReservaIntegracionTest extends PruebaIntegracion {

    /** El reloj de pruebas marca el lunes 2026-10-05 a las 08:00 en America/Bogota. */
    private static final String PROXIMO_LUNES = "2026-10-12";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CrearReservaUseCase crearReserva;

    private static String lunes(String hora) {
        return PROXIMO_LUNES + "T" + hora + ":00-05:00";
    }

    private ProveedorDePrueba proveedorConAgenda(List<Map<String, Object>> servicios) throws Exception {
        ProveedorDePrueba proveedor = proveedorConServicios(servicios);
        for (Integer idServicio : proveedor.idsServicios()) {
            crearHorario(proveedor.token(), idServicio, List.of(1), "08:00", "12:00");
        }
        return proveedor;
    }

    private static int idUsuarioDe(String email, JdbcTemplate jdbc) {
        return jdbc.queryForObject("select id from tbl_usuarios where email = ?", Integer.class, email);
    }

    @Nested
    @DisplayName("HU-07 - Crear reserva")
    class CrearReserva {

        @Test
        @DisplayName("Dado un turno disponible, cuando el cliente reserva, entonces se crea CONFIRMADA con su fin calculado y el historial registra la creación")
        void reservaExitosa() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            String token = clienteConSesion();

            // Act
            MvcResult resultado = reservar(token, proveedor.idServicio(), "2026-10-12T13:30:00Z")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                    .andExpect(jsonPath("$.fechaHoraInicio").value(lunes("08:30")))
                    .andExpect(jsonPath("$.fechaHoraFin").value(lunes("09:00")))
                    .andExpect(jsonPath("$.recursos.length()").value(0))
                    .andReturn();

            // Assert
            int idReserva = leer(resultado).get("id").asInt();
            Map<String, Object> historial = jdbc.queryForMap("select h.id_estado_anterior, e.nombre as estado_nuevo "
                    + "from tbl_historial_reservas h join tbl_estados e on e.id = h.id_estado_nuevo "
                    + "where h.id_reserva = ?", idReserva);
            assertThat(historial.get("id_estado_anterior")).isNull();
            assertThat(historial.get("estado_nuevo")).isEqualTo("CONFIRMADA");
            enviar(get(V1 + "/reservas/mias"), null, token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(idReserva))
                    .andExpect(jsonPath("$[0].nombreServicio").isNotEmpty());
            enviar(get(V1 + "/servicios/" + proveedor.idServicio() + "/reservas"), null, proveedor.token())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Dado una fecha pasada, cuando el cliente reserva, entonces recibe 422 RESERVA_EN_EL_PASADO")
        void fechaPasada() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            String token = clienteConSesion();

            // Act - Assert
            reservar(token, proveedor.idServicio(), "2026-10-05T07:30:00-05:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("RESERVA_EN_EL_PASADO"));
        }

        @Test
        @DisplayName("Dado una hora que no coincide con un turno, cuando el cliente reserva, entonces recibe 422 HORARIO_NO_DISPONIBLE con hasta 3 sugerencias")
        void horaFueraDeTurno() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            String token = clienteConSesion();

            // Act - Assert
            reservar(token, proveedor.idServicio(), lunes("08:10"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errorCode").value("HORARIO_NO_DISPONIBLE"))
                    .andExpect(jsonPath("$.details.length()").value(3))
                    .andExpect(jsonPath("$.details[0].tipo").value("SUGERENCIA"))
                    .andExpect(jsonPath("$.details[0].fechaHoraInicio").value(lunes("08:30")));
            reservar(token, proveedor.idServicio(), "2026-10-11T08:00:00-05:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.details[0].fechaHoraInicio").value(lunes("08:00")));
        }

        @Test
        @DisplayName("Dado un turno sin cupo, cuando otro cliente lo reserva, entonces recibe 409 TURNO_SIN_CUPO con sugerencias de otros turnos")
        void turnoSinCupo() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("09:00")).andExpect(status().isCreated());

            // Act - Assert
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("09:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("TURNO_SIN_CUPO"))
                    .andExpect(jsonPath("$.details[0].fechaHoraInicio").value(lunes("09:30")))
                    .andExpect(jsonPath("$.details[*].fechaHoraInicio", not(hasItem(lunes("09:00")))));
        }

        @Test
        @DisplayName("Dado un cliente con una reserva confirmada, cuando reserva otro servicio que se cruza en tiempo, entonces recibe 409 RESERVA_SOLAPADA")
        void reservaSolapadaDelCliente() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(
                    List.of(servicio("Consulta", 30, 1), servicio("Terapia", 60, 1)));
            String token = clienteConSesion();
            reservar(token, proveedor.idsServicios().get(0), lunes("10:00")).andExpect(status().isCreated());

            // Act - Assert
            reservar(token, proveedor.idsServicios().get(1), lunes("10:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("RESERVA_SOLAPADA"));
            reservar(token, proveedor.idsServicios().get(1), lunes("11:00")).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Dado un servicio inexistente, un proveedor, sin token o una fecha sin offset, cuando reserva, entonces recibe 404, 403, 401 y 400")
        void erroresDeSolicitud() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            String token = clienteConSesion();

            // Act - Assert
            reservar(token, 999999, lunes("08:00"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_ENCONTRADO"));
            reservar(proveedor.token(), proveedor.idServicio(), lunes("08:00")).andExpect(status().isForbidden());
            reservar(null, proveedor.idServicio(), lunes("08:00")).andExpect(status().isUnauthorized());
            reservar(token, proveedor.idServicio(), "2026-10-12T08:00:00")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SOLICITUD_INVALIDA"));
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Otro", 30, 1)));
            enviar(get(V1 + "/servicios/" + proveedor.idServicio() + "/reservas"), null, otro.token())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Dado varios clientes que reservan el último cupo al mismo tiempo, cuando se procesan en paralelo, entonces solo uno obtiene la reserva")
        void sinSobreocupacionConcurrente() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            List<UsuarioAutenticado> clientes = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String email = emailUnico("concurrente");
                registrarCliente(email).andExpect(status().isCreated());
                clientes.add(new UsuarioAutenticado(idUsuarioDe(email, jdbc), Rol.CLIENTE));
            }
            CrearReservaComando comando = new CrearReservaComando(proveedor.idServicio(),
                    OffsetDateTime.parse(lunes("11:30")));
            CountDownLatch salida = new CountDownLatch(1);
            List<Callable<CodigoError>> tareas = clientes.stream().<Callable<CodigoError>>map(cliente -> () -> {
                salida.await();
                try {
                    crearReserva.crear(cliente, comando);
                    return null;
                } catch (ExcepcionNegocio e) {
                    return e.getCodigo();
                }
            }).toList();

            // Act
            List<CodigoError> resultados = new ArrayList<>();
            try (ExecutorService hilos = Executors.newFixedThreadPool(clientes.size())) {
                List<Future<CodigoError>> futuros = tareas.stream().map(hilos::submit).toList();
                salida.countDown();
                for (Future<CodigoError> futuro : futuros) {
                    resultados.add(futuro.get());
                }
            }

            // Assert
            assertThat(resultados).containsOnlyOnce((CodigoError) null);
            assertThat(resultados).filteredOn(codigo -> codigo != null).containsOnly(CodigoError.TURNO_SIN_CUPO);
        }
    }

    @Nested
    @DisplayName("HU-07 - Consultar disponibilidad")
    class Disponibilidad {

        @Test
        @DisplayName("Dado una agenda de 08:00 a 12:00 y una reserva, cuando se consulta la disponibilidad del día, entonces se listan los turnos libres con sus cupos")
        void turnosDisponibles() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Clase", 30, 2)));
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("08:00")).andExpect(status().isCreated());
            String ruta = V1 + "/servicios/" + proveedor.idServicio() + "/disponibilidad";

            // Act - Assert
            mockMvc.perform(get(ruta).param("desde", PROXIMO_LUNES).param("hasta", PROXIMO_LUNES))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.zonaHoraria").value("America/Bogota"))
                    .andExpect(jsonPath("$.turnos.length()").value(8))
                    .andExpect(jsonPath("$.turnos[0].fechaHoraInicio").value(lunes("08:00")))
                    .andExpect(jsonPath("$.turnos[0].cuposDisponibles").value(1))
                    .andExpect(jsonPath("$.turnos[1].cuposDisponibles").value(2));
            mockMvc.perform(get(ruta).param("desde", "2026-10-05").param("hasta", "2026-10-05"))
                    .andExpect(jsonPath("$.turnos.length()").value(7))
                    .andExpect(jsonPath("$.turnos[0].fechaHoraInicio").value("2026-10-05T08:30:00-05:00"));
        }

        @Test
        @DisplayName("Dado un rango inválido o mayor a 31 días, cuando se consulta la disponibilidad, entonces recibe 400 VALIDACION_FALLIDA")
        void rangoInvalido() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Consulta", 30, 1)));
            String ruta = V1 + "/servicios/" + proveedor.idServicio() + "/disponibilidad";

            // Act - Assert
            mockMvc.perform(get(ruta).param("desde", "2026-10-01").param("hasta", "2026-11-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDACION_FALLIDA"));
            mockMvc.perform(get(ruta).param("desde", "2026-10-12").param("hasta", "2026-10-11"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(ruta).param("desde", "2026-10-12"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].campo").value("hasta"));
            mockMvc.perform(get(ruta).param("desde", "12/10/2026").param("hasta", "2026-10-12"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SOLICITUD_INVALIDA"));
            mockMvc.perform(get(V1 + "/servicios/999999/disponibilidad").param("desde", PROXIMO_LUNES)
                            .param("hasta", PROXIMO_LUNES))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("HU-09 - Controlar disponibilidad de recursos")
    class Recursos {

        @Test
        @DisplayName("Dado dos servicios que comparten un recurso, cuando se reservan a la misma hora, entonces el segundo recibe 409 RECURSO_NO_DISPONIBLE con el recurso y sugerencias")
        void recursoCompartidoOcupado() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(
                    List.of(servicio("Limpieza", 30, 1), servicio("Blanqueamiento", 60, 1)));
            int sala = crearRecurso(proveedor.token(), "Consultorio 1");
            asignarRecursos(proveedor.token(), proveedor.idsServicios().get(0), List.of(sala));
            asignarRecursos(proveedor.token(), proveedor.idsServicios().get(1), List.of(sala));
            reservar(clienteConSesion(), proveedor.idsServicios().get(0), lunes("10:00"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recursos[0].nombre").value("Consultorio 1"));

            // Act - Assert
            reservar(clienteConSesion(), proveedor.idsServicios().get(1), lunes("10:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("RECURSO_NO_DISPONIBLE"))
                    .andExpect(jsonPath("$.details[0].tipo").value("RECURSO"))
                    .andExpect(jsonPath("$.details[0].idRecurso").value(sala))
                    .andExpect(jsonPath("$.details[0].nombre").value("Consultorio 1"))
                    .andExpect(jsonPath("$.details[1].tipo").value("SUGERENCIA"))
                    .andExpect(jsonPath("$.details[1].fechaHoraInicio").value(lunes("11:00")))
                    .andExpect(jsonPath("$.details.length()", lessThanOrEqualTo(4)));
            mockMvc.perform(get(V1 + "/servicios/" + proveedor.idsServicios().get(1) + "/disponibilidad")
                            .param("desde", PROXIMO_LUNES).param("hasta", PROXIMO_LUNES))
                    .andExpect(jsonPath("$.turnos[*].fechaHoraInicio", not(hasItem(lunes("10:00")))))
                    .andExpect(jsonPath("$.turnos[*].fechaHoraInicio", hasItem(lunes("11:00"))));
        }

        @Test
        @DisplayName("Dado una clase grupal con un recurso, cuando varios clientes reservan el mismo turno, entonces comparten el recurso hasta agotar la capacidad")
        void turnoGrupalCompartido() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConAgenda(List.of(servicio("Yoga", 60, 2)));
            int salon = crearRecurso(proveedor.token(), "Salón de yoga");
            asignarRecursos(proveedor.token(), proveedor.idServicio(), List.of(salon));

            // Act - Assert
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("08:00")).andExpect(status().isCreated());
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("08:00"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recursos[*].id", everyItem(is(salon))));
            reservar(clienteConSesion(), proveedor.idServicio(), lunes("08:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("TURNO_SIN_CUPO"));
        }
    }
}

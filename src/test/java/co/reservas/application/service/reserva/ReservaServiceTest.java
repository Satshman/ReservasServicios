package co.reservas.application.service.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reserva.CrearReservaComando;
import co.reservas.application.port.in.reserva.RecursoEnConflicto;
import co.reservas.application.port.in.reserva.ReservaResultado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.recurso.OcupacionRecurso;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(30, Rol.CLIENTE);
    private static final Proveedor PROVEEDOR = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);
    private static final OffsetDateTime LUNES_1000 = OffsetDateTime.parse("2026-10-12T10:00:00-05:00");
    private static final Recurso SALA = new Recurso(4, 3, 1, "Sala 1", true);

    @Mock
    private ReservaRepositoryPort reservas;
    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private AgendaRepositoryPort agenda;
    @Mock
    private RecursoRepositoryPort recursos;
    @Mock
    private VerificadorPropiedad verificador;

    private ReservaService servicio;

    @BeforeEach
    void configurar() {
        servicio = new ReservaService(reservas, servicios, usuarios, agenda, recursos,
                new CalculadoraDisponibilidad(reservas), verificador, Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(usuarios.bloquearClientePorUsuario(30)).thenReturn(Optional.of(new Cliente(8, 30)));
        lenient().when(servicios.bloquearPorId(7)).thenReturn(Optional.of(SERVICIO));
        lenient().when(usuarios.buscarProveedorPorId(3)).thenReturn(Optional.of(PROVEEDOR));
        lenient().when(agenda.listarPorServicio(7)).thenReturn(List.of(
                new HorarioDisponible(1, 7, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))));
    }

    private void assertCodigo(OffsetDateTime inicio, CodigoError codigo) {
        assertThatThrownBy(() -> servicio.crear(CLIENTE, new CrearReservaComando(7, inicio)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un turno libre con un recurso, cuando el cliente reserva, entonces guarda la reserva con el recurso y su historial")
    void reservaExitosa() {
        // Arrange
        when(recursos.bloquearActivosPorServicio(7)).thenReturn(List.of(SALA));
        when(reservas.guardar(any())).thenAnswer(invocacion -> ((Reserva) invocacion.getArgument(0)).conId(99));

        // Act
        ReservaResultado resultado = servicio.crear(CLIENTE, new CrearReservaComando(7, LUNES_1000));

        // Assert
        assertThat(resultado.id()).isEqualTo(99);
        assertThat(resultado.fechaHoraFin()).isEqualTo(OffsetDateTime.parse("2026-10-12T10:30:00-05:00"));
        assertThat(resultado.recursos()).singleElement().satisfies(r -> assertThat(r.nombre()).isEqualTo("Sala 1"));
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(reservas).registrarHistorial(historial.capture());
        assertThat(historial.getValue().idUsuario()).isEqualTo(30);
        assertThat(historial.getValue().estadoAnterior()).isNull();
    }

    @Test
    @DisplayName("Dado un usuario sin perfil de cliente o un servicio inexistente o inactivo, cuando reserva, entonces responde 403, 404 o 422")
    void clienteYServicio() {
        // Arrange
        Servicio inactivo = new Servicio(9, 3, 1, EstadoServicio.INACTIVO, "Pausado", null, Duration.ofMinutes(30), 1);
        when(servicios.bloquearPorId(8)).thenReturn(Optional.empty());
        when(servicios.bloquearPorId(9)).thenReturn(Optional.of(inactivo));
        UsuarioAutenticado sinPerfil = new UsuarioAutenticado(31, Rol.CLIENTE);

        // Act - Assert
        assertThatThrownBy(() -> servicio.crear(sinPerfil, new CrearReservaComando(7, LUNES_1000)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.ACCESO_DENEGADO));
        assertThatThrownBy(() -> servicio.crear(CLIENTE, new CrearReservaComando(8, LUNES_1000)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.SERVICIO_NO_ENCONTRADO));
        assertThatThrownBy(() -> servicio.crear(CLIENTE, new CrearReservaComando(9, LUNES_1000)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.SERVICIO_NO_DISPONIBLE));
    }

    @Test
    @DisplayName("Dado un inicio pasado o fuera de turno, cuando reserva, entonces responde RESERVA_EN_EL_PASADO u HORARIO_NO_DISPONIBLE")
    void validacionesDeTiempo() {
        // Arrange
        lenient().when(reservas.contarConfirmadasPorTurno(any(), any(), any())).thenReturn(java.util.Map.of());

        // Act - Assert
        assertCodigo(OffsetDateTime.parse("2026-10-05T08:00:00-05:00"), CodigoError.RESERVA_EN_EL_PASADO);
        assertCodigo(OffsetDateTime.parse("2026-10-12T10:05:00-05:00"), CodigoError.HORARIO_NO_DISPONIBLE);
        verify(reservas, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado un turno lleno o un solape del cliente, cuando reserva, entonces responde TURNO_SIN_CUPO o RESERVA_SOLAPADA")
    void cupoYSolape() {
        // Arrange
        Instant inicio = LUNES_1000.toInstant();
        when(reservas.contarConfirmadasEnTurno(7, inicio)).thenReturn(1L).thenReturn(0L);
        lenient().when(reservas.contarConfirmadasPorTurno(any(), any(), any())).thenReturn(java.util.Map.of());
        when(reservas.existeSolapeCliente(8, inicio, inicio.plus(Duration.ofMinutes(30)))).thenReturn(true);

        // Act - Assert
        assertCodigo(LUNES_1000, CodigoError.TURNO_SIN_CUPO);
        assertCodigo(LUNES_1000, CodigoError.RESERVA_SOLAPADA);
    }

    @Test
    @DisplayName("Dado un recurso ocupado por otro servicio, cuando reserva, entonces responde RECURSO_NO_DISPONIBLE con el recurso en conflicto primero")
    void recursoOcupado() {
        // Arrange
        Instant inicio = LUNES_1000.toInstant();
        when(recursos.bloquearActivosPorServicio(7)).thenReturn(List.of(SALA));
        when(reservas.listarOcupaciones(any(), any(), any()))
                .thenReturn(List.of(new OcupacionRecurso(4, 70, 55, inicio, inicio.plusSeconds(3600))));
        when(reservas.contarConfirmadasPorTurno(any(), any(), any())).thenReturn(java.util.Map.of());

        // Act - Assert
        assertThatThrownBy(() -> servicio.crear(CLIENTE, new CrearReservaComando(7, LUNES_1000)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.RECURSO_NO_DISPONIBLE);
                    assertThat(e.getDetalles().getFirst()).isEqualTo(new RecursoEnConflicto(4, "Sala 1"));
                    assertThat(e.getDetalles()).hasSizeBetween(2, 4);
                });
        verify(reservas, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado reservas del cliente con recursos, cuando consulta sus reservas, entonces se devuelven en la zona del proveedor")
    void consultarMias() {
        // Arrange
        Instant inicio = LUNES_1000.toInstant();
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(new Cliente(8, 30)));
        when(reservas.listarPorCliente(8)).thenReturn(List.of(new Reserva(5, 8, 7, EstadoReserva.CONFIRMADA, inicio,
                inicio.plusSeconds(1800), AHORA, Set.of(4))));
        when(servicios.buscarPorIds(List.of(7))).thenReturn(List.of(SERVICIO));
        when(usuarios.buscarProveedoresPorIds(List.of(3))).thenReturn(List.of(PROVEEDOR));
        when(recursos.buscarPorIds(List.of(4))).thenReturn(List.of(SALA));

        // Act
        List<ReservaResultado> mias = servicio.consultarMias(CLIENTE);

        // Assert
        assertThat(mias).singleElement().satisfies(reserva -> {
            assertThat(reserva.fechaHoraInicio()).isEqualTo(LUNES_1000);
            assertThat(reserva.nombreServicio()).isEqualTo("Consulta");
            assertThat(reserva.recursos()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Dado un rango de fechas inválido o un servicio inactivo, cuando se consulta disponibilidad, entonces responde VALIDACION_FALLIDA o SERVICIO_NO_DISPONIBLE")
    void disponibilidad() {
        // Arrange
        Servicio inactivo = new Servicio(9, 3, 1, EstadoServicio.INACTIVO, "Pausado", null, Duration.ofMinutes(30), 1);
        when(servicios.buscarPorId(9)).thenReturn(Optional.of(inactivo));
        DisponibilidadService disponibilidad = new DisponibilidadService(servicios, usuarios, agenda, recursos,
                new CalculadoraDisponibilidad(reservas), Clock.fixed(AHORA, ZoneOffset.UTC));
        LocalDate lunes = LocalDate.of(2026, 10, 12);

        // Act - Assert
        assertThatThrownBy(() -> disponibilidad.consultar(7, null, lunes)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> disponibilidad.consultar(7, lunes, null)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> disponibilidad.consultar(7, lunes, lunes.plusDays(31)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION_FALLIDA));
        assertThatThrownBy(() -> disponibilidad.consultar(9, lunes, lunes.plusDays(30)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.SERVICIO_NO_DISPONIBLE));
    }
}

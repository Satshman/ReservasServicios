package co.reservas.application.service.agenda;

import co.reservas.application.port.in.agenda.CambioHorarioResultado;
import co.reservas.application.port.in.agenda.CrearHorarioComando;
import co.reservas.application.port.in.agenda.EditarHorarioComando;
import co.reservas.application.port.in.agenda.HorarioResultado;
import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final UsuarioAutenticado PROVEEDOR = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);
    private static final ServicioPropio PROPIO = new ServicioPropio(SERVICIO,
            new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota")));
    private static final HorarioDisponible LUNES =
            new HorarioDisponible(11, 7, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

    @Mock
    private AgendaRepositoryPort agenda;
    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private ReservaRepositoryPort reservas;
    @Mock
    private VerificadorPropiedad verificador;

    private AgendaService servicio;

    @BeforeEach
    void configurar() {
        servicio = new AgendaService(agenda, servicios, reservas, verificador, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Reserva reservaLunes1000() {
        Instant inicio = Instant.parse("2026-10-12T15:00:00Z");
        return new Reserva(90, 1, 7, EstadoReserva.CONFIRMADA, inicio, inicio.plus(Duration.ofMinutes(30)), AHORA,
                Set.of());
    }

    @Test
    @DisplayName("Dado días válidos sin solapes, cuando crea la agenda, entonces guarda un bloque por día ordenado")
    void crea() {
        // Arrange
        when(verificador.servicioPropioBloqueado(PROVEEDOR, 7)).thenReturn(PROPIO);
        when(agenda.listarPorServicio(7)).thenReturn(List.of());
        when(agenda.guardarTodos(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        List<HorarioResultado> creados = servicio.crear(PROVEEDOR, 7,
                new CrearHorarioComando(Set.of(5, 2), LocalTime.of(8, 0), LocalTime.of(9, 0)));

        // Assert
        assertThat(creados).extracting(HorarioResultado::diaSemana).containsExactly(2, 5);
    }

    @Test
    @DisplayName("Dado una lista de días vacía, cuando crea la agenda, entonces responde VALIDACION_FALLIDA")
    void sinDias() {
        // Arrange
        when(verificador.servicioPropioBloqueado(PROVEEDOR, 7)).thenReturn(PROPIO);

        // Act - Assert
        assertThatThrownBy(() -> servicio.crear(PROVEEDOR, 7,
                new CrearHorarioComando(Set.of(), LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION_FALLIDA));
    }

    @Test
    @DisplayName("Dado una reserva afectada y confirmar=false, cuando edita, entonces responde HORARIO_CON_RESERVAS sin guardar")
    void editarRequiereConfirmacion() {
        // Arrange
        when(agenda.buscarPorId(11)).thenReturn(Optional.of(LUNES));
        when(verificador.servicioPropioBloqueado(PROVEEDOR, 7)).thenReturn(PROPIO);
        when(agenda.listarPorServicio(7)).thenReturn(List.of(LUNES));
        when(reservas.listarConfirmadasFuturasPorServicio(7, AHORA)).thenReturn(List.of(reservaLunes1000()));
        EditarHorarioComando comando = new EditarHorarioComando(1, LocalTime.of(8, 0), LocalTime.of(10, 0));

        // Act - Assert
        assertThatThrownBy(() -> servicio.editar(PROVEEDOR, 11, comando, false))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.HORARIO_CON_RESERVAS);
                    assertThat(e.getDetalles()).hasSize(1);
                });
        verify(agenda, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado una reserva afectada y confirmar=true, cuando elimina, entonces elimina el bloque y devuelve la reserva afectada")
    void eliminarConfirmado() {
        // Arrange
        when(agenda.buscarPorId(11)).thenReturn(Optional.of(LUNES));
        when(verificador.servicioPropioBloqueado(PROVEEDOR, 7)).thenReturn(PROPIO);
        when(reservas.listarConfirmadasFuturasPorServicio(7, AHORA)).thenReturn(List.of(reservaLunes1000()));

        // Act
        CambioHorarioResultado resultado = servicio.eliminar(PROVEEDOR, 11, true);

        // Assert
        verify(agenda).eliminar(11);
        assertThat(resultado.horario()).isNull();
        assertThat(resultado.reservasAfectadas()).singleElement()
                .satisfies(afectada -> assertThat(afectada.fechaHoraInicio().toString())
                        .isEqualTo("2026-10-12T10:00-05:00"));
    }

    @Test
    @DisplayName("Dado un horario inexistente, cuando se edita o elimina, entonces responde HORARIO_NO_ENCONTRADO")
    void horarioInexistente() {
        // Arrange
        when(agenda.buscarPorId(404)).thenReturn(Optional.empty());

        // Act - Assert
        assertThatThrownBy(() -> servicio.eliminar(PROVEEDOR, 404, false))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.HORARIO_NO_ENCONTRADO));
    }

    @Test
    @DisplayName("Dado un servicio con bloques, cuando se consulta la agenda, entonces se listan por día y hora; si no existe responde SERVICIO_NO_ENCONTRADO")
    void consultar() {
        // Arrange
        HorarioDisponible martes = new HorarioDisponible(12, 7, DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(9, 0));
        when(servicios.buscarPorId(7)).thenReturn(Optional.of(SERVICIO));
        when(servicios.buscarPorId(8)).thenReturn(Optional.empty());
        when(agenda.listarPorServicio(7)).thenReturn(List.of(martes, LUNES));

        // Act
        List<HorarioResultado> horarios = servicio.consultar(7);

        // Assert
        assertThat(horarios).extracting(HorarioResultado::id).containsExactly(11, 12);
        assertThatThrownBy(() -> servicio.consultar(8)).isInstanceOf(ExcepcionNegocio.class);
    }
}

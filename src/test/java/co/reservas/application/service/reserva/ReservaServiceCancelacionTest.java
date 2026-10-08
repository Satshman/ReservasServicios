package co.reservas.application.service.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reserva.ReservaResultado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.PoliticaCancelacion;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceCancelacionTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-10-12T15:00:00Z");
    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(30, Rol.CLIENTE);
    private static final UsuarioAutenticado DUENO = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Proveedor PROVEEDOR = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);
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
                new CalculadoraDisponibilidad(reservas), verificador, new PoliticaCancelacion(Duration.ofDays(5)),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Reserva reserva(EstadoReserva estado, Instant inicio) {
        return new Reserva(5, 8, 7, estado, inicio, inicio.plusSeconds(1800), AHORA, Set.of(4));
    }

    private void stubMapeo() {
        when(servicios.buscarPorIds(List.of(7))).thenReturn(List.of(SERVICIO));
        when(usuarios.buscarProveedoresPorIds(List.of(3))).thenReturn(List.of(PROVEEDOR));
        when(recursos.buscarPorIds(List.of(4))).thenReturn(List.of(SALA));
    }

    private void assertCancelarFalla(UsuarioAutenticado usuario, CodigoError codigo) {
        assertThatThrownBy(() -> servicio.cancelar(usuario, 5))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
        verify(reservas, never()).actualizarEstado(any());
        verify(reservas, never()).registrarHistorial(any());
    }

    @Test
    @DisplayName("Dado una reserva confirmada de un cliente, cuando el cliente la cancela, entonces se guarda CANCELADA, se registra el historial y se devuelve en la zona del proveedor")
    void clienteCancelaSuReserva() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA, INICIO);
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(confirmada));
        stubMapeo();

        // Act
        ReservaResultado resultado = servicio.cancelar(CLIENTE, 5);

        // Assert
        verify(verificador).exigirAccesoAReserva(CLIENTE, confirmada);
        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);
        InOrder orden = inOrder(reservas);
        orden.verify(reservas).actualizarEstado(guardada.capture());
        orden.verify(reservas).registrarHistorial(historial.capture());
        assertThat(guardada.getValue().estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(guardada.getValue().id()).isEqualTo(5);
        assertThat(historial.getValue().idReserva()).isEqualTo(5);
        assertThat(historial.getValue().estadoAnterior()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(historial.getValue().estadoNuevo()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(historial.getValue().idUsuario()).isEqualTo(30);
        assertThat(historial.getValue().fechaCambio()).isEqualTo(AHORA);
        assertThat(resultado.id()).isEqualTo(5);
        assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(resultado.nombreServicio()).isEqualTo("Consulta");
        assertThat(resultado.fechaHoraInicio()).isEqualTo(OffsetDateTime.parse("2026-10-12T10:00:00-05:00"));
        assertThat(resultado.recursos()).singleElement().satisfies(r -> assertThat(r.nombre()).isEqualTo("Sala 1"));
    }

    @Test
    @DisplayName("Dado una reserva de su servicio, cuando el proveedor dueño la cancela, entonces el historial registra al proveedor como autor del cambio")
    void proveedorCancelaReservaDeSuServicio() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA, INICIO);
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(confirmada));
        stubMapeo();

        // Act
        ReservaResultado resultado = servicio.cancelar(DUENO, 5);

        // Assert
        verify(verificador).exigirAccesoAReserva(DUENO, confirmada);
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(reservas).registrarHistorial(historial.capture());
        assertThat(historial.getValue().idUsuario()).isEqualTo(20);
        assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Dado un id de reserva inexistente, cuando se cancela, entonces responde RESERVA_NO_ENCONTRADA sin verificar propiedad ni modificar nada")
    void reservaInexistente() {
        // Arrange
        when(reservas.bloquearPorId(5)).thenReturn(Optional.empty());

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.RESERVA_NO_ENCONTRADA);
        verify(verificador, never()).exigirAccesoAReserva(any(), any());
    }

    @Test
    @DisplayName("Dado un usuario que no es dueño de la reserva, cuando intenta cancelarla, entonces responde ACCESO_DENEGADO sin modificar nada")
    void usuarioAjeno() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA, INICIO);
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(confirmada));
        doThrow(VerificadorPropiedad.accesoDenegado("La reserva no pertenece al usuario autenticado."))
                .when(verificador).exigirAccesoAReserva(CLIENTE, confirmada);

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.ACCESO_DENEGADO);
    }

    @Test
    @DisplayName("Dado una reserva ya cancelada, cuando se cancela de nuevo, entonces responde RESERVA_NO_CANCELABLE y no duplica el historial")
    void reservaYaCancelada() {
        // Arrange
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(reserva(EstadoReserva.CANCELADA, INICIO)));

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.RESERVA_NO_CANCELABLE);
    }

    @Test
    @DisplayName("Dado una reserva completada, cuando el cliente o el proveedor la cancelan, entonces responde RESERVA_NO_CANCELABLE sin modificar nada")
    void reservaCompletada() {
        // Arrange
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(reserva(EstadoReserva.COMPLETADA, INICIO)));

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.RESERVA_NO_CANCELABLE);
        assertCancelarFalla(DUENO, CodigoError.RESERVA_NO_CANCELABLE);
    }

    @Test
    @DisplayName("Dado una reserva que ya comenzó, cuando el proveedor dueño la cancela, entonces responde RESERVA_EN_EL_PASADO porque sin ventana igual debe cancelar antes del inicio")
    void proveedorReservaYaIniciada() {
        // Arrange
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, AHORA)));

        // Act - Assert
        assertCancelarFalla(DUENO, CodigoError.RESERVA_EN_EL_PASADO);
    }

    @Test
    @DisplayName("Dado una reserva que comienza en exactamente 5 días, cuando el cliente la cancela, entonces se permite")
    void clienteEnElLimiteDelPlazo() {
        // Arrange
        when(reservas.bloquearPorId(5))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, AHORA.plus(Duration.ofDays(5)))));
        stubMapeo();

        // Act
        ReservaResultado resultado = servicio.cancelar(CLIENTE, 5);

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(reservas).registrarHistorial(any());
    }

    @Test
    @DisplayName("Dado una reserva que ya comenzó, cuando se cancela, entonces responde RESERVA_EN_EL_PASADO sin modificar nada")
    void reservaYaIniciada() {
        // Arrange
        when(reservas.bloquearPorId(5))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, AHORA.minus(Duration.ofMinutes(10)))));

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.RESERVA_EN_EL_PASADO);
    }

    @Test
    @DisplayName("Dado una reserva que comienza en 4 días, cuando el cliente la cancela, entonces responde CANCELACION_FUERA_DE_PLAZO sin modificar nada")
    void clienteFueraDePlazo() {
        // Arrange
        when(reservas.bloquearPorId(5))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, AHORA.plus(Duration.ofDays(4)))));

        // Act - Assert
        assertCancelarFalla(CLIENTE, CodigoError.CANCELACION_FUERA_DE_PLAZO);
    }

    @Test
    @DisplayName("Dado una reserva que comienza en 4 días, cuando el proveedor dueño la cancela, entonces se permite porque la ventana solo aplica al cliente")
    void proveedorSinVentana() {
        // Arrange
        Reserva proxima = reserva(EstadoReserva.CONFIRMADA, AHORA.plus(Duration.ofDays(4)));
        when(reservas.bloquearPorId(5)).thenReturn(Optional.of(proxima));
        stubMapeo();

        // Act
        ReservaResultado resultado = servicio.cancelar(DUENO, 5);

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(reservas).actualizarEstado(any());
    }
}


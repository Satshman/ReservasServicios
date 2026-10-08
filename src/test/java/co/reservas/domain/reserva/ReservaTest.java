package co.reservas.domain.reserva;

import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-10-12T15:00:00Z");
    private static final Duration SIN_VENTANA = Duration.ZERO;
    private static final Duration CINCO_DIAS = Duration.ofDays(5);

    @Test
    @DisplayName("Dado un servicio de 45 minutos, cuando se crea la reserva, entonces queda CONFIRMADA y termina 45 minutos después")
    void crearCalculaFin() {
        // Arrange
        Servicio servicio = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Terapia", null, Duration.ofMinutes(45), 1);

        // Act
        Reserva reserva = Reserva.crear(2, servicio, INICIO, Set.of(1, 2), AHORA).conId(50);
        HistorialReserva historial = HistorialReserva.creacion(reserva, 9, AHORA);

        // Assert
        assertThat(reserva.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reserva.fechaHoraFin()).isEqualTo(INICIO.plus(Duration.ofMinutes(45)));
        assertThat(reserva.id()).isEqualTo(50);
        assertThat(historial.estadoAnterior()).isNull();
        assertThat(historial.estadoNuevo()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(historial.idReserva()).isEqualTo(50);
    }

    @Test
    @DisplayName("Dado una reserva de 10:00 a 10:30, cuando se compara con otros intervalos, entonces solo se solapa si comparten tiempo")
    void solapes() {
        // Arrange
        Reserva reserva = new Reserva(1, 2, 7, EstadoReserva.CONFIRMADA, INICIO, INICIO.plusSeconds(1800), AHORA, null);

        // Act - Assert
        assertThat(reserva.idsRecursos()).isEmpty();
        assertThat(reserva.seSolapaCon(INICIO.plusSeconds(1799), INICIO.plusSeconds(3600))).isTrue();
        assertThat(reserva.seSolapaCon(INICIO.plusSeconds(1800), INICIO.plusSeconds(3600))).isFalse();
        assertThat(reserva.seSolapaCon(INICIO.minusSeconds(1800), INICIO)).isFalse();
    }

    @Test
    @DisplayName("Dado un fin anterior al inicio, cuando se construye la reserva, entonces se rechaza")
    void rangoInvalido() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> new Reserva(1, 2, 7, EstadoReserva.CONFIRMADA, INICIO, INICIO, AHORA, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Dado un recurso nuevo, cuando se crea, entonces queda activo y normaliza el nombre; un nombre vacío se rechaza")
    void recursoNuevo() {
        // Arrange - Act
        Recurso recurso = Recurso.nuevo(3, 1, "  Sala 1 ");

        // Assert
        assertThat(recurso.activo()).isTrue();
        assertThat(recurso.nombre()).isEqualTo("Sala 1");
        assertThat(recurso.perteneceA(3)).isTrue();
        assertThatThrownBy(() -> Recurso.nuevo(3, 1, " ")).isInstanceOf(ExcepcionNegocio.class);
    }

    private static Reserva reserva(EstadoReserva estado) {
        return new Reserva(50, 2, 7, estado, INICIO, INICIO.plusSeconds(1800), AHORA, Set.of(1, 2));
    }

    private static void assertCancelarFalla(Reserva reserva, Instant ahora, CodigoError codigo) {
        assertCancelarFalla(reserva, ahora, SIN_VENTANA, codigo);
    }

    private static void assertCancelarFalla(Reserva reserva, Instant ahora, Duration ventana, CodigoError codigo) {
        assertThatThrownBy(() -> reserva.cancelar(ahora, ventana))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado una reserva confirmada que aún no comienza, cuando se cancela, entonces queda CANCELADA y conserva el resto de sus datos")
    void cancelarReservaConfirmada() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);

        // Act
        Reserva cancelada = confirmada.cancelar(AHORA, CINCO_DIAS);

        // Assert
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(cancelada.id()).isEqualTo(50);
        assertThat(cancelada.idCliente()).isEqualTo(2);
        assertThat(cancelada.idServicio()).isEqualTo(7);
        assertThat(cancelada.fechaHoraInicio()).isEqualTo(INICIO);
        assertThat(cancelada.fechaHoraFin()).isEqualTo(confirmada.fechaHoraFin());
        assertThat(cancelada.creadoEn()).isEqualTo(AHORA);
        assertThat(cancelada.idsRecursos()).containsExactlyInAnyOrder(1, 2);
        assertThat(confirmada.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("Dado una reserva que comienza en un segundo y sin ventana mínima, cuando se cancela, entonces todavía se permite")
    void cancelarJustoAntesDelInicio() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);

        // Act
        Reserva cancelada = confirmada.cancelar(INICIO.minusSeconds(1), SIN_VENTANA);

        // Assert
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Dado una reserva ya cancelada o completada, cuando se cancela, entonces se rechaza con RESERVA_NO_CANCELABLE")
    void cancelarReservaEnEstadoFinal() {
        // Arrange
        Reserva cancelada = reserva(EstadoReserva.CANCELADA);
        Reserva completada = reserva(EstadoReserva.COMPLETADA);

        // Act - Assert
        assertCancelarFalla(cancelada, AHORA, CodigoError.RESERVA_NO_CANCELABLE);
        assertCancelarFalla(completada, AHORA, CodigoError.RESERVA_NO_CANCELABLE);
    }

    @Test
    @DisplayName("Dado una reserva que ya comenzó o está por comenzar en este instante, cuando se cancela, entonces se rechaza con RESERVA_EN_EL_PASADO")
    void cancelarReservaIniciada() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);

        // Act - Assert
        assertCancelarFalla(confirmada, INICIO, CodigoError.RESERVA_EN_EL_PASADO);
        assertCancelarFalla(confirmada, INICIO.plusSeconds(600), CodigoError.RESERVA_EN_EL_PASADO);
        assertCancelarFalla(confirmada, INICIO, CINCO_DIAS, CodigoError.RESERVA_EN_EL_PASADO);
    }

    @Test
    @DisplayName("Dado una ventana mínima de 5 días, cuando se cancela con exactamente 5 días de anticipación, entonces se permite")
    void cancelarEnElLimiteDeLaVentana() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);

        // Act
        Reserva cancelada = confirmada.cancelar(INICIO.minus(CINCO_DIAS), CINCO_DIAS);

        // Assert
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Dado una ventana mínima de 5 días, cuando faltan menos de 5 días para la reserva, entonces se rechaza con CANCELACION_FUERA_DE_PLAZO")
    void cancelarDentroDeLaVentana() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);

        // Act - Assert
        assertCancelarFalla(confirmada, INICIO.minus(CINCO_DIAS).plusSeconds(1), CINCO_DIAS,
                CodigoError.CANCELACION_FUERA_DE_PLAZO);
        assertCancelarFalla(confirmada, INICIO.minusSeconds(1), CINCO_DIAS, CodigoError.CANCELACION_FUERA_DE_PLAZO);
    }

    @Test
    @DisplayName("Dado distintas ventanas mínimas, cuando la cancelación se rechaza, entonces el mensaje indica la anticipación exigida")
    void mensajeDeLaVentana() {
        // Arrange
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
        Instant ahora = INICIO.minusSeconds(60);

        // Act - Assert
        assertThatThrownBy(() -> confirmada.cancelar(ahora, CINCO_DIAS)).hasMessageContaining("5 días");
        assertThatThrownBy(() -> confirmada.cancelar(ahora, Duration.ofDays(1))).hasMessageContaining("1 día ");
        assertThatThrownBy(() -> confirmada.cancelar(ahora, Duration.ofHours(36))).hasMessageContaining("36 horas");
        assertThatThrownBy(() -> confirmada.cancelar(ahora, Duration.ofHours(1))).hasMessageContaining("1 hora ");
        assertThatThrownBy(() -> confirmada.cancelar(ahora, Duration.ofMinutes(90))).hasMessageContaining("90 minutos");
    }

    @Test
    @DisplayName("Dado una reserva cancelada, cuando se calcula la transición, entonces el historial registra el estado anterior, el nuevo y quién la hizo")
    void historialDeCancelacion() {
        // Arrange
        Reserva cancelada = reserva(EstadoReserva.CONFIRMADA).cancelar(AHORA, CINCO_DIAS);

        // Act
        HistorialReserva historial = HistorialReserva.cambioDeEstado(EstadoReserva.CONFIRMADA, cancelada, 30, AHORA);

        // Assert
        assertThat(historial.id()).isNull();
        assertThat(historial.idReserva()).isEqualTo(50);
        assertThat(historial.estadoAnterior()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(historial.estadoNuevo()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(historial.idUsuario()).isEqualTo(30);
        assertThat(historial.fechaCambio()).isEqualTo(AHORA);
    }
}

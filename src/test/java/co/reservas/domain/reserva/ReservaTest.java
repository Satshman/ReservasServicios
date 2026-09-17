package co.reservas.domain.reserva;

import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
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
}

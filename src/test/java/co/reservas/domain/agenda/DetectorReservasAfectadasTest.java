package co.reservas.domain.agenda;

import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorReservasAfectadasTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final HorarioDisponible LUNES_8_A_12 =
            new HorarioDisponible(1, 7, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

    private static Reserva reserva(int id, String inicioUtc, EstadoReserva estado, int idServicio) {
        Instant inicio = Instant.parse(inicioUtc);
        return new Reserva(id, 1, idServicio, estado, inicio, inicio.plus(Duration.ofMinutes(30)), AHORA, Set.of());
    }

    @Test
    @DisplayName("Dado una reserva a las 10:00 de Bogotá, cuando el bloque se reduce a 08:00-10:00, entonces la reserva queda afectada")
    void reservaQueYaNoCabe() {
        // Arrange
        Reserva lunes1000 = reserva(1, "2026-10-12T15:00:00Z", EstadoReserva.CONFIRMADA, 7);
        HorarioDisponible nuevo = new HorarioDisponible(1, 7, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        // Act
        List<Reserva> afectadas = DetectorReservasAfectadas.alEditar(LUNES_8_A_12, nuevo, List.of(lunes1000), BOGOTA,
                AHORA);

        // Assert
        assertThat(afectadas).containsExactly(lunes1000);
    }

    @Test
    @DisplayName("Dado una reserva que en UTC cae el martes pero en Bogotá es lunes, cuando se elimina el bloque del lunes, entonces se detecta como afectada")
    void usaLaZonaDelProveedor() {
        // Arrange: lunes 20:00 en Bogotá = martes 01:00 UTC
        HorarioDisponible lunesNoche =
                new HorarioDisponible(2, 7, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0));
        Reserva lunes2000Bogota = reserva(2, "2026-10-13T01:00:00Z", EstadoReserva.CONFIRMADA, 7);

        // Act
        List<Reserva> enBogota = DetectorReservasAfectadas.alEliminar(lunesNoche, List.of(lunes2000Bogota), BOGOTA,
                AHORA);
        List<Reserva> enUtc = DetectorReservasAfectadas.alEliminar(lunesNoche, List.of(lunes2000Bogota),
                ZoneId.of("UTC"), AHORA);

        // Assert
        assertThat(enBogota).containsExactly(lunes2000Bogota);
        assertThat(enUtc).isEmpty();
    }

    @Test
    @DisplayName("Dado reservas pasadas, canceladas, de otro servicio o que siguen cabiendo, cuando se edita el bloque, entonces no se consideran afectadas")
    void descartaNoAfectadas() {
        // Arrange
        List<Reserva> reservas = List.of(
                reserva(1, "2026-10-05T14:00:00Z", EstadoReserva.CONFIRMADA, 7),
                reserva(2, "2026-10-12T15:00:00Z", EstadoReserva.CANCELADA, 7),
                reserva(3, "2026-10-12T15:00:00Z", EstadoReserva.CONFIRMADA, 99),
                reserva(4, "2026-10-12T14:00:00Z", EstadoReserva.CONFIRMADA, 7));
        HorarioDisponible nuevo = new HorarioDisponible(1, 7, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Act
        List<Reserva> afectadas = DetectorReservasAfectadas.alEditar(LUNES_8_A_12, nuevo, reservas, BOGOTA, AHORA);

        // Assert
        assertThat(afectadas).isEmpty();
    }

    @Test
    @DisplayName("Dado que el bloque se mueve a otro día, cuando se edita, entonces las reservas del día original quedan afectadas en orden cronológico")
    void cambioDeDia() {
        // Arrange
        Reserva segunda = reserva(5, "2026-10-19T14:00:00Z", EstadoReserva.CONFIRMADA, 7);
        Reserva primera = reserva(6, "2026-10-12T14:00:00Z", EstadoReserva.CONFIRMADA, 7);
        HorarioDisponible martes = new HorarioDisponible(1, 7, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        // Act
        List<Reserva> afectadas = DetectorReservasAfectadas.alEditar(LUNES_8_A_12, martes, List.of(segunda, primera),
                BOGOTA, AHORA);

        // Assert
        assertThat(afectadas).containsExactly(primera, segunda);
    }
}

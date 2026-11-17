package co.reservas.domain.agenda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorTurnosTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private static HorarioDisponible bloque(DayOfWeek dia, String inicio, String fin) {
        return new HorarioDisponible(1, 1, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    @Test
    @DisplayName("Dado un bloque de 08:00 a 10:10 con turnos de 30 minutos, cuando se generan turnos, entonces se crean 4 turnos y el sobrante se descarta")
    void generaTurnosDelBloque() {
        // Arrange
        List<HorarioDisponible> bloques = List.of(bloque(DayOfWeek.MONDAY, "08:00", "10:10"));

        // Act
        List<Turno> turnos = GeneradorTurnos.generar(bloques, Duration.ofMinutes(30), BOGOTA, LUNES, LUNES);

        // Assert
        assertThat(turnos).hasSize(4);
        assertThat(turnos.getFirst().inicio()).isEqualTo(Instant.parse("2026-10-12T13:00:00Z"));
        assertThat(turnos.getLast().fin()).isEqualTo(Instant.parse("2026-10-12T15:00:00Z"));
    }

    @Test
    @DisplayName("Dado bloques de lunes y miércoles, cuando se generan turnos de una semana, entonces solo aparecen esos días ordenados")
    void soloDiasConBloque() {
        // Arrange
        List<HorarioDisponible> bloques = List.of(bloque(DayOfWeek.WEDNESDAY, "14:00", "15:00"),
                bloque(DayOfWeek.MONDAY, "08:00", "09:00"));

        // Act
        List<Turno> turnos = GeneradorTurnos.generar(bloques, Duration.ofMinutes(60), BOGOTA, LUNES, LUNES.plusDays(6));

        // Assert
        assertThat(turnos).extracting(Turno::inicio).containsExactly(
                Instant.parse("2026-10-12T13:00:00Z"), Instant.parse("2026-10-14T19:00:00Z"));
    }

    @Test
    @DisplayName("Dado un instante UTC, cuando se valida el turno, entonces se interpreta en la zona horaria del proveedor")
    void validaEnZonaDelProveedor() {
        // Arrange
        List<HorarioDisponible> bloques = List.of(bloque(DayOfWeek.MONDAY, "08:00", "12:00"));
        Instant lunes0800Bogota = Instant.parse("2026-10-12T13:00:00Z");

        // Act - Assert
        assertThat(GeneradorTurnos.esTurnoValido(bloques, Duration.ofMinutes(30), BOGOTA, lunes0800Bogota)).isTrue();
        assertThat(GeneradorTurnos.esTurnoValido(bloques, Duration.ofMinutes(30), ZoneId.of("Europe/Madrid"),
                lunes0800Bogota)).isFalse();
        assertThat(GeneradorTurnos.esTurnoValido(bloques, Duration.ofMinutes(30), BOGOTA,
                lunes0800Bogota.plusSeconds(60))).isFalse();
    }
}

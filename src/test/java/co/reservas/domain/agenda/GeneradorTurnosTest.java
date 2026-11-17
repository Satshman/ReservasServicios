package co.reservas.domain.agenda;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorTurnosTest {

    @Test
    @DisplayName("Dado un bloque horario semanal, cuando se generan turnos en un rango de fechas, entonces devuelve los turnos alineados a la duración del servicio")
    void generaTurnosAlineadosEnZonaDelProveedor() {
        // Arrange
        ZoneId zona = ZoneId.of("America/Bogota");
        LocalDate lunes = LocalDate.of(2026, 9, 21);
        Duration duracion = Duration.ofMinutes(30);
        HorarioDisponible bloque = new HorarioDisponible(
                1, 10, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0));

        // Act
        List<Turno> turnos = GeneradorTurnos.generar(List.of(bloque), duracion, zona, lunes, lunes);

        // Assert
        Instant t1 = ZonedDateTime.of(lunes, LocalTime.of(8, 0), zona).toInstant();
        Instant t2 = ZonedDateTime.of(lunes, LocalTime.of(8, 30), zona).toInstant();
        assertEquals(List.of(
                new Turno(t1, t1.plus(duracion)),
                new Turno(t2, t2.plus(duracion))
        ), turnos);
        assertTrue(GeneradorTurnos.esTurnoValido(List.of(bloque), duracion, zona, t1));
        assertFalse(GeneradorTurnos.esTurnoValido(List.of(bloque), duracion, zona, t1.plus(Duration.ofMinutes(15))));
    }

    @Test
    @DisplayName("Dado un bloque dominical de 01:00 a 03:00 en America/New_York, cuando inicia el horario de verano, entonces solo genera turnos que caben antes de las 03:00 y descarta las horas inexistentes")
    void descartaTurnosEnSaltoHorarioDeVerano() {
        // Arrange
        ZoneId zona = ZoneId.of("America/New_York");
        LocalDate domingoCambioHorario = LocalDate.of(2026, 3, 8);
        Duration duracion = Duration.ofMinutes(30);
        HorarioDisponible bloque = new HorarioDisponible(
                1, 10, DayOfWeek.SUNDAY, LocalTime.of(1, 0), LocalTime.of(3, 0));

        // Act
        List<Turno> turnos = GeneradorTurnos.generar(
                List.of(bloque), duracion, zona, domingoCambioHorario, domingoCambioHorario);

        // Assert
        Instant turno0100 = ZonedDateTime.of(domingoCambioHorario, LocalTime.of(1, 0), zona).toInstant();
        Instant turno0130 = ZonedDateTime.of(domingoCambioHorario, LocalTime.of(1, 30), zona).toInstant();
        Instant turno0300 = ZonedDateTime.of(domingoCambioHorario, LocalTime.of(3, 0), zona).toInstant();

        assertEquals(List.of(
                new Turno(turno0100, turno0100.plus(duracion)),
                new Turno(turno0130, turno0130.plus(duracion))
        ), turnos);
        assertTrue(GeneradorTurnos.esTurnoValido(List.of(bloque), duracion, zona, turno0130));
        assertFalse(GeneradorTurnos.esTurnoValido(List.of(bloque), duracion, zona, turno0300));
    }
}
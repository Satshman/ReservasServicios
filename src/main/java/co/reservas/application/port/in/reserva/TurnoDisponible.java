package co.reservas.application.port.in.reserva;

import java.time.OffsetDateTime;

public record TurnoDisponible(OffsetDateTime fechaHoraInicio, OffsetDateTime fechaHoraFin, int cuposDisponibles) {
}

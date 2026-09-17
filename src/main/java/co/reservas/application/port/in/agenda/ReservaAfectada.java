package co.reservas.application.port.in.agenda;

import java.time.OffsetDateTime;

public record ReservaAfectada(Integer idReserva, OffsetDateTime fechaHoraInicio, OffsetDateTime fechaHoraFin) {
}

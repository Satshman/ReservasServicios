package co.reservas.application.port.in.reserva;

import java.time.OffsetDateTime;

public record CrearReservaComando(Integer idServicio, OffsetDateTime fechaHoraInicio) {
}

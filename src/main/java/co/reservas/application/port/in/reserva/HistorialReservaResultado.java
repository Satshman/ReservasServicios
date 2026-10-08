package co.reservas.application.port.in.reserva;

import co.reservas.domain.reserva.EstadoReserva;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reserva con su estado actual y sus cambios de estado en orden cronológico (el primero es la creación).
 */
public record HistorialReservaResultado(
        Integer idReserva,
        Integer idServicio,
        String nombreServicio,
        Integer idCliente,
        EstadoReserva estado,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        Instant creadoEn,
        List<CambioEstadoReservaResultado> cambios) {
}

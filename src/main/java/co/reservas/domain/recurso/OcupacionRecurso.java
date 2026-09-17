package co.reservas.domain.recurso;

import java.time.Instant;

/**
 * Uso de un recurso por una reserva confirmada.
 */
public record OcupacionRecurso(Integer idRecurso, Integer idReserva, Integer idServicio, Instant inicio,
                               Instant fin) {
}

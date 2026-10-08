package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.domain.reserva.EstadoReserva;

import java.time.Instant;

/**
 * Transición de estado de una reserva. {@code estadoAnterior} es nulo en la creación.
 */
public record CambioEstadoReservaResultado(EstadoReserva estadoAnterior, EstadoReserva estadoNuevo,
                                           AutorCambio realizadoPor, Instant fechaCambio) {
}

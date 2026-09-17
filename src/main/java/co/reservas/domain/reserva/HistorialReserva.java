package co.reservas.domain.reserva;

import java.time.Instant;

/**
 * Transición de estado de una reserva. {@code estadoAnterior} es nulo en la creación.
 */
public record HistorialReserva(Integer id, Integer idReserva, EstadoReserva estadoAnterior,
                               EstadoReserva estadoNuevo, Integer idUsuario, Instant fechaCambio) {

    public static HistorialReserva creacion(Reserva reserva, Integer idUsuario, Instant ahora) {
        return new HistorialReserva(null, reserva.id(), null, reserva.estado(), idUsuario, ahora);
    }
}

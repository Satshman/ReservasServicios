package co.reservas.domain.reserva;

import co.reservas.domain.servicio.Servicio;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Reserva de un turno de un servicio por parte de un cliente.
 */
public record Reserva(
        Integer id,
        Integer idCliente,
        Integer idServicio,
        EstadoReserva estado,
        Instant fechaHoraInicio,
        Instant fechaHoraFin,
        Instant creadoEn,
        Set<Integer> idsRecursos) {

    public Reserva {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fechaHoraInicio, "fechaHoraInicio");
        Objects.requireNonNull(fechaHoraFin, "fechaHoraFin");
        if (!fechaHoraFin.isAfter(fechaHoraInicio)) {
            throw new IllegalArgumentException("fechaHoraFin debe ser posterior a fechaHoraInicio");
        }
        idsRecursos = idsRecursos == null ? Set.of() : Set.copyOf(idsRecursos);
    }

    public static Reserva crear(Integer idCliente, Servicio servicio, Instant inicio, Set<Integer> idsRecursos,
                                Instant ahora) {
        return new Reserva(null, idCliente, servicio.id(), EstadoReserva.CONFIRMADA, inicio,
                inicio.plus(servicio.duracion()), ahora, idsRecursos);
    }

    public boolean seSolapaCon(Instant inicio, Instant fin) {
        return fechaHoraInicio.isBefore(fin) && inicio.isBefore(fechaHoraFin);
    }

    public Reserva conId(Integer nuevoId) {
        return new Reserva(nuevoId, idCliente, idServicio, estado, fechaHoraInicio, fechaHoraFin, creadoEn,
                idsRecursos);
    }
}

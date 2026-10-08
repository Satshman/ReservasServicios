package co.reservas.domain.reserva;

import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.Duration;
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

    /**
     * Cancela la reserva si sigue CONFIRMADA, no ha comenzado y faltan al menos {@code anticipacionMinima} para su
     * inicio. Con exactamente esa anticipación todavía se permite.
     */
    public Reserva cancelar(Instant ahora, Duration anticipacionMinima) {
        if (estado == EstadoReserva.CANCELADA) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_NO_CANCELABLE, "La reserva ya está cancelada.");
        }
        if (estado == EstadoReserva.COMPLETADA) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_NO_CANCELABLE,
                    "No se puede cancelar una reserva completada.");
        }
        if (!fechaHoraInicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_EN_EL_PASADO,
                    "No se puede cancelar una reserva que ya comenzó.");
        }
        if (fechaHoraInicio.isBefore(ahora.plus(anticipacionMinima))) {
            throw new ExcepcionNegocio(CodigoError.CANCELACION_FUERA_DE_PLAZO,
                    "Solo se puede cancelar con al menos " + describir(anticipacionMinima) + " de anticipación.");
        }
        return new Reserva(id, idCliente, idServicio, EstadoReserva.CANCELADA, fechaHoraInicio, fechaHoraFin,
                creadoEn, idsRecursos);
    }

    private static String describir(Duration duracion) {
        if (duracion.equals(Duration.ofDays(duracion.toDays()))) {
            return duracion.toDays() == 1 ? "1 día" : duracion.toDays() + " días";
        }
        if (duracion.equals(Duration.ofHours(duracion.toHours()))) {
            return duracion.toHours() == 1 ? "1 hora" : duracion.toHours() + " horas";
        }
        return duracion.toMinutes() + " minutos";
    }
}

package co.reservas.domain.agenda;

import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.Reserva;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Determina qué reservas confirmadas y futuras quedan afectadas al editar o eliminar un bloque de agenda.
 */
public final class DetectorReservasAfectadas {

    private DetectorReservasAfectadas() {
    }

    public static List<Reserva> alEditar(HorarioDisponible original, HorarioDisponible nuevo,
                                         Collection<Reserva> reservas, ZoneId zona, Instant ahora) {
        return filtrar(original, reservas, zona, ahora,
                reserva -> !nuevo.contieneIntervalo(local(reserva.fechaHoraInicio(), zona),
                        local(reserva.fechaHoraFin(), zona)));
    }

    public static List<Reserva> alEliminar(HorarioDisponible original, Collection<Reserva> reservas, ZoneId zona,
                                           Instant ahora) {
        return filtrar(original, reservas, zona, ahora, reserva -> true);
    }

    private static List<Reserva> filtrar(HorarioDisponible original, Collection<Reserva> reservas, ZoneId zona,
                                         Instant ahora, Predicate<Reserva> yaNoCabe) {
        return reservas.stream()
                .filter(reserva -> reserva.estado() == EstadoReserva.CONFIRMADA)
                .filter(reserva -> Objects.equals(reserva.idServicio(), original.idServicio()))
                .filter(reserva -> reserva.fechaHoraInicio().isAfter(ahora))
                .filter(reserva -> original.contieneInicio(local(reserva.fechaHoraInicio(), zona)))
                .filter(yaNoCabe)
                .sorted(Comparator.comparing(Reserva::fechaHoraInicio))
                .toList();
    }

    private static LocalDateTime local(Instant instante, ZoneId zona) {
        return LocalDateTime.ofInstant(instante, zona);
    }
}

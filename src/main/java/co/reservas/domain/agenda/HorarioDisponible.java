package co.reservas.domain.agenda;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bloque semanal de atención de un servicio: un día ISO (1 = lunes) y un rango de horas locales.
 */
public record HorarioDisponible(Integer id, Integer idServicio, DayOfWeek diaSemana, LocalTime horaInicio,
                                LocalTime horaFin) {

    public HorarioDisponible {
        Objects.requireNonNull(diaSemana, "diaSemana");
        Objects.requireNonNull(horaInicio, "horaInicio");
        Objects.requireNonNull(horaFin, "horaFin");
        if (!horaInicio.isBefore(horaFin)) {
            throw ExcepcionNegocio.validacion("horaInicio", "Debe ser anterior a horaFin");
        }
    }

    public static HorarioDisponible nuevo(Integer id, Integer idServicio, Integer diaSemana, LocalTime horaInicio,
                                          LocalTime horaFin) {
        if (diaSemana == null || diaSemana < 1 || diaSemana > 7) {
            throw ExcepcionNegocio.validacion("diaSemana", "Debe estar entre 1 (lunes) y 7 (domingo)");
        }
        return new HorarioDisponible(id, idServicio, DayOfWeek.of(diaSemana), horaInicio, horaFin);
    }

    public Duration duracion() {
        return Duration.between(horaInicio, horaFin);
    }

    public void validarCubreDuracion(Duration duracionServicio) {
        if (duracion().compareTo(duracionServicio) < 0) {
            throw new ExcepcionNegocio(CodigoError.BLOQUE_MENOR_A_DURACION,
                    "El bloque debe durar al menos la duración del servicio.",
                    List.of(Map.of("duracionMinutosServicio", duracionServicio.toMinutes(),
                            "duracionMinutosBloque", duracion().toMinutes())));
        }
    }

    public boolean seSolapaCon(HorarioDisponible otro) {
        boolean mismoBloque = id != null && id.equals(otro.id);
        return !mismoBloque
                && diaSemana == otro.diaSemana
                && horaInicio.isBefore(otro.horaFin)
                && otro.horaInicio.isBefore(horaFin);
    }

    /**
     * Lanza {@code HORARIO_SOLAPADO} si algún bloque nuevo se cruza con uno existente del mismo día.
     */
    public static void validarSinSolapes(Collection<HorarioDisponible> nuevos,
                                         Collection<HorarioDisponible> existentes) {
        for (HorarioDisponible nuevo : nuevos) {
            for (HorarioDisponible existente : existentes) {
                if (nuevo.seSolapaCon(existente)) {
                    throw new ExcepcionNegocio(CodigoError.HORARIO_SOLAPADO,
                            "El bloque se solapa con otro bloque del mismo servicio y día.",
                            List.of(Map.of("idHorario", existente.id(),
                                    "diaSemana", existente.diaSemana().getValue(),
                                    "horaInicio", existente.horaInicio().toString(),
                                    "horaFin", existente.horaFin().toString())));
                }
            }
        }
    }

    /**
     * Indica si un instante local cae dentro del bloque: mismo día de la semana y hora en [inicio, fin).
     */
    public boolean contieneInicio(LocalDateTime inicioLocal) {
        LocalTime hora = inicioLocal.toLocalTime();
        return inicioLocal.getDayOfWeek() == diaSemana && !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
    }

    /**
     * Indica si el intervalo local completo cabe dentro del bloque, sin cruzar la medianoche.
     */
    public boolean contieneIntervalo(LocalDateTime inicioLocal, LocalDateTime finLocal) {
        return inicioLocal.getDayOfWeek() == diaSemana
                && inicioLocal.toLocalDate().equals(finLocal.toLocalDate())
                && !inicioLocal.toLocalTime().isBefore(horaInicio)
                && !finLocal.toLocalTime().isAfter(horaFin);
    }

    /**
     * Indica si el inicio coincide con un turno del bloque: alineado a {@code horaInicio + k × duracion}.
     */
    public boolean admiteTurno(LocalDateTime inicioLocal, Duration duracionTurno) {
        if (!contieneIntervalo(inicioLocal, inicioLocal.plus(duracionTurno))) {
            return false;
        }
        Duration desdeInicio = Duration.between(inicioLocal.toLocalDate().atTime(horaInicio), inicioLocal);
        return desdeInicio.toNanos() % duracionTurno.toNanos() == 0;
    }
}

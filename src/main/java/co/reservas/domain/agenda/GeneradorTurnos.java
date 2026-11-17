package co.reservas.domain.agenda;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * Genera los turnos de un servicio a partir de sus bloques semanales, en la zona horaria del proveedor.
 */
public final class GeneradorTurnos {

    private GeneradorTurnos() {
    }

    public static List<Turno> generar(Collection<HorarioDisponible> bloques, Duration duracion, ZoneId zona,
                                      LocalDate desde, LocalDate hasta) {
        TreeMap<Instant, Turno> turnos = new TreeMap<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            for (HorarioDisponible bloque : bloques) {
                if (bloque.diaSemana() == fecha.getDayOfWeek()) {
                    agregarTurnosDelBloque(turnos, bloque, fecha, duracion, zona);
                }
            }
        }
        return new ArrayList<>(turnos.values());
    }

    public static boolean esTurnoValido(Collection<HorarioDisponible> bloques, Duration duracion, ZoneId zona,
                                        Instant inicio) {
        LocalDateTime inicioLocal = LocalDateTime.ofInstant(inicio, zona);
        return bloques.stream().anyMatch(bloque -> bloque.admiteTurno(inicioLocal, duracion));
    }

    private static void agregarTurnosDelBloque(TreeMap<Instant, Turno> turnos, HorarioDisponible bloque,
                                               LocalDate fecha, Duration duracion, ZoneId zona) {
        LocalDateTime limite = fecha.atTime(bloque.horaFin());
        LocalDateTime inicio = fecha.atTime(bloque.horaInicio());
        while (!inicio.plus(duracion).isAfter(limite)) {
            Instant inicioInstante = inicio.atZone(zona).toInstant();
            turnos.putIfAbsent(inicioInstante, new Turno(inicioInstante, inicioInstante.plus(duracion)));
            inicio = inicio.plus(duracion);
        }
    }
}

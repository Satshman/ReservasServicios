package co.reservas.domain.recurso;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Detecta recursos ocupados. Un recurso está ocupado si otra reserva confirmada lo usa en un intervalo que se
 * solapa, salvo que sea del mismo servicio y el mismo inicio (turno grupal compartido).
 */
public final class DetectorConflictosRecursos {

    private DetectorConflictosRecursos() {
    }

    public static SortedSet<Integer> recursosOcupados(Collection<Integer> idsRecursos, Integer idServicio,
                                                      Instant inicio, Instant fin,
                                                      Collection<OcupacionRecurso> ocupaciones) {
        SortedSet<Integer> ocupados = new TreeSet<>();
        for (OcupacionRecurso ocupacion : ocupaciones) {
            if (idsRecursos.contains(ocupacion.idRecurso())
                    && seSolapan(inicio, fin, ocupacion)
                    && !esTurnoCompartido(idServicio, inicio, ocupacion)) {
                ocupados.add(ocupacion.idRecurso());
            }
        }
        return ocupados;
    }

    private static boolean seSolapan(Instant inicio, Instant fin, OcupacionRecurso ocupacion) {
        return ocupacion.inicio().isBefore(fin) && inicio.isBefore(ocupacion.fin());
    }

    private static boolean esTurnoCompartido(Integer idServicio, Instant inicio, OcupacionRecurso ocupacion) {
        return Objects.equals(idServicio, ocupacion.idServicio()) && inicio.equals(ocupacion.inicio());
    }
}

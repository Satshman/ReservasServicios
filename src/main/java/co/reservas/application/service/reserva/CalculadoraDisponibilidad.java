package co.reservas.application.service.reserva;

import co.reservas.application.port.in.reserva.SugerenciaTurno;
import co.reservas.application.port.in.reserva.TurnoDisponible;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.domain.agenda.GeneradorTurnos;
import co.reservas.domain.agenda.Turno;
import co.reservas.domain.recurso.DetectorConflictosRecursos;
import co.reservas.domain.recurso.OcupacionRecurso;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calcula turnos futuros con cupo y sin recursos ocupados, y sugerencias de turnos alternativos.
 */
@Component
public class CalculadoraDisponibilidad {

    static final int MAXIMO_SUGERENCIAS = 3;
    static final int DIAS_SUGERENCIAS = 14;

    private final ReservaRepositoryPort reservas;

    public CalculadoraDisponibilidad(ReservaRepositoryPort reservas) {
        this.reservas = reservas;
    }

    public List<TurnoDisponible> turnosDisponibles(ContextoAgenda contexto, LocalDate desde, LocalDate hasta,
                                                   Instant ahora) {
        List<Turno> candidatos = GeneradorTurnos.generar(contexto.bloques(), contexto.servicio().duracion(),
                        contexto.zona(), desde, hasta).stream()
                .filter(turno -> turno.inicio().isAfter(ahora))
                .toList();
        return filtrarDisponibles(contexto, candidatos, Integer.MAX_VALUE);
    }

    /**
     * Sugiere hasta tres turnos disponibles a partir de {@code referencia} dentro de los próximos catorce días.
     */
    public List<SugerenciaTurno> sugerencias(ContextoAgenda contexto, Instant referencia, Instant ahora) {
        Instant desdeInstante = referencia.isAfter(ahora) ? referencia : ahora;
        LocalDate desde = LocalDate.ofInstant(desdeInstante, contexto.zona());
        List<Turno> candidatos = GeneradorTurnos.generar(contexto.bloques(), contexto.servicio().duracion(),
                        contexto.zona(), desde, desde.plusDays(DIAS_SUGERENCIAS)).stream()
                .filter(turno -> turno.inicio().isAfter(ahora) && !turno.inicio().isBefore(desdeInstante))
                .toList();
        return filtrarDisponibles(contexto, candidatos, MAXIMO_SUGERENCIAS).stream()
                .map(turno -> new SugerenciaTurno(turno.fechaHoraInicio(), turno.fechaHoraFin()))
                .toList();
    }

    private List<TurnoDisponible> filtrarDisponibles(ContextoAgenda contexto, List<Turno> candidatos, int limite) {
        if (candidatos.isEmpty()) {
            return List.of();
        }
        Instant desde = candidatos.getFirst().inicio();
        Instant hasta = candidatos.getLast().fin();
        Integer idServicio = contexto.servicio().id();
        Map<Instant, Long> ocupadosPorTurno = reservas.contarConfirmadasPorTurno(idServicio, desde, hasta);
        List<Integer> idsRecursos = contexto.idsRecursos();
        List<OcupacionRecurso> ocupaciones = idsRecursos.isEmpty()
                ? List.of()
                : reservas.listarOcupaciones(idsRecursos, desde, hasta);

        List<TurnoDisponible> disponibles = new ArrayList<>();
        for (Turno turno : candidatos) {
            long cupos = contexto.servicio().capacidad() - ocupadosPorTurno.getOrDefault(turno.inicio(), 0L);
            boolean recursosLibres = DetectorConflictosRecursos.recursosOcupados(idsRecursos, idServicio,
                    turno.inicio(), turno.fin(), ocupaciones).isEmpty();
            if (cupos > 0 && recursosLibres) {
                disponibles.add(new TurnoDisponible(turno.inicio().atZone(contexto.zona()).toOffsetDateTime(),
                        turno.fin().atZone(contexto.zona()).toOffsetDateTime(), (int) cupos));
                if (disponibles.size() >= limite) {
                    break;
                }
            }
        }
        return disponibles;
    }
}

package co.reservas.application.service.reserva;

import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.Servicio;

import java.time.ZoneId;
import java.util.List;

/**
 * Datos necesarios para calcular turnos disponibles de un servicio.
 */
public record ContextoAgenda(Servicio servicio, ZoneId zona, List<HorarioDisponible> bloques,
                             List<Recurso> recursos) {

    public List<Integer> idsRecursos() {
        return recursos.stream().map(Recurso::id).toList();
    }
}

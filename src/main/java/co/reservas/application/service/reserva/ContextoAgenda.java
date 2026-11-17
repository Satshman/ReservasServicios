package co.reservas.application.service.reserva;

import co.reservas.domain.agenda.ExcepcionDisponibilidad;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.Servicio;

import java.time.ZoneId;
import java.util.List;

/**
 * Datos necesarios para calcular turnos disponibles de un servicio.
 */
public record ContextoAgenda(Servicio servicio, ZoneId zona, List<HorarioDisponible> bloques,
                             List<Recurso> recursos, List<ExcepcionDisponibilidad> excepciones) {

    public ContextoAgenda(Servicio servicio, ZoneId zona, List<HorarioDisponible> bloques, List<Recurso> recursos) {
        this(servicio, zona, bloques, recursos, List.of());
    }

    public List<Integer> idsRecursos() {
        return recursos.stream().map(Recurso::id).toList();
    }
}
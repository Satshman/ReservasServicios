package co.reservas.application.port.in.agenda;

import java.time.LocalTime;
import java.util.Set;

public record CrearHorarioComando(Set<Integer> diasSemana, LocalTime horaInicio, LocalTime horaFin) {

    public CrearHorarioComando {
        diasSemana = diasSemana == null ? Set.of() : Set.copyOf(diasSemana);
    }
}

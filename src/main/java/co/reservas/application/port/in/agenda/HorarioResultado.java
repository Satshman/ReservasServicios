package co.reservas.application.port.in.agenda;

import java.time.LocalTime;

public record HorarioResultado(Integer id, Integer idServicio, int diaSemana, LocalTime horaInicio,
                               LocalTime horaFin) {
}

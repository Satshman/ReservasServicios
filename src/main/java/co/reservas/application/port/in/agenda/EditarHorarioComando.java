package co.reservas.application.port.in.agenda;

import java.time.LocalTime;

public record EditarHorarioComando(Integer diaSemana, LocalTime horaInicio, LocalTime horaFin) {
}

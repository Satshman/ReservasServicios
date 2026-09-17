package co.reservas.application.port.in.reserva;

import java.util.List;

public record DisponibilidadResultado(Integer idServicio, String zonaHoraria, List<TurnoDisponible> turnos) {
}

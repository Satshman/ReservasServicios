package co.reservas.application.port.in.agenda;

import java.util.List;

/**
 * Resultado de editar o eliminar un bloque. {@code horario} es nulo cuando el bloque se eliminó.
 */
public record CambioHorarioResultado(HorarioResultado horario, List<ReservaAfectada> reservasAfectadas) {
}

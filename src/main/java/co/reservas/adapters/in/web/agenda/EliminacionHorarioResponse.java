package co.reservas.adapters.in.web.agenda;

import co.reservas.application.port.in.agenda.ReservaAfectada;

import java.util.List;

public record EliminacionHorarioResponse(List<ReservaAfectada> reservasAfectadas) {
}

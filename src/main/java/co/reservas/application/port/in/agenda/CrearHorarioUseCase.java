package co.reservas.application.port.in.agenda;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;

public interface CrearHorarioUseCase {

    List<HorarioResultado> crear(UsuarioAutenticado usuario, Integer idServicio, CrearHorarioComando comando);
}

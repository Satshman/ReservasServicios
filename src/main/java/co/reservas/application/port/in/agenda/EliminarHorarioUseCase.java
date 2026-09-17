package co.reservas.application.port.in.agenda;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

public interface EliminarHorarioUseCase {

    CambioHorarioResultado eliminar(UsuarioAutenticado usuario, Integer idHorario, boolean confirmar);
}

package co.reservas.application.port.in.agenda;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

public interface EditarHorarioUseCase {

    CambioHorarioResultado editar(UsuarioAutenticado usuario, Integer idHorario, EditarHorarioComando comando,
                                  boolean confirmar);
}

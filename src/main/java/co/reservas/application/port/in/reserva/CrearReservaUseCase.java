package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

public interface CrearReservaUseCase {

    ReservaResultado crear(UsuarioAutenticado usuario, CrearReservaComando comando);
}

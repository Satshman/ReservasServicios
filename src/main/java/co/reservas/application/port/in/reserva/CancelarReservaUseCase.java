package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

public interface CancelarReservaUseCase {
    ReservaResultado cancelar(UsuarioAutenticado usuario, Integer idReserva);
}

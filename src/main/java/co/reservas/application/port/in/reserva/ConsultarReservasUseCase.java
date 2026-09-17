package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;

public interface ConsultarReservasUseCase {

    List<ReservaResultado> consultarMias(UsuarioAutenticado usuario);

    List<ReservaResultado> consultarPorServicio(UsuarioAutenticado usuario, Integer idServicio);
}

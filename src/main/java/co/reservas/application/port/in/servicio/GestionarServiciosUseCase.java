package co.reservas.application.port.in.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;

public interface GestionarServiciosUseCase {

    ServicioResultado crear(UsuarioAutenticado usuario, NuevoServicioComando comando);

    List<ServicioResultado> listar(Integer idProveedor, Integer idCategoria);

    ServicioResultado consultar(Integer idServicio);
}

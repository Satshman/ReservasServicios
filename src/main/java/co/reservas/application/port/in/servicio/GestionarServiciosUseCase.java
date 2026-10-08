package co.reservas.application.port.in.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.domain.servicio.EstadoServicio;

import java.util.List;

public interface GestionarServiciosUseCase {

    ServicioResultado crear(UsuarioAutenticado usuario, NuevoServicioComando comando);

    List<ServicioResultado> listar(Integer idProveedor, Integer idCategoria);

    ServicioResultado consultar(Integer idServicio);

    /**
     * Activa o inactiva un servicio propio. Un servicio inactivo no acepta reservas nuevas; las confirmadas se
     * mantienen. Si el estado no cambia no se registra nada en el historial.
     */
    ServicioResultado cambiarEstado(UsuarioAutenticado usuario, Integer idServicio, EstadoServicio estado);
}

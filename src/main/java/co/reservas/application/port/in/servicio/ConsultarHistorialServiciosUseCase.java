package co.reservas.application.port.in.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;

/**
 * HU-11: historial de cambios de los servicios del proveedor autenticado.
 */
public interface ConsultarHistorialServiciosUseCase {

    /**
     * Cambios en orden cronológico. Con {@code idServicio} nulo incluye todos los servicios del proveedor.
     */
    List<CambioServicioResultado> consultar(UsuarioAutenticado usuario, Integer idServicio);
}

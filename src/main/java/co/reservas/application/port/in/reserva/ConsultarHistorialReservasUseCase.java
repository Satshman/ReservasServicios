package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;

/**
 * HU-10 (cliente: sus reservas) y HU-14 (proveedor: las reservas de sus servicios).
 */
public interface ConsultarHistorialReservasUseCase {

    /**
     * Reservas del usuario ordenadas de la más reciente a la más antigua, cada una con sus cambios de estado.
     */
    List<HistorialReservaResultado> consultar(UsuarioAutenticado usuario, FiltroHistorialReservas filtro);

    /**
     * Historial de una reserva; solo para el cliente que la hizo o el proveedor dueño del servicio.
     */
    HistorialReservaResultado consultarReserva(UsuarioAutenticado usuario, Integer idReserva);
}

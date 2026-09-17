package co.reservas.application.service.servicio;

import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.usuario.Proveedor;

/**
 * Servicio cuya propiedad ya se verificó, junto con su proveedor.
 */
public record ServicioPropio(Servicio servicio, Proveedor proveedor) {
}

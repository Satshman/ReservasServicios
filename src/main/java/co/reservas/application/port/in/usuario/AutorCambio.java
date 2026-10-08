package co.reservas.application.port.in.usuario;

import co.reservas.domain.usuario.Rol;

/**
 * Usuario que hizo un cambio registrado en un historial. No incluye datos de contacto.
 */
public record AutorCambio(Integer idUsuario, String nombre, Rol rol) {
}

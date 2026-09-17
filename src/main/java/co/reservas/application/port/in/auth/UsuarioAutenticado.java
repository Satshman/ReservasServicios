package co.reservas.application.port.in.auth;

import co.reservas.domain.usuario.Rol;

/**
 * Identidad del usuario que invoca un caso de uso, extraída del token de acceso.
 */
public record UsuarioAutenticado(Integer idUsuario, Rol rol) {
}

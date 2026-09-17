package co.reservas.domain.usuario;

/**
 * Perfil de cliente asociado uno a uno a un usuario.
 */
public record Cliente(Integer id, Integer idUsuario) {
}

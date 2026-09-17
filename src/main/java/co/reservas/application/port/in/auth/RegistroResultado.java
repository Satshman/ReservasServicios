package co.reservas.application.port.in.auth;

import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;

public record RegistroResultado(Integer id, String email, Rol rol, EstadoUsuario estado, String mensaje) {
}

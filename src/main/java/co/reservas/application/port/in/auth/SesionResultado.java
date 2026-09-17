package co.reservas.application.port.in.auth;

import co.reservas.domain.usuario.Rol;

public record SesionResultado(String accessToken, String tokenType, long expiresIn, Rol rol, String redirectTo) {
}

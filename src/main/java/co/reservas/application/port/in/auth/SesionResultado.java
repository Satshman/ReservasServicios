package co.reservas.application.port.in.auth;

import co.reservas.domain.usuario.Rol;

public record SesionResultado(
        String accessToken,
        String tokenType,
        long expiresIn,
        Rol rol,
        String redirectTo,
        String refreshToken
) {
    public SesionResultado(String accessToken, String tokenType, long expiresIn, Rol rol, String redirectTo) {
        this(accessToken, tokenType, expiresIn, rol, redirectTo, null);
    }
}
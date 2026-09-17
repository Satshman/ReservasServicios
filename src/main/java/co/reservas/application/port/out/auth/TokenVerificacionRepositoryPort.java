package co.reservas.application.port.out.auth;

import co.reservas.domain.usuario.TokenVerificacion;

import java.time.Instant;
import java.util.Optional;

public interface TokenVerificacionRepositoryPort {

    TokenVerificacion guardar(TokenVerificacion token);

    Optional<TokenVerificacion> buscarPorHash(String tokenHash);

    /**
     * Invalida los tokens sin usar del usuario haciendo que expiren en {@code ahora}.
     */
    void invalidarPendientes(Integer idUsuario, Instant ahora);
}

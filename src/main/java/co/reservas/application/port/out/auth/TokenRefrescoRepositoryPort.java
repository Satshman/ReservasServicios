package co.reservas.application.port.out.auth;

import co.reservas.domain.usuario.TokenRefresco;

import java.time.Instant;
import java.util.Optional;

public interface TokenRefrescoRepositoryPort {

    TokenRefresco guardar(TokenRefresco token);

    Optional<TokenRefresco> buscarPorHash(String tokenHash);

    void revocarActivosDeUsuario(Integer idUsuario, Instant ahora);
}
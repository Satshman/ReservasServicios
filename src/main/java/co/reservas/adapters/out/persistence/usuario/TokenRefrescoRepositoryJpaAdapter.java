package co.reservas.adapters.out.persistence.usuario;

import co.reservas.application.port.out.auth.TokenRefrescoRepositoryPort;
import co.reservas.domain.usuario.TokenRefresco;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class TokenRefrescoRepositoryJpaAdapter implements TokenRefrescoRepositoryPort {

    private final TokenRefrescoJpaRepository jpa;

    public TokenRefrescoRepositoryJpaAdapter(TokenRefrescoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TokenRefresco guardar(TokenRefresco token) {
        return jpa.save(TokenRefrescoEntity.deDominio(token)).aDominio();
    }

    @Override
    public Optional<TokenRefresco> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(TokenRefrescoEntity::aDominio);
    }

    @Override
    public void revocarActivosDeUsuario(Integer idUsuario, Instant ahora) {
        jpa.revocarActivosPorUsuario(idUsuario, ahora);
    }
}
package co.reservas.adapters.out.persistence.usuario;

import co.reservas.application.port.out.auth.TokenVerificacionRepositoryPort;
import co.reservas.domain.usuario.TokenVerificacion;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class TokenVerificacionRepositoryJpaAdapter implements TokenVerificacionRepositoryPort {

    private final TokenVerificacionJpaRepository tokens;

    public TokenVerificacionRepositoryJpaAdapter(TokenVerificacionJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public TokenVerificacion guardar(TokenVerificacion token) {
        TokenVerificacionEntity guardado = tokens.save(new TokenVerificacionEntity(token.id(), token.idUsuario(),
                token.tokenHash(), token.expiraEn(), token.usadoEn(), token.creadoEn()));
        return aDominio(guardado);
    }

    @Override
    public Optional<TokenVerificacion> buscarPorHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(TokenVerificacionRepositoryJpaAdapter::aDominio);
    }

    @Override
    public void invalidarPendientes(Integer idUsuario, Instant ahora) {
        tokens.invalidarPendientes(idUsuario, ahora);
    }

    private static TokenVerificacion aDominio(TokenVerificacionEntity entidad) {
        return new TokenVerificacion(entidad.getId(), entidad.getIdUsuario(), entidad.getTokenHash(),
                entidad.getExpiraEn(), entidad.getUsadoEn(), entidad.getCreadoEn());
    }
}

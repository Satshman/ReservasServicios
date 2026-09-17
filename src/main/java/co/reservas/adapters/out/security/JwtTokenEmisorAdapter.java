package co.reservas.adapters.out.security;

import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.domain.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Emite tokens de acceso JWT firmados con HS256.
 */
@Component
public class JwtTokenEmisorAdapter implements TokenEmisorPort {

    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration expiracion;
    private final String emisor;

    public JwtTokenEmisorAdapter(JwtEncoder encoder, Clock clock,
                                 @Value("${app.jwt.expiration}") Duration expiracion,
                                 @Value("${app.jwt.issuer}") String emisor) {
        this.encoder = encoder;
        this.clock = clock;
        this.expiracion = expiracion;
        this.emisor = emisor;
    }

    @Override
    public TokenAcceso emitir(Usuario usuario) {
        Instant ahora = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(String.valueOf(usuario.id()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiracion))
                .claim(CLAIM_ROL, usuario.rol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenAcceso(valor, expiracion.toSeconds());
    }
}

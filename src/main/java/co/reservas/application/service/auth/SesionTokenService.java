package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.GestionarSesionTokenUseCase;
import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.auth.TokenRefrescoRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.TokenRefresco;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SesionTokenService implements GestionarSesionTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(SesionTokenService.class);
    private static final String TIPO_TOKEN = "Bearer";
    private static final Duration VIGENCIA_REFRESCO = Duration.ofDays(7);
    private static final int BYTES_TOKEN = 32;

    private final TokenRefrescoRepositoryPort tokensRefresco;
    private final UsuarioRepositoryPort usuarios;
    private final TokenEmisorPort tokenEmisor;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public SesionTokenService(TokenRefrescoRepositoryPort tokensRefresco,
                              UsuarioRepositoryPort usuarios,
                              TokenEmisorPort tokenEmisor,
                              Clock clock) {
        this.tokensRefresco = tokensRefresco;
        this.usuarios = usuarios;
        this.tokenEmisor = tokenEmisor;
        this.clock = clock;
    }

    public String emitirTokenRefresco(Integer idUsuario, Instant ahora) {
        byte[] bytes = new byte[BYTES_TOKEN];
        secureRandom.nextBytes(bytes);
        String tokenEnClaro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokensRefresco.guardar(TokenRefresco.nuevo(idUsuario, tokenEnClaro, ahora.plus(VIGENCIA_REFRESCO), ahora));
        return tokenEnClaro;
    }

    @Override
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public SesionResultado rotar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw tokenRefrescoInvalido();
        }
        Instant ahora = Instant.now(clock);
        String hash = TokenVerificacion.calcularHash(refreshToken);
        TokenRefresco existente = tokensRefresco.buscarPorHash(hash)
                .orElseThrow(SesionTokenService::tokenRefrescoInvalido);

        if (existente.revocadoEn() != null) {
            tokensRefresco.revocarActivosDeUsuario(existente.idUsuario(), ahora);
            log.atWarn()
                    .addKeyValue("evento", "REUTILIZACION_TOKEN_REFRESCO")
                    .addKeyValue("idUsuario", existente.idUsuario())
                    .log("Intento de reutilización de token de refresco revocado");
            throw tokenRefrescoInvalido();
        }
        if (!existente.estaActivo(ahora)) {
            throw tokenRefrescoInvalido();
        }

        Usuario usuario = usuarios.buscarPorId(existente.idUsuario())
                .orElseThrow(SesionTokenService::tokenRefrescoInvalido);
        if (usuario.estado() != EstadoUsuario.ACTIVO) {
            throw tokenRefrescoInvalido();
        }

        tokensRefresco.guardar(existente.revocar(ahora));
        String nuevoRefresh = emitirTokenRefresco(usuario.id(), ahora);
        TokenEmisorPort.TokenAcceso nuevoAccess = tokenEmisor.emitir(usuario);

        log.atInfo()
                .addKeyValue("evento", "TOKEN_ROTADO")
                .addKeyValue("idUsuario", usuario.id())
                .log("Token de sesión rotado");

        return new SesionResultado(nuevoAccess.valor(), TIPO_TOKEN, nuevoAccess.expiraEnSegundos(),
                usuario.rol(), usuario.rol().rutaPanel(), nuevoRefresh);
    }

    @Override
    @Transactional
    public void revocar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        Instant ahora = Instant.now(clock);
        String hash = TokenVerificacion.calcularHash(refreshToken);
        tokensRefresco.buscarPorHash(hash)
                .filter(t -> t.revocadoEn() == null)
                .ifPresent(token -> {
                    tokensRefresco.guardar(token.revocar(ahora));
                    log.atInfo()
                            .addKeyValue("evento", "TOKEN_REVOCADO")
                            .addKeyValue("idUsuario", token.idUsuario())
                            .log("Token de refresco revocado");
                });
    }

    private static ExcepcionNegocio tokenRefrescoInvalido() {
        return new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS,
                "El token de refresco no es válido, expiró o fue revocado.");
    }
}
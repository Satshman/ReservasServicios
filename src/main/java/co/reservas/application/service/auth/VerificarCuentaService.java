package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.VerificarCuentaUseCase;
import co.reservas.application.port.out.auth.TokenVerificacionRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class VerificarCuentaService implements VerificarCuentaUseCase {

    private static final int LONGITUD_MAXIMA_TOKEN = 128;
    private static final Logger log = LoggerFactory.getLogger(VerificarCuentaService.class);

    private final TokenVerificacionRepositoryPort tokens;
    private final UsuarioRepositoryPort usuarios;
    private final Clock clock;

    public VerificarCuentaService(TokenVerificacionRepositoryPort tokens, UsuarioRepositoryPort usuarios,
                                  Clock clock) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void verificar(String token) {
        if (token == null || token.isBlank() || token.length() > LONGITUD_MAXIMA_TOKEN) {
            throw tokenInvalido();
        }
        Instant ahora = Instant.now(clock);
        TokenVerificacion verificacion = tokens.buscarPorHash(TokenVerificacion.calcularHash(token))
                .filter(t -> t.esValido(ahora))
                .orElseThrow(VerificarCuentaService::tokenInvalido);
        Usuario usuario = usuarios.buscarPorId(verificacion.idUsuario())
                .orElseThrow(VerificarCuentaService::tokenInvalido);
        if (usuario.estado() == EstadoUsuario.INACTIVO) {
            throw tokenInvalido();
        }
        if (usuario.estado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            usuarios.guardar(usuario.activar());
        }
        tokens.guardar(verificacion.marcarUsado(ahora));
        log.atInfo()
                .addKeyValue("evento", "CUENTA_VERIFICADA")
                .addKeyValue("idUsuario", usuario.id())
                .log("Cuenta verificada");
    }

    private static ExcepcionNegocio tokenInvalido() {
        return new ExcepcionNegocio(CodigoError.TOKEN_VERIFICACION_INVALIDO,
                "El enlace de verificación no es válido, ya fue usado o expiró.");
    }
}

package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.ReenviarVerificacionUseCase;
import co.reservas.application.port.out.auth.TokenVerificacionRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Reenvía el correo de verificación. No revela si el correo existe: el resultado es siempre el mismo.
 */
@Service
public class ReenviarVerificacionService implements ReenviarVerificacionUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final TokenVerificacionRepositoryPort tokens;
    private final SolicitadorVerificacion solicitadorVerificacion;
    private final Clock clock;

    public ReenviarVerificacionService(UsuarioRepositoryPort usuarios, TokenVerificacionRepositoryPort tokens,
                                       SolicitadorVerificacion solicitadorVerificacion, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.solicitadorVerificacion = solicitadorVerificacion;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void reenviar(String email) {
        usuarios.buscarPorEmail(Usuario.normalizarEmail(email))
                .filter(usuario -> usuario.estado() == EstadoUsuario.PENDIENTE_VERIFICACION)
                .ifPresent(usuario -> {
                    Instant ahora = Instant.now(clock);
                    tokens.invalidarPendientes(usuario.id(), ahora);
                    solicitadorVerificacion.solicitar(usuario, ahora);
                });
    }
}

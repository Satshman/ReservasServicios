package co.reservas.application.service.auth;

import co.reservas.application.port.out.auth.TokenVerificacionRepositoryPort;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Emite un token de verificación nuevo y solicita el envío del correo al confirmar la transacción.
 */
@Component
public class SolicitadorVerificacion {

    private final TokenVerificacionRepositoryPort tokens;
    private final ApplicationEventPublisher eventos;

    public SolicitadorVerificacion(TokenVerificacionRepositoryPort tokens, ApplicationEventPublisher eventos) {
        this.tokens = tokens;
        this.eventos = eventos;
    }

    public void solicitar(Usuario usuario, Instant ahora) {
        String tokenPlano = TokenVerificacion.generarValorPlano();
        tokens.guardar(TokenVerificacion.emitir(usuario.id(), TokenVerificacion.calcularHash(tokenPlano), ahora));
        eventos.publishEvent(new VerificacionCuentaSolicitada(usuario.id(), usuario.email(), usuario.nombreCompleto(),
                tokenPlano));
    }
}

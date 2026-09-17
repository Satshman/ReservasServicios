package co.reservas.application.service.auth;

import co.reservas.application.port.out.auth.NotificacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía el correo de verificación después del commit. Un fallo se registra y nunca revierte el registro.
 */
@Component
public class NotificadorVerificacionCuenta {

    private static final Logger log = LoggerFactory.getLogger(NotificadorVerificacionCuenta.class);

    private final NotificacionPort notificaciones;

    public NotificadorVerificacionCuenta(NotificacionPort notificaciones) {
        this.notificaciones = notificaciones;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alSolicitarVerificacion(VerificacionCuentaSolicitada evento) {
        try {
            notificaciones.enviarVerificacionCuenta(evento.email(), evento.nombre(), evento.tokenPlano());
            log.atInfo()
                    .addKeyValue("evento", "CORREO_VERIFICACION_ENVIADO")
                    .addKeyValue("idUsuario", evento.idUsuario())
                    .log("Correo de verificación enviado");
        } catch (RuntimeException e) {
            log.atWarn()
                    .addKeyValue("evento", "CORREO_VERIFICACION_FALLIDO")
                    .addKeyValue("idUsuario", evento.idUsuario())
                    .addKeyValue("causa", e.getClass().getSimpleName())
                    .log("No se pudo enviar el correo de verificación; el usuario puede solicitar un reenvío");
        }
    }
}

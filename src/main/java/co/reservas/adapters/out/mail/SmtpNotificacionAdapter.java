package co.reservas.adapters.out.mail;

import co.reservas.application.port.out.auth.NotificacionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SmtpNotificacionAdapter implements NotificacionPort {

    static final String RUTA_VERIFICACION = "/api/v1/auth/verificacion";

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String remitente;

    public SmtpNotificacionAdapter(JavaMailSender mailSender, @Value("${app.base-url}") String baseUrl,
                                   @Value("${app.mail.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.remitente = remitente;
    }

    @Override
    public void enviarVerificacionCuenta(String email, String nombre, String tokenPlano) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(email);
        mensaje.setSubject("Verifica tu cuenta - Plataforma de Reservas de Servicios");
        mensaje.setText("""
                Hola %s,

                Gracias por registrarte. Para activar tu cuenta abre el siguiente enlace (vigente por 24 horas):

                %s

                Si no creaste esta cuenta, ignora este mensaje.
                """.formatted(nombre, enlaceVerificacion(tokenPlano)));
        mailSender.send(mensaje);
    }

    String enlaceVerificacion(String tokenPlano) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(RUTA_VERIFICACION)
                .queryParam("token", tokenPlano)
                .build()
                .toUriString();
    }
}

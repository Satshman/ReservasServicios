package co.reservas.adapters.out.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Correo del código MFA de administrador")
class SmtpNotificacionMfaTest {

    @Test
    @DisplayName("Dado un código MFA, cuando se envía, entonces el correo va al administrador desde el remitente configurado con el código y su vigencia")
    void enviaElCodigoPorCorreo() {
        // Arrange
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpNotificacionAdapter adapter = new SmtpNotificacionAdapter(mailSender, "https://reservas.example.com",
                "no-reply@reservas.local");

        // Act
        adapter.enviarCodigoMfa("admin@prueba.co", "Ana", "482915");

        // Assert
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getTo()).containsExactly("admin@prueba.co");
        assertThat(mensaje.getFrom()).isEqualTo("no-reply@reservas.local");
        assertThat(mensaje.getSubject()).contains("administrador");
        assertThat(mensaje.getText()).contains("Hola Ana").contains("482915").contains("5 minutos");
    }
}

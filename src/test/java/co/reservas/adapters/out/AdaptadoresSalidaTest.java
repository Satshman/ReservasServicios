package co.reservas.adapters.out;

import co.reservas.adapters.out.mail.SmtpNotificacionAdapter;
import co.reservas.adapters.out.security.BCryptPasswordHasherAdapter;
import co.reservas.adapters.out.security.JwtTokenEmisorAdapter;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdaptadoresSalidaTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");

    @Test
    @DisplayName("Dado una contraseña válida, cuando se hashea con BCrypt, entonces coincide solo con la original y rechaza más de 72 bytes")
    void bcrypt() {
        // Arrange
        BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter(4);
        String largaMultibyte = "ñ".repeat(40);

        // Act
        String hash = hasher.hashear("clave-de-prueba-segura");

        // Assert
        assertThat(hasher.coincide("clave-de-prueba-segura", hash)).isTrue();
        assertThat(hasher.coincide("otra-clave", hash)).isFalse();
        assertThat(hasher.coincide(largaMultibyte, hash)).isFalse();
        assertThat(hasher.coincide("clave", null)).isFalse();
        assertThatThrownBy(() -> hasher.hashear(largaMultibyte)).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    @DisplayName("Dado un usuario proveedor, cuando se emite el JWT, entonces contiene sujeto, rol, emisor y vence en 30 minutos")
    void jwt() {
        // Arrange
        SecretKeySpec clave = new SecretKeySpec(
                "clave-de-pruebas-unitarias-de-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtTokenEmisorAdapter emisor = new JwtTokenEmisorAdapter(new NimbusJwtEncoder(new ImmutableSecret<>(clave)),
                Clock.fixed(AHORA, ZoneOffset.UTC), Duration.ofMinutes(30), "reservas-servicios");
        Usuario usuario = new Usuario(42, Rol.PROVEEDOR, EstadoUsuario.ACTIVO, null, null, "Ana", "a@b.co", null,
                "hash", 0, null, AHORA);

        // Act
        TokenEmisorPort.TokenAcceso token = emisor.emitir(usuario);

        // Assert
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
        Jwt jwt = decoder.decode(token.valor());
        assertThat(token.expiraEnSegundos()).isEqualTo(1800);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("PROVEEDOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("reservas-servicios");
        assertThat(jwt.getExpiresAt()).isEqualTo(AHORA.plus(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Dado un token de verificación, cuando se envía el correo, entonces el enlace apunta a APP_BASE_URL con el token")
    void correoVerificacion() {
        // Arrange
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpNotificacionAdapter adapter = new SmtpNotificacionAdapter(mailSender, "https://reservas.example.com/",
                "no-reply@reservas.example.com");

        // Act
        adapter.enviarVerificacionCuenta("ana@prueba.co", "Ana", "abc_DEF-123");

        // Assert
        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("ana@prueba.co");
        assertThat(mensaje.getValue().getText())
                .contains("https://reservas.example.com/api/v1/auth/verificacion?token=abc_DEF-123");
    }
}

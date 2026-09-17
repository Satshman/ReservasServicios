package co.reservas.adapters.out.security;

import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    /** BCrypt solo procesa los primeros 72 bytes de la contraseña. */
    static final int MAXIMO_BYTES = 72;

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasherAdapter(@Value("${app.seguridad.bcrypt-fuerza:10}") int fuerza) {
        this.encoder = new BCryptPasswordEncoder(fuerza);
    }

    @Override
    public String hashear(String passwordPlano) {
        if (excedeLimite(passwordPlano)) {
            throw ExcepcionNegocio.validacion("password", "Supera la longitud admitida en bytes");
        }
        return encoder.encode(passwordPlano);
    }

    @Override
    public boolean coincide(String passwordPlano, String hash) {
        if (passwordPlano == null || hash == null || excedeLimite(passwordPlano)) {
            return false;
        }
        return encoder.matches(passwordPlano, hash);
    }

    private static boolean excedeLimite(String passwordPlano) {
        return passwordPlano.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES;
    }
}

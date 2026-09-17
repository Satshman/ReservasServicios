package co.reservas.domain.usuario;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Bloqueo temporal de la cuenta tras varios intentos fallidos de inicio de sesión.
 */
public record PoliticaBloqueo(int maxIntentos, Duration duracionBloqueo) {

    public PoliticaBloqueo {
        Objects.requireNonNull(duracionBloqueo, "duracionBloqueo");
        if (maxIntentos < 1) {
            throw new IllegalArgumentException("maxIntentos debe ser mayor que cero");
        }
        if (duracionBloqueo.isNegative() || duracionBloqueo.isZero()) {
            throw new IllegalArgumentException("duracionBloqueo debe ser positiva");
        }
    }

    /**
     * Suma un intento fallido. Al alcanzar el máximo bloquea la cuenta y reinicia el contador.
     */
    public Usuario registrarIntentoFallido(Usuario usuario, Instant ahora) {
        int intentos = usuario.intentosFallidos() + 1;
        if (intentos >= maxIntentos) {
            return usuario.conIntentosFallidos(0, ahora.plus(duracionBloqueo));
        }
        return usuario.conIntentosFallidos(intentos, usuario.bloqueadoHasta());
    }
}

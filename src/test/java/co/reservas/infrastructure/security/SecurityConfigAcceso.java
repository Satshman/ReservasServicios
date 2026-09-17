package co.reservas.infrastructure.security;

import javax.crypto.SecretKey;

/**
 * Expone a las pruebas la construcción de la clave JWT, que es de visibilidad de paquete.
 */
public final class SecurityConfigAcceso {

    private SecurityConfigAcceso() {
    }

    public static SecretKey clave(String secreto) {
        return SecurityConfig.claveSecreta(secreto);
    }
}

package co.reservas.application.port.out.auth;

public interface NotificacionPort {

    /**
     * Envía el correo con el enlace de verificación construido a partir del token en claro.
     */
    void enviarVerificacionCuenta(String email, String nombre, String tokenPlano);
}

package co.reservas.application.port.in.auth;

/**
 * Crea la primera cuenta de administrador desde la configuración del despliegue (no desde la API pública).
 */
public interface AprovisionarAdministradorUseCase {

    /**
     * @return {@code true} si creó la cuenta; {@code false} si ya existía un usuario con ese correo.
     */
    boolean aprovisionar(String email, String nombreCompleto, String password);
}

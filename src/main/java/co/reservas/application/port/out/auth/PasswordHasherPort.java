package co.reservas.application.port.out.auth;

public interface PasswordHasherPort {

    String hashear(String passwordPlano);

    boolean coincide(String passwordPlano, String hash);
}

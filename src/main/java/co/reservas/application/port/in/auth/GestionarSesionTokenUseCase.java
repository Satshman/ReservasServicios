package co.reservas.application.port.in.auth;

public interface GestionarSesionTokenUseCase {

    SesionResultado rotar(String refreshToken);

    void revocar(String refreshToken);
}
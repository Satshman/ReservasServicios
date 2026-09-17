package co.reservas.application.port.in.auth;

public interface IniciarSesionUseCase {

    SesionResultado iniciarSesion(String email, String password);
}

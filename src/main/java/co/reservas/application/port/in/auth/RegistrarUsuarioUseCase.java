package co.reservas.application.port.in.auth;

public interface RegistrarUsuarioUseCase {

    RegistroResultado registrar(RegistrarUsuarioComando comando);
}

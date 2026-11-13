package co.reservas.application.port.in.auth;

public interface MfaAdminUseCase {

    void solicitarCodigo(String email, String password);

    SesionResultado verificarCodigo(String email, String codigo);
}
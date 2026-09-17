package co.reservas.application.service.auth;

/**
 * Evento publicado al emitir un token de verificación. El correo se envía después del commit.
 */
public record VerificacionCuentaSolicitada(Integer idUsuario, String email, String nombre, String tokenPlano) {

    @Override
    public String toString() {
        return "VerificacionCuentaSolicitada[idUsuario=" + idUsuario + "]";
    }
}

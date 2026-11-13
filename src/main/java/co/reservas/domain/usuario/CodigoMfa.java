package co.reservas.domain.usuario;

import java.time.Instant;

/**
 * Desafío de segundo factor (MFA) para usuarios con rol ADMIN, almacenado como hash SHA-256.
 */
public record CodigoMfa(
        Integer id,
        Integer idUsuario,
        String codigoHash,
        Instant expiraEn,
        Instant usadoEn,
        Instant creadoEn
) {

    public static CodigoMfa nuevo(Integer idUsuario, String codigoClaro, Instant expiraEn, Instant creadoEn) {
        return new CodigoMfa(null, idUsuario, TokenVerificacion.calcularHash(codigoClaro),
                expiraEn, null, creadoEn);
    }

    public boolean esValido(Instant ahora) {
        return usadoEn == null && expiraEn.isAfter(ahora);
    }

    public CodigoMfa marcarUsado(Instant ahora) {
        return new CodigoMfa(id, idUsuario, codigoHash, expiraEn, ahora, creadoEn);
    }
}
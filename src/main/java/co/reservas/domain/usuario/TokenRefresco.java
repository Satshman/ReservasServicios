package co.reservas.domain.usuario;

import java.time.Instant;

/**
 * Token de refresco persistido como hash SHA-256 para renovar o revocar sesiones.
 */
public record TokenRefresco(
        Integer id,
        Integer idUsuario,
        String tokenHash,
        Instant expiraEn,
        Instant revocadoEn,
        Instant creadoEn
) {

    public static TokenRefresco nuevo(Integer idUsuario, String tokenEnClaro, Instant expiraEn, Instant creadoEn) {
        return new TokenRefresco(null, idUsuario, TokenVerificacion.calcularHash(tokenEnClaro),
                expiraEn, null, creadoEn);
    }

    public boolean estaActivo(Instant ahora) {
        return revocadoEn == null && expiraEn.isAfter(ahora);
    }

    public TokenRefresco revocar(Instant ahora) {
        return new TokenRefresco(id, idUsuario, tokenHash, expiraEn, ahora, creadoEn);
    }
}
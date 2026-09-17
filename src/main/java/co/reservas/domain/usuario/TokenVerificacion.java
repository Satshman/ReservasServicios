package co.reservas.domain.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de verificación de cuenta. Solo se guarda su hash SHA-256; el valor en claro viaja por correo.
 */
public record TokenVerificacion(Integer id, Integer idUsuario, String tokenHash, Instant expiraEn,
                                Instant usadoEn, Instant creadoEn) {

    public static final Duration VIGENCIA = Duration.ofHours(24);
    private static final int BYTES_ALEATORIOS = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public static TokenVerificacion emitir(Integer idUsuario, String tokenHash, Instant ahora) {
        return new TokenVerificacion(null, idUsuario, tokenHash, ahora.plus(VIGENCIA), null, ahora);
    }

    public static String generarValorPlano() {
        byte[] bytes = new byte[BYTES_ALEATORIOS];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String calcularHash(String valorPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valorPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public boolean esValido(Instant ahora) {
        return usadoEn == null && expiraEn.isAfter(ahora);
    }

    public TokenVerificacion marcarUsado(Instant ahora) {
        return new TokenVerificacion(id, idUsuario, tokenHash, expiraEn, ahora, creadoEn);
    }
}

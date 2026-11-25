package co.reservas.application.port.out.auth;

import co.reservas.domain.usuario.CodigoMfa;

import java.time.Instant;
import java.util.Optional;

public interface CodigoMfaRepositoryPort {

    CodigoMfa guardar(CodigoMfa codigo);

    Optional<CodigoMfa> buscarUltimoActivo(Integer idUsuario, String codigoHash);

    /**
     * Anula los códigos pendientes del usuario (los marca como usados) para que solo valga el más reciente.
     */
    void invalidarActivos(Integer idUsuario, Instant ahora);
}
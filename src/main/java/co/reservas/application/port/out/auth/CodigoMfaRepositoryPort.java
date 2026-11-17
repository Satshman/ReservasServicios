package co.reservas.application.port.out.auth;

import co.reservas.domain.usuario.CodigoMfa;

import java.util.Optional;

public interface CodigoMfaRepositoryPort {

    CodigoMfa guardar(CodigoMfa codigo);

    Optional<CodigoMfa> buscarUltimoActivo(Integer idUsuario, String codigoHash);
}
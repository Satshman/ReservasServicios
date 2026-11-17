package co.reservas.adapters.out.persistence.usuario;

import co.reservas.application.port.out.auth.CodigoMfaRepositoryPort;
import co.reservas.domain.usuario.CodigoMfa;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CodigoMfaRepositoryJpaAdapter implements CodigoMfaRepositoryPort {

    private final CodigoMfaJpaRepository jpa;

    public CodigoMfaRepositoryJpaAdapter(CodigoMfaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CodigoMfa guardar(CodigoMfa codigo) {
        return jpa.save(CodigoMfaEntity.deDominio(codigo)).aDominio();
    }

    @Override
    public Optional<CodigoMfa> buscarUltimoActivo(Integer idUsuario, String codigoHash) {
        return jpa.findFirstByIdUsuarioAndCodigoHashAndUsadoEnIsNullOrderByCreadoEnDesc(idUsuario, codigoHash)
                .map(CodigoMfaEntity::aDominio);
    }
}
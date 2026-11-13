package co.reservas.adapters.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoMfaJpaRepository extends JpaRepository<CodigoMfaEntity, Integer> {

    Optional<CodigoMfaEntity> findFirstByIdUsuarioAndCodigoHashAndUsadoEnIsNullOrderByCreadoEnDesc(
            Integer idUsuario, String codigoHash);
}
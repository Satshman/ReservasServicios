package co.reservas.adapters.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CodigoMfaJpaRepository extends JpaRepository<CodigoMfaEntity, Integer> {

    Optional<CodigoMfaEntity> findFirstByIdUsuarioAndCodigoHashAndUsadoEnIsNullOrderByCreadoEnDesc(
            Integer idUsuario, String codigoHash);

    @Modifying
    @Query("UPDATE CodigoMfaEntity c SET c.usadoEn = :ahora WHERE c.idUsuario = :idUsuario AND c.usadoEn IS NULL")
    void invalidarActivosPorUsuario(@Param("idUsuario") Integer idUsuario, @Param("ahora") Instant ahora);
}

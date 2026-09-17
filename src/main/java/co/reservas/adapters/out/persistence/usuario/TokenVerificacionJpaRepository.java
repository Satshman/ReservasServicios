package co.reservas.adapters.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenVerificacionJpaRepository extends JpaRepository<TokenVerificacionEntity, Integer> {

    Optional<TokenVerificacionEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenVerificacionEntity t set t.expiraEn = :ahora "
            + "where t.idUsuario = :idUsuario and t.usadoEn is null and t.expiraEn > :ahora")
    int invalidarPendientes(@Param("idUsuario") Integer idUsuario, @Param("ahora") Instant ahora);
}

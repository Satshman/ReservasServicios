package co.reservas.adapters.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenRefrescoJpaRepository extends JpaRepository<TokenRefrescoEntity, Integer> {

    Optional<TokenRefrescoEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE TokenRefrescoEntity t SET t.revocadoEn = :ahora WHERE t.idUsuario = :idUsuario AND t.revocadoEn IS NULL")
    void revocarActivosPorUsuario(@Param("idUsuario") Integer idUsuario, @Param("ahora") Instant ahora);
}
package co.reservas.adapters.out.persistence.usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Integer> {

    Optional<ClienteEntity> findByIdUsuario(Integer idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClienteEntity c where c.idUsuario = :idUsuario")
    Optional<ClienteEntity> bloquearPorIdUsuario(@Param("idUsuario") Integer idUsuario);
}

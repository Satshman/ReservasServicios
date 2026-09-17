package co.reservas.adapters.out.persistence.usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Integer> {

    @Query("select count(u) > 0 from UsuarioEntity u where lower(u.email) = :email")
    boolean existeEmail(@Param("email") String email);

    @Query("select u from UsuarioEntity u where lower(u.email) = :email")
    Optional<UsuarioEntity> buscarPorEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where lower(u.email) = :email")
    Optional<UsuarioEntity> bloquearPorEmail(@Param("email") String email);
}

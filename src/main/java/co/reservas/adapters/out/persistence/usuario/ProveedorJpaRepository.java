package co.reservas.adapters.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorEntity, Integer> {

    Optional<ProveedorEntity> findByIdUsuario(Integer idUsuario);
}

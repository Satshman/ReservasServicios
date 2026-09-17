package co.reservas.adapters.out.persistence.servicio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioJpaRepository extends JpaRepository<ServicioEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ServicioEntity s where s.id = :id")
    Optional<ServicioEntity> bloquearPorId(@Param("id") Integer id);

    @Query("select s from ServicioEntity s where s.idEstado = :idEstado "
            + "and (:idProveedor is null or s.idProveedor = :idProveedor) "
            + "and (:idCategoria is null or s.idCategoria = :idCategoria) "
            + "order by s.id")
    List<ServicioEntity> listar(@Param("idEstado") Integer idEstado, @Param("idProveedor") Integer idProveedor,
                                @Param("idCategoria") Integer idCategoria);

    boolean existsByIdProveedorAndNombre(Integer idProveedor, String nombre);
}

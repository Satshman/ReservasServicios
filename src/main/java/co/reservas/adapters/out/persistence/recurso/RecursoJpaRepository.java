package co.reservas.adapters.out.persistence.recurso;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecursoJpaRepository extends JpaRepository<RecursoEntity, Integer> {

    String ACTIVOS_DEL_SERVICIO = "select r from RecursoEntity r where r.activo = true and exists ("
            + "select 1 from ServicioRecursoEntity sr where sr.id.idRecurso = r.id and sr.id.idServicio = :idServicio"
            + ") order by r.id";

    boolean existsByIdProveedorAndNombre(Integer idProveedor, String nombre);

    List<RecursoEntity> findByIdProveedorOrderByIdAsc(Integer idProveedor);

    @Query(ACTIVOS_DEL_SERVICIO)
    List<RecursoEntity> listarActivosPorServicio(@Param("idServicio") Integer idServicio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(ACTIVOS_DEL_SERVICIO)
    List<RecursoEntity> bloquearActivosPorServicio(@Param("idServicio") Integer idServicio);
}

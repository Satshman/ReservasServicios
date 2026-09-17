package co.reservas.adapters.out.persistence.recurso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioRecursoJpaRepository extends JpaRepository<ServicioRecursoEntity, ServicioRecursoId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ServicioRecursoEntity sr where sr.id.idServicio = :idServicio")
    int eliminarPorServicio(@Param("idServicio") Integer idServicio);
}

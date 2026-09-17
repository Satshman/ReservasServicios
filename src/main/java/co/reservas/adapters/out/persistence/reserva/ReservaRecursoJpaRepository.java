package co.reservas.adapters.out.persistence.reserva;

import co.reservas.domain.recurso.OcupacionRecurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReservaRecursoJpaRepository extends JpaRepository<ReservaRecursoEntity, ReservaRecursoId> {

    List<ReservaRecursoEntity> findByIdIdReservaIn(Collection<Integer> idsReservas);

    @Query("select new co.reservas.domain.recurso.OcupacionRecurso("
            + "rr.id.idRecurso, r.id, r.idServicio, r.fechaHoraInicio, r.fechaHoraFin) "
            + "from ReservaRecursoEntity rr join ReservaEntity r on r.id = rr.id.idReserva "
            + "where rr.id.idRecurso in :idsRecursos and r.idEstado = :idEstado "
            + "and r.fechaHoraInicio < :hasta and r.fechaHoraFin > :desde")
    List<OcupacionRecurso> listarOcupaciones(@Param("idsRecursos") Collection<Integer> idsRecursos,
                                             @Param("idEstado") Integer idEstado,
                                             @Param("desde") Instant desde,
                                             @Param("hasta") Instant hasta);
}

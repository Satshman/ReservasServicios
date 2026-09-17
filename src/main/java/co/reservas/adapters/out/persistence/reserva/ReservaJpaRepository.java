package co.reservas.adapters.out.persistence.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservaJpaRepository extends JpaRepository<ReservaEntity, Integer> {

    long countByIdServicioAndFechaHoraInicioAndIdEstado(Integer idServicio, Instant fechaHoraInicio, Integer idEstado);

    @Query("select r.fechaHoraInicio as inicio, count(r) as cantidad from ReservaEntity r "
            + "where r.idServicio = :idServicio and r.idEstado = :idEstado "
            + "and r.fechaHoraInicio >= :desde and r.fechaHoraInicio < :hasta "
            + "group by r.fechaHoraInicio")
    List<ConteoTurno> contarPorTurno(@Param("idServicio") Integer idServicio, @Param("idEstado") Integer idEstado,
                                     @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select count(r) > 0 from ReservaEntity r where r.idCliente = :idCliente and r.idEstado = :idEstado "
            + "and r.fechaHoraInicio < :fin and r.fechaHoraFin > :inicio")
    boolean existeSolapeCliente(@Param("idCliente") Integer idCliente, @Param("idEstado") Integer idEstado,
                                @Param("inicio") Instant inicio, @Param("fin") Instant fin);

    List<ReservaEntity> findByIdServicioAndIdEstadoAndFechaHoraInicioAfter(Integer idServicio, Integer idEstado,
                                                                           Instant ahora);

    List<ReservaEntity> findByIdClienteOrderByFechaHoraInicioAsc(Integer idCliente);

    List<ReservaEntity> findByIdServicioOrderByFechaHoraInicioAsc(Integer idServicio);

    interface ConteoTurno {
        Instant getInicio();

        Long getCantidad();
    }
}

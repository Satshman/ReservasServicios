package co.reservas.adapters.out.persistence.agenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcepcionDisponibilidadJpaRepository extends JpaRepository<ExcepcionDisponibilidadEntity, Integer> {

    List<ExcepcionDisponibilidadEntity> findByIdServicioOrderByFechaAscHoraInicioAsc(Integer idServicio);
}
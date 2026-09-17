package co.reservas.adapters.out.persistence.agenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioDisponibleJpaRepository extends JpaRepository<HorarioDisponibleEntity, Integer> {

    List<HorarioDisponibleEntity> findByIdServicioOrderByDiaSemanaAscHoraInicioAsc(Integer idServicio);
}

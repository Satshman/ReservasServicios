package co.reservas.adapters.out.persistence.servicio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HistorialServicioJpaRepository extends JpaRepository<HistorialServicioEntity, Integer> {

    List<HistorialServicioEntity> findByIdServicioInOrderByFechaCambioAscIdAsc(Collection<Integer> idsServicios);
}

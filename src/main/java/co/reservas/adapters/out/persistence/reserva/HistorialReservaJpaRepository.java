package co.reservas.adapters.out.persistence.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HistorialReservaJpaRepository extends JpaRepository<HistorialReservaEntity, Integer> {

    List<HistorialReservaEntity> findByIdReservaInOrderByFechaCambioAscIdAsc(Collection<Integer> idsReservas);
}

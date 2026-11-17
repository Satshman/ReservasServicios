package co.reservas.adapters.out.persistence.agenda;

import co.reservas.application.port.out.agenda.ExcepcionDisponibilidadRepositoryPort;
import co.reservas.domain.agenda.ExcepcionDisponibilidad;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExcepcionDisponibilidadRepositoryJpaAdapter implements ExcepcionDisponibilidadRepositoryPort {

    private final ExcepcionDisponibilidadJpaRepository jpa;

    public ExcepcionDisponibilidadRepositoryJpaAdapter(ExcepcionDisponibilidadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ExcepcionDisponibilidad guardar(ExcepcionDisponibilidad excepcion) {
        return jpa.save(ExcepcionDisponibilidadEntity.deDominio(excepcion)).aDominio();
    }

    @Override
    public Optional<ExcepcionDisponibilidad> buscarPorId(Integer id) {
        return jpa.findById(id).map(ExcepcionDisponibilidadEntity::aDominio);
    }

    @Override
    public List<ExcepcionDisponibilidad> listarPorServicio(Integer idServicio) {
        return jpa.findByIdServicioOrderByFechaAscHoraInicioAsc(idServicio).stream()
                .map(ExcepcionDisponibilidadEntity::aDominio)
                .toList();
    }

    @Override
    public void eliminar(Integer id) {
        jpa.deleteById(id);
    }
}
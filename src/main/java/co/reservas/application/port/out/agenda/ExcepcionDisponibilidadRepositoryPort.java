package co.reservas.application.port.out.agenda;

import co.reservas.domain.agenda.ExcepcionDisponibilidad;

import java.util.List;
import java.util.Optional;

public interface ExcepcionDisponibilidadRepositoryPort {

    ExcepcionDisponibilidad guardar(ExcepcionDisponibilidad excepcion);

    Optional<ExcepcionDisponibilidad> buscarPorId(Integer id);

    List<ExcepcionDisponibilidad> listarPorServicio(Integer idServicio);

    void eliminar(Integer id);
}
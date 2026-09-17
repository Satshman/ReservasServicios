package co.reservas.application.port.out.agenda;

import co.reservas.domain.agenda.HorarioDisponible;

import java.util.List;
import java.util.Optional;

public interface AgendaRepositoryPort {

    List<HorarioDisponible> guardarTodos(List<HorarioDisponible> horarios);

    HorarioDisponible guardar(HorarioDisponible horario);

    Optional<HorarioDisponible> buscarPorId(Integer id);

    List<HorarioDisponible> listarPorServicio(Integer idServicio);

    void eliminar(Integer id);
}

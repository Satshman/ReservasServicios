package co.reservas.adapters.out.persistence.agenda;

import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.domain.agenda.HorarioDisponible;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Component
public class AgendaRepositoryJpaAdapter implements AgendaRepositoryPort {

    private final HorarioDisponibleJpaRepository horarios;

    public AgendaRepositoryJpaAdapter(HorarioDisponibleJpaRepository horarios) {
        this.horarios = horarios;
    }

    @Override
    public List<HorarioDisponible> guardarTodos(List<HorarioDisponible> nuevos) {
        return horarios.saveAll(nuevos.stream().map(AgendaRepositoryJpaAdapter::aEntidad).toList()).stream()
                .map(AgendaRepositoryJpaAdapter::aDominio)
                .toList();
    }

    @Override
    public HorarioDisponible guardar(HorarioDisponible horario) {
        return aDominio(horarios.save(aEntidad(horario)));
    }

    @Override
    public Optional<HorarioDisponible> buscarPorId(Integer id) {
        return horarios.findById(id).map(AgendaRepositoryJpaAdapter::aDominio);
    }

    @Override
    public List<HorarioDisponible> listarPorServicio(Integer idServicio) {
        return horarios.findByIdServicioOrderByDiaSemanaAscHoraInicioAsc(idServicio).stream()
                .map(AgendaRepositoryJpaAdapter::aDominio)
                .toList();
    }

    @Override
    public void eliminar(Integer id) {
        horarios.deleteById(id);
    }

    private static HorarioDisponibleEntity aEntidad(HorarioDisponible horario) {
        return new HorarioDisponibleEntity(horario.id(), horario.idServicio(),
                (short) horario.diaSemana().getValue(), horario.horaInicio(), horario.horaFin());
    }

    private static HorarioDisponible aDominio(HorarioDisponibleEntity entidad) {
        return new HorarioDisponible(entidad.getId(), entidad.getIdServicio(), DayOfWeek.of(entidad.getDiaSemana()),
                entidad.getHoraInicio(), entidad.getHoraFin());
    }
}

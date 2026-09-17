package co.reservas.adapters.out.persistence.reserva;

import co.reservas.adapters.out.persistence.CatalogoSistema;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.domain.recurso.OcupacionRecurso;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReservaRepositoryJpaAdapter implements ReservaRepositoryPort {

    private final ReservaJpaRepository reservas;
    private final ReservaRecursoJpaRepository reservasRecursos;
    private final HistorialReservaJpaRepository historial;
    private final CatalogoSistema catalogo;

    public ReservaRepositoryJpaAdapter(ReservaJpaRepository reservas, ReservaRecursoJpaRepository reservasRecursos,
                                       HistorialReservaJpaRepository historial, CatalogoSistema catalogo) {
        this.reservas = reservas;
        this.reservasRecursos = reservasRecursos;
        this.historial = historial;
        this.catalogo = catalogo;
    }

    @Override
    public Reserva guardar(Reserva reserva) {
        ReservaEntity guardada = reservas.saveAndFlush(new ReservaEntity(reserva.id(), reserva.idCliente(),
                reserva.idServicio(), catalogo.idEstado(reserva.estado()), reserva.fechaHoraInicio(),
                reserva.fechaHoraFin(), reserva.creadoEn()));
        reservasRecursos.saveAll(reserva.idsRecursos().stream()
                .map(idRecurso -> new ReservaRecursoEntity(guardada.getId(), idRecurso))
                .toList());
        return reserva.conId(guardada.getId());
    }

    @Override
    public void registrarHistorial(HistorialReserva cambio) {
        historial.save(new HistorialReservaEntity(cambio.idReserva(), catalogo.idEstadoReservaONulo(
                cambio.estadoAnterior()), catalogo.idEstado(cambio.estadoNuevo()), cambio.idUsuario(),
                cambio.fechaCambio()));
    }

    @Override
    public long contarConfirmadasEnTurno(Integer idServicio, Instant inicio) {
        return reservas.countByIdServicioAndFechaHoraInicioAndIdEstado(idServicio, inicio, confirmada());
    }

    @Override
    public Map<Instant, Long> contarConfirmadasPorTurno(Integer idServicio, Instant desde, Instant hasta) {
        return reservas.contarPorTurno(idServicio, confirmada(), desde, hasta).stream()
                .collect(Collectors.toMap(ReservaJpaRepository.ConteoTurno::getInicio,
                        ReservaJpaRepository.ConteoTurno::getCantidad));
    }

    @Override
    public boolean existeSolapeCliente(Integer idCliente, Instant inicio, Instant fin) {
        return reservas.existeSolapeCliente(idCliente, confirmada(), inicio, fin);
    }

    @Override
    public List<Reserva> listarConfirmadasFuturasPorServicio(Integer idServicio, Instant ahora) {
        return aDominio(reservas.findByIdServicioAndIdEstadoAndFechaHoraInicioAfter(idServicio, confirmada(), ahora));
    }

    @Override
    public List<OcupacionRecurso> listarOcupaciones(Collection<Integer> idsRecursos, Instant desde, Instant hasta) {
        if (idsRecursos.isEmpty()) {
            return List.of();
        }
        return reservasRecursos.listarOcupaciones(idsRecursos, confirmada(), desde, hasta);
    }

    @Override
    public List<Reserva> listarPorCliente(Integer idCliente) {
        return aDominio(reservas.findByIdClienteOrderByFechaHoraInicioAsc(idCliente));
    }

    @Override
    public List<Reserva> listarPorServicio(Integer idServicio) {
        return aDominio(reservas.findByIdServicioOrderByFechaHoraInicioAsc(idServicio));
    }

    private Integer confirmada() {
        return catalogo.idEstado(EstadoReserva.CONFIRMADA);
    }

    private List<Reserva> aDominio(List<ReservaEntity> entidades) {
        if (entidades.isEmpty()) {
            return List.of();
        }
        Map<Integer, Set<Integer>> recursosPorReserva = reservasRecursos
                .findByIdIdReservaIn(entidades.stream().map(ReservaEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(rr -> rr.getId().getIdReserva(),
                        Collectors.mapping(rr -> rr.getId().getIdRecurso(), Collectors.toSet())));
        return entidades.stream()
                .map(entidad -> new Reserva(entidad.getId(), entidad.getIdCliente(), entidad.getIdServicio(),
                        catalogo.estadoReserva(entidad.getIdEstado()), entidad.getFechaHoraInicio(),
                        entidad.getFechaHoraFin(), entidad.getCreadoEn(),
                        recursosPorReserva.getOrDefault(entidad.getId(), Set.of())))
                .toList();
    }
}

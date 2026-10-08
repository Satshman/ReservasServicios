package co.reservas.adapters.out.persistence.reserva;

import co.reservas.adapters.out.persistence.CatalogoSistema;
import co.reservas.application.port.out.reserva.CriterioBusquedaReservas;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.domain.recurso.OcupacionRecurso;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<HistorialReserva> listarHistorial(Collection<Integer> idsReservas) {
        if (idsReservas.isEmpty()) {
            return List.of();
        }
        return historial.findByIdReservaInOrderByFechaCambioAscIdAsc(idsReservas).stream()
                .map(entidad -> new HistorialReserva(entidad.getId(), entidad.getIdReserva(),
                        catalogo.estadoReserva(entidad.getIdEstadoAnterior()),
                        catalogo.estadoReserva(entidad.getIdEstadoNuevo()), entidad.getIdUsuario(),
                        entidad.getFechaCambio()))
                .toList();
    }

    @Override
    public Optional<Reserva> buscarPorId(Integer idReserva) {
        return reservas.findById(idReserva).map(entidad -> aDominio(List.of(entidad)).getFirst());
    }

    /**
     * Consulta parametrizada con Criteria API: solo se agregan los filtros presentes. Usa los índices
     * {@code (id_cliente, fecha_hora_inicio)} y {@code (id_servicio, fecha_hora_inicio)}.
     */
    @Override
    public List<Reserva> buscar(CriterioBusquedaReservas criterio) {
        if (criterio.idsServicios() != null && criterio.idsServicios().isEmpty()) {
            return List.of();
        }
        Integer idEstado = criterio.estado() == null ? null : catalogo.idEstado(criterio.estado());
        Specification<ReservaEntity> especificacion = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            Path<Instant> inicio = raiz.get("fechaHoraInicio");
            if (criterio.idCliente() != null) {
                condiciones.add(cb.equal(raiz.get("idCliente"), criterio.idCliente()));
            }
            if (criterio.idsServicios() != null) {
                condiciones.add(raiz.get("idServicio").in(criterio.idsServicios()));
            }
            if (idEstado != null) {
                condiciones.add(cb.equal(raiz.get("idEstado"), idEstado));
            }
            if (criterio.inicioDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(inicio, criterio.inicioDesde()));
            }
            if (criterio.inicioHasta() != null) {
                condiciones.add(cb.lessThan(inicio, criterio.inicioHasta()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        return aDominio(reservas.findAll(especificacion,
                Sort.by(Sort.Order.desc("fechaHoraInicio"), Sort.Order.desc("id"))));
    }

    @Override
    public Optional<Reserva> bloquearPorId(Integer idReserva) {
        return reservas.bloquearPorId(idReserva).map(entidad -> aDominio(List.of(entidad)).getFirst());
    }

    @Override
    public void actualizarEstado(Reserva reserva) {
        ReservaEntity entidad = reservas.findById(reserva.id())
                .orElseThrow(() -> new IllegalStateException("Reserva inexistente: " + reserva.id()));
        entidad.cambiarEstado(catalogo.idEstado(reserva.estado()));
        reservas.saveAndFlush(entidad);
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

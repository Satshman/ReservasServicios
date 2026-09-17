package co.reservas.application.service.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.recurso.RecursoResultado;
import co.reservas.application.port.in.reserva.ConsultarReservasUseCase;
import co.reservas.application.port.in.reserva.CrearReservaComando;
import co.reservas.application.port.in.reserva.CrearReservaUseCase;
import co.reservas.application.port.in.reserva.RecursoEnConflicto;
import co.reservas.application.port.in.reserva.ReservaResultado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.GeneradorTurnos;
import co.reservas.domain.recurso.DetectorConflictosRecursos;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservaService implements CrearReservaUseCase, ConsultarReservasUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepositoryPort reservas;
    private final ServicioRepositoryPort servicios;
    private final UsuarioRepositoryPort usuarios;
    private final AgendaRepositoryPort agenda;
    private final RecursoRepositoryPort recursos;
    private final CalculadoraDisponibilidad calculadora;
    private final VerificadorPropiedad verificador;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public ReservaService(ReservaRepositoryPort reservas, ServicioRepositoryPort servicios,
                          UsuarioRepositoryPort usuarios, AgendaRepositoryPort agenda, RecursoRepositoryPort recursos,
                          CalculadoraDisponibilidad calculadora, VerificadorPropiedad verificador, Clock clock) {
        this.reservas = reservas;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.agenda = agenda;
        this.recursos = recursos;
        this.calculadora = calculadora;
        this.verificador = verificador;
        this.clock = clock;
    }

    /**
     * Orden de bloqueo: cliente, servicio y recursos por id. Así se serializan las reservas concurrentes que
     * compiten por el mismo cupo, recurso o cliente sin generar interbloqueos.
     */
    @Override
    @Transactional
    public ReservaResultado crear(UsuarioAutenticado usuario, CrearReservaComando comando) {
        Cliente cliente = usuarios.bloquearClientePorUsuario(usuario.idUsuario())
                .orElseThrow(() -> VerificadorPropiedad.accesoDenegado("La operación requiere un cliente."));
        Servicio servicio = servicios.bloquearPorId(comando.idServicio())
                .orElseThrow(VerificadorPropiedad::servicioNoEncontrado);
        if (!servicio.estaActivo()) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, "El servicio no está disponible.");
        }
        Instant ahora = Instant.now(clock);
        Instant inicio = comando.fechaHoraInicio().toInstant();
        if (!inicio.isAfter(ahora)) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_EN_EL_PASADO, "La fecha y hora de inicio debe ser futura.");
        }
        ContextoAgenda contexto = contexto(servicio);
        if (!GeneradorTurnos.esTurnoValido(contexto.bloques(), servicio.duracion(), contexto.zona(), inicio)) {
            throw new ExcepcionNegocio(CodigoError.HORARIO_NO_DISPONIBLE,
                    "La fecha y hora no corresponde a un turno de la agenda del servicio.",
                    calculadora.sugerencias(contexto, inicio, ahora));
        }
        if (reservas.contarConfirmadasEnTurno(servicio.id(), inicio) >= servicio.capacidad()) {
            throw new ExcepcionNegocio(CodigoError.TURNO_SIN_CUPO, "El turno ya no tiene cupos disponibles.",
                    calculadora.sugerencias(contexto, inicio, ahora));
        }
        Instant fin = inicio.plus(servicio.duracion());
        if (reservas.existeSolapeCliente(cliente.id(), inicio, fin)) {
            throw new ExcepcionNegocio(CodigoError.RESERVA_SOLAPADA,
                    "Ya tienes una reserva confirmada que se cruza con ese horario.");
        }
        List<Recurso> recursosBloqueados = recursos.bloquearActivosPorServicio(servicio.id());
        validarRecursosLibres(contexto, recursosBloqueados, inicio, fin, ahora);

        Set<Integer> idsRecursos = recursosBloqueados.stream().map(Recurso::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Reserva reserva = reservas.guardar(Reserva.crear(cliente.id(), servicio, inicio, idsRecursos, ahora));
        reservas.registrarHistorial(HistorialReserva.creacion(reserva, usuario.idUsuario(), ahora));
        log.atInfo()
                .addKeyValue("evento", "RESERVA_CREADA")
                .addKeyValue("idReserva", reserva.id())
                .addKeyValue("idServicio", servicio.id())
                .addKeyValue("idUsuario", usuario.idUsuario())
                .log("Reserva creada");
        return mapear(reserva, servicio, contexto.zona(), indexar(recursosBloqueados));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResultado> consultarMias(UsuarioAutenticado usuario) {
        Cliente cliente = usuarios.buscarClientePorUsuario(usuario.idUsuario())
                .orElseThrow(() -> VerificadorPropiedad.accesoDenegado("La operación requiere un cliente."));
        return mapearTodas(reservas.listarPorCliente(cliente.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResultado> consultarPorServicio(UsuarioAutenticado usuario, Integer idServicio) {
        ServicioPropio propio = verificador.servicioPropio(usuario, idServicio);
        return mapearTodas(reservas.listarPorServicio(propio.servicio().id()));
    }

    private ContextoAgenda contexto(Servicio servicio) {
        Proveedor proveedor = usuarios.buscarProveedorPorId(servicio.idProveedor())
                .orElseThrow(() -> new IllegalStateException("Servicio sin proveedor: " + servicio.id()));
        return new ContextoAgenda(servicio, proveedor.zonaHoraria(), agenda.listarPorServicio(servicio.id()),
                recursos.listarActivosPorServicio(servicio.id()));
    }

    private void validarRecursosLibres(ContextoAgenda contexto, List<Recurso> recursosBloqueados, Instant inicio,
                                       Instant fin, Instant ahora) {
        if (recursosBloqueados.isEmpty()) {
            return;
        }
        List<Integer> ids = recursosBloqueados.stream().map(Recurso::id).toList();
        SortedSet<Integer> ocupados = DetectorConflictosRecursos.recursosOcupados(ids, contexto.servicio().id(),
                inicio, fin, reservas.listarOcupaciones(ids, inicio, fin));
        if (ocupados.isEmpty()) {
            return;
        }
        Map<Integer, Recurso> porId = indexar(recursosBloqueados);
        List<Object> detalles = new ArrayList<>();
        ocupados.forEach(id -> detalles.add(new RecursoEnConflicto(id, porId.get(id).nombre())));
        ContextoAgenda actualizado = new ContextoAgenda(contexto.servicio(), contexto.zona(), contexto.bloques(),
                recursosBloqueados);
        detalles.addAll(calculadora.sugerencias(actualizado, inicio, ahora));
        throw new ExcepcionNegocio(CodigoError.RECURSO_NO_DISPONIBLE,
                "Uno o más recursos del servicio ya están ocupados en ese horario.", detalles);
    }

    private List<ReservaResultado> mapearTodas(List<Reserva> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Integer, Servicio> serviciosPorId = servicios.buscarPorIds(
                        lista.stream().map(Reserva::idServicio).distinct().toList()).stream()
                .collect(Collectors.toMap(Servicio::id, Function.identity()));
        Map<Integer, Proveedor> proveedoresPorId = usuarios.buscarProveedoresPorIds(
                        serviciosPorId.values().stream().map(Servicio::idProveedor).distinct().toList()).stream()
                .collect(Collectors.toMap(Proveedor::id, Function.identity()));
        Map<Integer, Recurso> recursosPorId = indexar(recursos.buscarPorIds(
                lista.stream().flatMap(reserva -> reserva.idsRecursos().stream()).distinct().toList()));
        return lista.stream()
                .sorted(Comparator.comparing(Reserva::fechaHoraInicio))
                .map(reserva -> {
                    Servicio servicio = serviciosPorId.get(reserva.idServicio());
                    ZoneId zona = proveedoresPorId.get(servicio.idProveedor()).zonaHoraria();
                    return mapear(reserva, servicio, zona, recursosPorId);
                })
                .toList();
    }

    private static Map<Integer, Recurso> indexar(Collection<Recurso> lista) {
        return lista.stream().collect(Collectors.toMap(Recurso::id, Function.identity()));
    }

    private static ReservaResultado mapear(Reserva reserva, Servicio servicio, ZoneId zona,
                                           Map<Integer, Recurso> recursosPorId) {
        List<RecursoResultado> recursosReserva = reserva.idsRecursos().stream()
                .sorted()
                .map(recursosPorId::get)
                .map(RecursoResultado::de)
                .toList();
        return new ReservaResultado(reserva.id(), servicio.id(), servicio.nombre(), reserva.idCliente(),
                reserva.estado(), reserva.fechaHoraInicio().atZone(zona).toOffsetDateTime(),
                reserva.fechaHoraFin().atZone(zona).toOffsetDateTime(), recursosReserva, reserva.creadoEn());
    }
}

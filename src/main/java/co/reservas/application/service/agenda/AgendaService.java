package co.reservas.application.service.agenda;

import co.reservas.application.port.in.agenda.CambioHorarioResultado;
import co.reservas.application.port.in.agenda.ConsultarHorariosUseCase;
import co.reservas.application.port.in.agenda.CrearHorarioComando;
import co.reservas.application.port.in.agenda.CrearHorarioUseCase;
import co.reservas.application.port.in.agenda.EditarHorarioComando;
import co.reservas.application.port.in.agenda.EditarHorarioUseCase;
import co.reservas.application.port.in.agenda.EliminarHorarioUseCase;
import co.reservas.application.port.in.agenda.HorarioResultado;
import co.reservas.application.port.in.agenda.ReservaAfectada;
import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.DetectorReservasAfectadas;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class AgendaService implements CrearHorarioUseCase, ConsultarHorariosUseCase, EditarHorarioUseCase,
        EliminarHorarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    private final AgendaRepositoryPort agenda;
    private final ServicioRepositoryPort servicios;
    private final ReservaRepositoryPort reservas;
    private final VerificadorPropiedad verificador;
    private final Clock clock;

    public AgendaService(AgendaRepositoryPort agenda, ServicioRepositoryPort servicios, ReservaRepositoryPort reservas,
                         VerificadorPropiedad verificador, Clock clock) {
        this.agenda = agenda;
        this.servicios = servicios;
        this.reservas = reservas;
        this.verificador = verificador;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<HorarioResultado> crear(UsuarioAutenticado usuario, Integer idServicio, CrearHorarioComando comando) {
        Servicio servicio = verificador.servicioPropioBloqueado(usuario, idServicio).servicio();
        if (comando.diasSemana().isEmpty()) {
            throw ExcepcionNegocio.validacion("diasSemana", "Debe indicar al menos un día");
        }
        List<HorarioDisponible> nuevos = comando.diasSemana().stream()
                .sorted()
                .map(dia -> HorarioDisponible.nuevo(null, idServicio, dia, comando.horaInicio(), comando.horaFin()))
                .toList();
        nuevos.forEach(horario -> horario.validarCubreDuracion(servicio.duracion()));
        HorarioDisponible.validarSinSolapes(nuevos, agenda.listarPorServicio(idServicio));

        List<HorarioResultado> creados = agenda.guardarTodos(nuevos).stream().map(AgendaService::mapear).toList();
        log.atInfo()
                .addKeyValue("evento", "HORARIOS_CREADOS")
                .addKeyValue("idServicio", idServicio)
                .addKeyValue("idUsuario", usuario.idUsuario())
                .addKeyValue("cantidad", creados.size())
                .log("Bloques de agenda creados");
        return creados;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioResultado> consultar(Integer idServicio) {
        if (servicios.buscarPorId(idServicio).isEmpty()) {
            throw VerificadorPropiedad.servicioNoEncontrado();
        }
        return agenda.listarPorServicio(idServicio).stream()
                .sorted(Comparator.comparing(HorarioDisponible::diaSemana).thenComparing(HorarioDisponible::horaInicio))
                .map(AgendaService::mapear)
                .toList();
    }

    @Override
    @Transactional
    public CambioHorarioResultado editar(UsuarioAutenticado usuario, Integer idHorario, EditarHorarioComando comando,
                                         boolean confirmar) {
        HorarioDisponible original = buscarHorario(idHorario);
        ServicioPropio propio = verificador.servicioPropioBloqueado(usuario, original.idServicio());
        HorarioDisponible nuevo = HorarioDisponible.nuevo(original.id(), original.idServicio(), comando.diaSemana(),
                comando.horaInicio(), comando.horaFin());
        nuevo.validarCubreDuracion(propio.servicio().duracion());
        HorarioDisponible.validarSinSolapes(List.of(nuevo), agenda.listarPorServicio(original.idServicio()));

        Instant ahora = Instant.now(clock);
        ZoneId zona = propio.proveedor().zonaHoraria();
        List<Reserva> afectadas = DetectorReservasAfectadas.alEditar(original, nuevo,
                reservas.listarConfirmadasFuturasPorServicio(original.idServicio(), ahora), zona, ahora);
        exigirConfirmacion(afectadas, confirmar, zona);

        HorarioResultado guardado = mapear(agenda.guardar(nuevo));
        registrarCambio("HORARIO_EDITADO", usuario, idHorario, afectadas.size());
        return new CambioHorarioResultado(guardado, mapearAfectadas(afectadas, zona));
    }

    @Override
    @Transactional
    public CambioHorarioResultado eliminar(UsuarioAutenticado usuario, Integer idHorario, boolean confirmar) {
        HorarioDisponible original = buscarHorario(idHorario);
        ServicioPropio propio = verificador.servicioPropioBloqueado(usuario, original.idServicio());

        Instant ahora = Instant.now(clock);
        ZoneId zona = propio.proveedor().zonaHoraria();
        List<Reserva> afectadas = DetectorReservasAfectadas.alEliminar(original,
                reservas.listarConfirmadasFuturasPorServicio(original.idServicio(), ahora), zona, ahora);
        exigirConfirmacion(afectadas, confirmar, zona);

        agenda.eliminar(idHorario);
        registrarCambio("HORARIO_ELIMINADO", usuario, idHorario, afectadas.size());
        return new CambioHorarioResultado(null, mapearAfectadas(afectadas, zona));
    }

    private HorarioDisponible buscarHorario(Integer idHorario) {
        return agenda.buscarPorId(idHorario)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.HORARIO_NO_ENCONTRADO, "El horario no existe."));
    }

    private static void exigirConfirmacion(List<Reserva> afectadas, boolean confirmar, ZoneId zona) {
        if (!afectadas.isEmpty() && !confirmar) {
            throw new ExcepcionNegocio(CodigoError.HORARIO_CON_RESERVAS,
                    "El cambio afecta reservas confirmadas. Repite la solicitud con confirmar=true para aplicarlo.",
                    mapearAfectadas(afectadas, zona));
        }
    }

    private static List<ReservaAfectada> mapearAfectadas(List<Reserva> afectadas, ZoneId zona) {
        return afectadas.stream()
                .map(reserva -> new ReservaAfectada(reserva.id(),
                        reserva.fechaHoraInicio().atZone(zona).toOffsetDateTime(),
                        reserva.fechaHoraFin().atZone(zona).toOffsetDateTime()))
                .toList();
    }

    private static void registrarCambio(String evento, UsuarioAutenticado usuario, Integer idHorario,
                                        int reservasAfectadas) {
        log.atInfo()
                .addKeyValue("evento", evento)
                .addKeyValue("idHorario", idHorario)
                .addKeyValue("idUsuario", usuario.idUsuario())
                .addKeyValue("reservasAfectadas", reservasAfectadas)
                .log("Cambio de agenda aplicado");
    }

    static HorarioResultado mapear(HorarioDisponible horario) {
        return new HorarioResultado(horario.id(), horario.idServicio(), horario.diaSemana().getValue(),
                horario.horaInicio(), horario.horaFin());
    }
}

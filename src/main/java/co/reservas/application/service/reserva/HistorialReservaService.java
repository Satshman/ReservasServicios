package co.reservas.application.service.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reserva.CambioEstadoReservaResultado;
import co.reservas.application.port.in.reserva.ConsultarHistorialReservasUseCase;
import co.reservas.application.port.in.reserva.FiltroHistorialReservas;
import co.reservas.application.port.in.reserva.HistorialReservaResultado;
import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.application.port.out.reserva.CriterioBusquedaReservas;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.application.service.usuario.AutoresCambio;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU-10 y HU-14. El rol del token decide el alcance: el cliente ve sus reservas y el proveedor las de sus servicios.
 */
@Service
public class HistorialReservaService implements ConsultarHistorialReservasUseCase {

    private final ReservaRepositoryPort reservas;
    private final ServicioRepositoryPort servicios;
    private final UsuarioRepositoryPort usuarios;
    private final VerificadorPropiedad verificador;
    private final AutoresCambio autores;

    public HistorialReservaService(ReservaRepositoryPort reservas, ServicioRepositoryPort servicios,
                                   UsuarioRepositoryPort usuarios, VerificadorPropiedad verificador,
                                   AutoresCambio autores) {
        this.reservas = reservas;
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.verificador = verificador;
        this.autores = autores;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialReservaResultado> consultar(UsuarioAutenticado usuario, FiltroHistorialReservas filtro) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.hasta().isBefore(filtro.desde())) {
            throw ExcepcionNegocio.validacion("hasta", "Debe ser igual o posterior a desde");
        }
        CriterioBusquedaReservas criterio = switch (usuario.rol()) {
            case CLIENTE -> criterio(clienteDe(usuario).id(), idsServiciosCliente(filtro), filtro);
            case PROVEEDOR -> criterio(null, idsServiciosProveedor(usuario, filtro), filtro);
            default -> throw VerificadorPropiedad.accesoDenegado("La operación requiere un cliente o un proveedor.");
        };
        return mapear(reservas.buscar(criterio), filtro);
    }

    /**
     * Orden de validación: existencia (404) y propiedad (403), igual que la cancelación.
     */
    @Override
    @Transactional(readOnly = true)
    public HistorialReservaResultado consultarReserva(UsuarioAutenticado usuario, Integer idReserva) {
        Reserva reserva = reservas.buscarPorId(idReserva)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RESERVA_NO_ENCONTRADA, "La reserva no existe."));
        verificador.exigirAccesoAReserva(usuario, reserva);
        return mapear(List.of(reserva), FiltroHistorialReservas.sinFiltros()).getFirst();
    }

    private Cliente clienteDe(UsuarioAutenticado usuario) {
        return usuarios.buscarClientePorUsuario(usuario.idUsuario())
                .orElseThrow(() -> VerificadorPropiedad.accesoDenegado("La operación requiere un cliente."));
    }

    private static Set<Integer> idsServiciosCliente(FiltroHistorialReservas filtro) {
        return filtro.idServicio() == null ? null : Set.of(filtro.idServicio());
    }

    /**
     * Un servicio ajeno produce 403; sin filtro se usan todos los servicios del proveedor.
     */
    private Set<Integer> idsServiciosProveedor(UsuarioAutenticado usuario, FiltroHistorialReservas filtro) {
        if (filtro.idServicio() != null) {
            return Set.of(verificador.servicioPropio(usuario, filtro.idServicio()).servicio().id());
        }
        Proveedor proveedor = verificador.proveedorDe(usuario);
        return servicios.listarPorProveedor(proveedor.id()).stream().map(Servicio::id).collect(Collectors.toSet());
    }

    /**
     * Las fechas del filtro son locales a la zona de cada proveedor, que puede variar entre las reservas de un
     * cliente. La base de datos filtra con un margen de un día a cada lado (cubre cualquier desfase UTC) y
     * {@link #dentroDelRango} aplica el rango exacto.
     */
    private static CriterioBusquedaReservas criterio(Integer idCliente, Set<Integer> idsServicios,
                                                     FiltroHistorialReservas filtro) {
        Instant desde = filtro.desde() == null ? null
                : filtro.desde().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant hasta = filtro.hasta() == null ? null
                : filtro.hasta().plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new CriterioBusquedaReservas(idCliente, idsServicios, filtro.estado(), desde, hasta);
    }

    private static boolean dentroDelRango(Reserva reserva, ZoneId zona, FiltroHistorialReservas filtro) {
        LocalDate fecha = reserva.fechaHoraInicio().atZone(zona).toLocalDate();
        return (filtro.desde() == null || !fecha.isBefore(filtro.desde()))
                && (filtro.hasta() == null || !fecha.isAfter(filtro.hasta()));
    }

    /**
     * Conserva el orden recibido (de la más reciente a la más antigua) y agrega los cambios de cada reserva.
     */
    private List<HistorialReservaResultado> mapear(List<Reserva> lista, FiltroHistorialReservas filtro) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Integer, Servicio> serviciosPorId = servicios.buscarPorIds(
                        lista.stream().map(Reserva::idServicio).distinct().toList()).stream()
                .collect(Collectors.toMap(Servicio::id, Function.identity()));
        Map<Integer, ZoneId> zonaPorProveedor = usuarios.buscarProveedoresPorIds(
                        serviciosPorId.values().stream().map(Servicio::idProveedor).distinct().toList()).stream()
                .collect(Collectors.toMap(Proveedor::id, Proveedor::zonaHoraria));
        List<Reserva> enRango = lista.stream()
                .filter(reserva -> dentroDelRango(reserva, zonaDe(reserva, serviciosPorId, zonaPorProveedor), filtro))
                .toList();
        Map<Integer, List<HistorialReserva>> cambiosPorReserva = reservas.listarHistorial(
                        enRango.stream().map(Reserva::id).toList()).stream()
                .collect(Collectors.groupingBy(HistorialReserva::idReserva));
        Map<Integer, AutorCambio> autoresPorId = autores.porId(cambiosPorReserva.values().stream()
                .flatMap(List::stream).map(HistorialReserva::idUsuario).toList());
        return enRango.stream()
                .map(reserva -> {
                    ZoneId zona = zonaDe(reserva, serviciosPorId, zonaPorProveedor);
                    List<CambioEstadoReservaResultado> cambios = cambiosPorReserva
                            .getOrDefault(reserva.id(), List.of()).stream()
                            .map(cambio -> new CambioEstadoReservaResultado(cambio.estadoAnterior(),
                                    cambio.estadoNuevo(), autoresPorId.get(cambio.idUsuario()),
                                    cambio.fechaCambio()))
                            .toList();
                    return new HistorialReservaResultado(reserva.id(), reserva.idServicio(),
                            serviciosPorId.get(reserva.idServicio()).nombre(), reserva.idCliente(), reserva.estado(),
                            reserva.fechaHoraInicio().atZone(zona).toOffsetDateTime(),
                            reserva.fechaHoraFin().atZone(zona).toOffsetDateTime(), reserva.creadoEn(), cambios);
                })
                .toList();
    }

    private static ZoneId zonaDe(Reserva reserva, Map<Integer, Servicio> serviciosPorId,
                                 Map<Integer, ZoneId> zonaPorProveedor) {
        return zonaPorProveedor.get(serviciosPorId.get(reserva.idServicio()).idProveedor());
    }
}

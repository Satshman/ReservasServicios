package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.servicio.GestionarServiciosUseCase;
import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.in.servicio.ServicioResultado;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.HistorialServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioService implements GestionarServiciosUseCase {

    private static final Logger log = LoggerFactory.getLogger(ServicioService.class);

    private final ServicioRepositoryPort servicios;
    private final UsuarioRepositoryPort usuarios;
    private final RegistroServicios registroServicios;
    private final VerificadorPropiedad verificador;
    private final Clock clock;

    public ServicioService(ServicioRepositoryPort servicios, UsuarioRepositoryPort usuarios,
                           RegistroServicios registroServicios, VerificadorPropiedad verificador, Clock clock) {
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.registroServicios = registroServicios;
        this.verificador = verificador;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ServicioResultado crear(UsuarioAutenticado usuario, NuevoServicioComando comando) {
        Proveedor proveedor = verificador.proveedorDe(usuario);
        List<NuevoServicioComando> comandos = List.of(comando);
        registroServicios.validar(comandos, indice -> "");
        if (servicios.existeNombre(proveedor.id(), comando.nombre().trim())) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_YA_REGISTRADO,
                    "El proveedor ya tiene un servicio con ese nombre.");
        }
        Servicio servicio = registroServicios.guardar(proveedor.id(), comandos, usuario.idUsuario(),
                Instant.now(clock)).getFirst();
        return mapear(servicio, proveedor);
    }

    @Override
    @Transactional
    public ServicioResultado cambiarEstado(UsuarioAutenticado usuario, Integer idServicio, EstadoServicio estado) {
        ServicioPropio propio = verificador.servicioPropioBloqueado(usuario, idServicio);
        Servicio actual = propio.servicio();
        if (actual.estado() == estado) {
            return mapear(actual, propio.proveedor());
        }
        Servicio actualizado = servicios.guardar(actual.conEstado(estado));
        servicios.registrarHistorial(HistorialServicio.cambioDeEstado(actual.estado(), actualizado,
                usuario.idUsuario(), Instant.now(clock)));
        log.atInfo()
                .addKeyValue("evento", "SERVICIO_ESTADO_CAMBIADO")
                .addKeyValue("idServicio", idServicio)
                .addKeyValue("idUsuario", usuario.idUsuario())
                .addKeyValue("estado", estado)
                .log("Estado del servicio actualizado");
        return mapear(actualizado, propio.proveedor());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioResultado> listar(Integer idProveedor, Integer idCategoria) {
        List<Servicio> encontrados = servicios.listarActivos(idProveedor, idCategoria);
        Map<Integer, Proveedor> proveedores = usuarios.buscarProveedoresPorIds(
                        encontrados.stream().map(Servicio::idProveedor).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Proveedor::id, Function.identity()));
        return encontrados.stream()
                .map(servicio -> mapear(servicio, proveedores.get(servicio.idProveedor())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServicioResultado consultar(Integer idServicio) {
        Servicio servicio = servicios.buscarPorId(idServicio)
                .orElseThrow(VerificadorPropiedad::servicioNoEncontrado);
        Proveedor proveedor = usuarios.buscarProveedorPorId(servicio.idProveedor())
                .orElseThrow(() -> new IllegalStateException("Servicio sin proveedor: " + idServicio));
        return mapear(servicio, proveedor);
    }

    static ServicioResultado mapear(Servicio servicio, Proveedor proveedor) {
        return new ServicioResultado(servicio.id(), proveedor.id(), proveedor.nombreComercial(),
                proveedor.zonaHoraria().getId(), servicio.idCategoria(), servicio.nombre(), servicio.descripcion(),
                servicio.duracion().toMinutes(), servicio.capacidad(), servicio.estado());
    }
}

package co.reservas.application.service.recurso;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.recurso.CrearRecursoComando;
import co.reservas.application.port.in.recurso.GestionarRecursosUseCase;
import co.reservas.application.port.in.recurso.RecursoResultado;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.HistorialServicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecursoService implements GestionarRecursosUseCase {

    private final RecursoRepositoryPort recursos;
    private final CatalogoRepositoryPort catalogos;
    private final ServicioRepositoryPort servicios;
    private final VerificadorPropiedad verificador;
    private final Clock clock;

    public RecursoService(RecursoRepositoryPort recursos, CatalogoRepositoryPort catalogos,
                          ServicioRepositoryPort servicios, VerificadorPropiedad verificador, Clock clock) {
        this.recursos = recursos;
        this.catalogos = catalogos;
        this.servicios = servicios;
        this.verificador = verificador;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RecursoResultado crear(UsuarioAutenticado usuario, CrearRecursoComando comando) {
        Proveedor proveedor = verificador.proveedorDe(usuario);
        Recurso recurso = Recurso.nuevo(proveedor.id(), comando.idTipoRecurso(), comando.nombre());
        if (!catalogos.existeTipoRecurso(comando.idTipoRecurso())) {
            throw new ExcepcionNegocio(CodigoError.TIPO_RECURSO_NO_ENCONTRADO, "El tipo de recurso indicado no existe.");
        }
        if (recursos.existeNombre(proveedor.id(), recurso.nombre())) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_YA_REGISTRADO,
                    "El proveedor ya tiene un recurso con ese nombre.");
        }
        return RecursoResultado.de(recursos.guardar(recurso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResultado> listar(UsuarioAutenticado usuario) {
        Proveedor proveedor = verificador.proveedorDe(usuario);
        return recursos.listarPorProveedor(proveedor.id()).stream()
                .sorted(Comparator.comparing(Recurso::id))
                .map(RecursoResultado::de)
                .toList();
    }

    @Override
    @Transactional
    public List<RecursoResultado> asignarAServicio(UsuarioAutenticado usuario, Integer idServicio,
                                                   Set<Integer> idsRecursos) {
        ServicioPropio propio = verificador.servicioPropioBloqueado(usuario, idServicio);
        Set<Integer> ids = new TreeSet<>(idsRecursos);
        Map<Integer, Recurso> encontrados = recursos.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Recurso::id, Function.identity()));
        for (Integer id : ids) {
            validarAsignable(encontrados.get(id), id, propio.proveedor());
        }
        List<Recurso> anteriores = recursos.listarAsignadosAServicio(idServicio);
        recursos.reemplazarAsignaciones(idServicio, ids);
        List<Recurso> asignados = ids.stream().map(encontrados::get).toList();
        registrarAsignacion(usuario, idServicio, anteriores, asignados);
        return asignados.stream().map(RecursoResultado::de).toList();
    }

    /**
     * Registra el cambio en el historial del servicio (HU-11) solo si el conjunto de recursos cambió.
     */
    private void registrarAsignacion(UsuarioAutenticado usuario, Integer idServicio, List<Recurso> anteriores,
                                     List<Recurso> nuevos) {
        Set<Integer> idsAnteriores = anteriores.stream().map(Recurso::id).collect(Collectors.toSet());
        Set<Integer> idsNuevos = nuevos.stream().map(Recurso::id).collect(Collectors.toSet());
        if (idsAnteriores.equals(idsNuevos)) {
            return;
        }
        servicios.registrarHistorial(HistorialServicio.recursosAsignados(idServicio,
                anteriores.stream().map(Recurso::nombre).toList(), nuevos.stream().map(Recurso::nombre).toList(),
                usuario.idUsuario(), Instant.now(clock)));
    }

    private static void validarAsignable(Recurso recurso, Integer id, Proveedor proveedor) {
        if (recurso == null) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El recurso no existe.",
                    List.of(new DetalleCampo("idsRecursos", "No existe el recurso " + id)));
        }
        if (!recurso.perteneceA(proveedor.id())) {
            throw VerificadorPropiedad.accesoDenegado("El recurso " + id + " no pertenece al proveedor autenticado.");
        }
        if (!recurso.activo()) {
            throw ExcepcionNegocio.validacion("idsRecursos", "El recurso " + id + " está inactivo");
        }
    }
}

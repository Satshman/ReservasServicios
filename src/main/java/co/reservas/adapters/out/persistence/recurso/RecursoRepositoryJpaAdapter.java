package co.reservas.adapters.out.persistence.recurso;

import co.reservas.adapters.out.persistence.RestriccionesUnicas;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class RecursoRepositoryJpaAdapter implements RecursoRepositoryPort {

    private final RecursoJpaRepository recursos;
    private final ServicioRecursoJpaRepository asignaciones;

    public RecursoRepositoryJpaAdapter(RecursoJpaRepository recursos, ServicioRecursoJpaRepository asignaciones) {
        this.recursos = recursos;
        this.asignaciones = asignaciones;
    }

    @Override
    public Recurso guardar(Recurso recurso) {
        RecursoEntity entidad = new RecursoEntity(recurso.id(), recurso.idProveedor(), recurso.idTipoRecurso(),
                recurso.nombre(), recurso.activo());
        return aDominio(RestriccionesUnicas.traducir(() -> recursos.saveAndFlush(entidad),
                "uk_recursos_proveedor_nombre", () -> new ExcepcionNegocio(CodigoError.RECURSO_YA_REGISTRADO,
                        "El proveedor ya tiene un recurso con ese nombre.")));
    }

    @Override
    public boolean existeNombre(Integer idProveedor, String nombre) {
        return recursos.existsByIdProveedorAndNombre(idProveedor, nombre);
    }

    @Override
    public List<Recurso> listarPorProveedor(Integer idProveedor) {
        return recursos.findByIdProveedorOrderByIdAsc(idProveedor).stream()
                .map(RecursoRepositoryJpaAdapter::aDominio)
                .toList();
    }

    @Override
    public List<Recurso> buscarPorIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return recursos.findAllById(ids).stream().map(RecursoRepositoryJpaAdapter::aDominio).toList();
    }

    @Override
    public List<Recurso> listarActivosPorServicio(Integer idServicio) {
        return recursos.listarActivosPorServicio(idServicio).stream()
                .map(RecursoRepositoryJpaAdapter::aDominio)
                .toList();
    }

    @Override
    public List<Recurso> bloquearActivosPorServicio(Integer idServicio) {
        return recursos.bloquearActivosPorServicio(idServicio).stream()
                .map(RecursoRepositoryJpaAdapter::aDominio)
                .toList();
    }

    @Override
    public void reemplazarAsignaciones(Integer idServicio, Set<Integer> idsRecursos) {
        asignaciones.eliminarPorServicio(idServicio);
        asignaciones.saveAll(idsRecursos.stream()
                .map(idRecurso -> new ServicioRecursoEntity(idServicio, idRecurso))
                .toList());
    }

    private static Recurso aDominio(RecursoEntity entidad) {
        return new Recurso(entidad.getId(), entidad.getIdProveedor(), entidad.getIdTipoRecurso(),
                entidad.getNombre(), entidad.isActivo());
    }
}

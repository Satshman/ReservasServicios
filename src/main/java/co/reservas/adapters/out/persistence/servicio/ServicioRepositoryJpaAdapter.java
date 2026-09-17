package co.reservas.adapters.out.persistence.servicio;

import co.reservas.adapters.out.persistence.CatalogoSistema;
import co.reservas.adapters.out.persistence.RestriccionesUnicas;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class ServicioRepositoryJpaAdapter implements ServicioRepositoryPort {

    private final ServicioJpaRepository servicios;
    private final CatalogoSistema catalogo;

    public ServicioRepositoryJpaAdapter(ServicioJpaRepository servicios, CatalogoSistema catalogo) {
        this.servicios = servicios;
        this.catalogo = catalogo;
    }

    @Override
    public Servicio guardar(Servicio servicio) {
        ServicioEntity entidad = new ServicioEntity(servicio.id(), servicio.idProveedor(), servicio.idCategoria(),
                catalogo.idEstado(servicio.estado()), servicio.nombre(), servicio.descripcion(), servicio.duracion(),
                (short) servicio.capacidad());
        return aDominio(RestriccionesUnicas.traducir(() -> servicios.saveAndFlush(entidad),
                "uk_servicios_proveedor_nombre", () -> new ExcepcionNegocio(CodigoError.SERVICIO_YA_REGISTRADO,
                        "El proveedor ya tiene un servicio con ese nombre.")));
    }

    @Override
    public Optional<Servicio> buscarPorId(Integer id) {
        return servicios.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Servicio> bloquearPorId(Integer id) {
        return servicios.bloquearPorId(id).map(this::aDominio);
    }

    @Override
    public List<Servicio> buscarPorIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return servicios.findAllById(ids).stream().map(this::aDominio).toList();
    }

    @Override
    public List<Servicio> listarActivos(Integer idProveedor, Integer idCategoria) {
        return servicios.listar(catalogo.idEstado(EstadoServicio.ACTIVO), idProveedor, idCategoria).stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public boolean existeNombre(Integer idProveedor, String nombre) {
        return servicios.existsByIdProveedorAndNombre(idProveedor, nombre);
    }

    private Servicio aDominio(ServicioEntity entidad) {
        return new Servicio(entidad.getId(), entidad.getIdProveedor(), entidad.getIdCategoria(),
                catalogo.estadoServicio(entidad.getIdEstado()), entidad.getNombre(), entidad.getDescripcion(),
                entidad.getDuracion(), entidad.getCapacidad());
    }
}

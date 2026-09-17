package co.reservas.application.port.out.recurso;

import co.reservas.domain.recurso.Recurso;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RecursoRepositoryPort {

    Recurso guardar(Recurso recurso);

    boolean existeNombre(Integer idProveedor, String nombre);

    List<Recurso> listarPorProveedor(Integer idProveedor);

    List<Recurso> buscarPorIds(Collection<Integer> ids);

    List<Recurso> listarActivosPorServicio(Integer idServicio);

    /**
     * Bloquea ({@code FOR UPDATE}) los recursos activos del servicio en orden de id para evitar interbloqueos.
     */
    List<Recurso> bloquearActivosPorServicio(Integer idServicio);

    void reemplazarAsignaciones(Integer idServicio, Set<Integer> idsRecursos);
}

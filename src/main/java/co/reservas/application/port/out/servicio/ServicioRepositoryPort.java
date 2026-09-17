package co.reservas.application.port.out.servicio;

import co.reservas.domain.servicio.Servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioRepositoryPort {

    Servicio guardar(Servicio servicio);

    Optional<Servicio> buscarPorId(Integer id);

    /**
     * Busca el servicio bloqueando su fila ({@code SELECT … FOR UPDATE}).
     */
    Optional<Servicio> bloquearPorId(Integer id);

    List<Servicio> buscarPorIds(Collection<Integer> ids);

    List<Servicio> listarActivos(Integer idProveedor, Integer idCategoria);

    boolean existeNombre(Integer idProveedor, String nombre);
}

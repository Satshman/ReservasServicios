package co.reservas.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CatalogoSistemaJpaRepository extends JpaRepository<EstadoEntity, Integer> {

    @Query("select e.id as id, t.nombre as tipo, e.nombre as nombre "
            + "from EstadoEntity e join TipoEstadoEntity t on t.id = e.idTipoEstado")
    List<FilaEstado> listarEstados();

    @Query("select r from RolEntity r")
    List<RolEntity> listarRoles();

    interface FilaEstado {
        Integer getId();

        String getTipo();

        String getNombre();
    }
}

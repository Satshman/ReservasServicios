package co.reservas.application.port.out.servicio;

import co.reservas.domain.shared.ElementoCatalogo;

import java.util.List;

public interface CatalogoRepositoryPort {

    boolean existeCategoria(Integer idCategoria);

    boolean existeTipoRecurso(Integer idTipoRecurso);

    List<ElementoCatalogo> listarCategorias();

    List<ElementoCatalogo> listarTiposRecurso();

    List<ElementoCatalogo> listarTiposDocumento();
}

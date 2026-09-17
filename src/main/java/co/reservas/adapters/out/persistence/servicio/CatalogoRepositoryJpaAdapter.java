package co.reservas.adapters.out.persistence.servicio;

import co.reservas.adapters.out.persistence.recurso.TipoRecursoEntity;
import co.reservas.adapters.out.persistence.recurso.TipoRecursoJpaRepository;
import co.reservas.adapters.out.persistence.usuario.TipoDocumentoEntity;
import co.reservas.adapters.out.persistence.usuario.TipoDocumentoJpaRepository;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.domain.shared.ElementoCatalogo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoRepositoryJpaAdapter implements CatalogoRepositoryPort {

    private static final Sort POR_ID = Sort.by("id");

    private final CategoriaServicioJpaRepository categorias;
    private final TipoRecursoJpaRepository tiposRecurso;
    private final TipoDocumentoJpaRepository tiposDocumento;

    public CatalogoRepositoryJpaAdapter(CategoriaServicioJpaRepository categorias,
                                        TipoRecursoJpaRepository tiposRecurso,
                                        TipoDocumentoJpaRepository tiposDocumento) {
        this.categorias = categorias;
        this.tiposRecurso = tiposRecurso;
        this.tiposDocumento = tiposDocumento;
    }

    @Override
    public boolean existeCategoria(Integer idCategoria) {
        return idCategoria != null && categorias.existsById(idCategoria);
    }

    @Override
    public boolean existeTipoRecurso(Integer idTipoRecurso) {
        return idTipoRecurso != null && tiposRecurso.existsById(idTipoRecurso);
    }

    @Override
    public List<ElementoCatalogo> listarCategorias() {
        return categorias.findAll(POR_ID).stream()
                .map((CategoriaServicioEntity c) -> new ElementoCatalogo(c.getId(), c.getNombre()))
                .toList();
    }

    @Override
    public List<ElementoCatalogo> listarTiposRecurso() {
        return tiposRecurso.findAll(POR_ID).stream()
                .map((TipoRecursoEntity t) -> new ElementoCatalogo(t.getId(), t.getNombre()))
                .toList();
    }

    @Override
    public List<ElementoCatalogo> listarTiposDocumento() {
        return tiposDocumento.findAll(POR_ID).stream()
                .map((TipoDocumentoEntity t) -> new ElementoCatalogo(t.getId(), t.getNombre()))
                .toList();
    }
}

package co.reservas.application.service.servicio;

import co.reservas.application.port.in.servicio.ConsultarCatalogosUseCase;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.domain.shared.ElementoCatalogo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoService implements ConsultarCatalogosUseCase {

    private final CatalogoRepositoryPort catalogos;

    public CatalogoService(CatalogoRepositoryPort catalogos) {
        this.catalogos = catalogos;
    }

    @Override
    public List<ElementoCatalogo> categorias() {
        return catalogos.listarCategorias();
    }

    @Override
    public List<ElementoCatalogo> tiposRecurso() {
        return catalogos.listarTiposRecurso();
    }

    @Override
    public List<ElementoCatalogo> tiposDocumento() {
        return catalogos.listarTiposDocumento();
    }
}

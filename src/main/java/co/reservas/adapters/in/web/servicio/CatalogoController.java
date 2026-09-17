package co.reservas.adapters.in.web.servicio;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.application.port.in.servicio.ConsultarCatalogosUseCase;
import co.reservas.domain.shared.ElementoCatalogo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1 + "/catalogos")
@Tag(name = "Catálogos", description = "Catálogos de negocio configurables")
public class CatalogoController {

    private final ConsultarCatalogosUseCase catalogos;

    public CatalogoController(ConsultarCatalogosUseCase catalogos) {
        this.catalogos = catalogos;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorías de servicio")
    public List<ElementoCatalogo> categorias() {
        return catalogos.categorias();
    }

    @GetMapping("/tipos-recurso")
    @Operation(summary = "Tipos de recurso")
    public List<ElementoCatalogo> tiposRecurso() {
        return catalogos.tiposRecurso();
    }

    @GetMapping("/tipos-documento")
    @Operation(summary = "Tipos de documento")
    public List<ElementoCatalogo> tiposDocumento() {
        return catalogos.tiposDocumento();
    }
}

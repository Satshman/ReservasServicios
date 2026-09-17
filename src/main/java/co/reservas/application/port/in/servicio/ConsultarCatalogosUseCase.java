package co.reservas.application.port.in.servicio;

import co.reservas.domain.shared.ElementoCatalogo;

import java.util.List;

public interface ConsultarCatalogosUseCase {

    List<ElementoCatalogo> categorias();

    List<ElementoCatalogo> tiposRecurso();

    List<ElementoCatalogo> tiposDocumento();
}

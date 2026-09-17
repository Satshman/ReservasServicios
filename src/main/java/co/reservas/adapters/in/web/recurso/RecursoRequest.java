package co.reservas.adapters.in.web.recurso;

import co.reservas.application.port.in.recurso.CrearRecursoComando;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecursoRequest(
        @Schema(example = "Consultorio 1") @NotBlank @Size(max = 100) String nombre,
        @Schema(example = "1", description = "Id de GET /api/v1/catalogos/tipos-recurso") @NotNull @Positive
        Integer idTipoRecurso) {

    public CrearRecursoComando aComando() {
        return new CrearRecursoComando(nombre, idTipoRecurso);
    }
}

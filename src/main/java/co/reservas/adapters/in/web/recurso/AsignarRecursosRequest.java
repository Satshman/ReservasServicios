package co.reservas.adapters.in.web.recurso;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record AsignarRecursosRequest(
        @ArraySchema(arraySchema = @Schema(example = "[1]",
                description = "Recursos que ocupa todo turno del servicio. Una lista vacía quita las asignaciones."))
        @NotNull Set<@NotNull @Positive Integer> idsRecursos) {
}

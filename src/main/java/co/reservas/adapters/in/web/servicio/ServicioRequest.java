package co.reservas.adapters.in.web.servicio;

import co.reservas.application.port.in.servicio.NuevoServicioComando;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServicioRequest(
        @Schema(example = "Limpieza dental") @NotBlank @Size(max = 100) String nombre,
        @Schema(example = "Profilaxis y control general") @Size(max = 1000) String descripcion,
        @Schema(example = "1", description = "Id de GET /api/v1/catalogos/categorias") @NotNull @Positive
        Integer idCategoria,
        @Schema(example = "30", minimum = "5", maximum = "480") @NotNull @Min(5) @Max(480) Integer duracionMinutos,
        @Schema(example = "1", description = "Reservas simultáneas por turno (por defecto 1)") @Min(1)
        @Max(Short.MAX_VALUE) Integer capacidad) {

    public NuevoServicioComando aComando() {
        return new NuevoServicioComando(nombre, descripcion, idCategoria, duracionMinutos, capacidad);
    }
}

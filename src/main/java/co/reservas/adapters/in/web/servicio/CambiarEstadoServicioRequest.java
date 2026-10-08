package co.reservas.adapters.in.web.servicio;

import co.reservas.domain.servicio.EstadoServicio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoServicioRequest(
        @Schema(example = "INACTIVO", description = "Un servicio INACTIVO no acepta reservas nuevas; las confirmadas "
                + "se mantienen.")
        @NotNull EstadoServicio estado) {
}

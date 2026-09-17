package co.reservas.adapters.in.web.reserva;

import co.reservas.application.port.in.reserva.CrearReservaComando;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record ReservaRequest(
        @Schema(example = "1") @NotNull @Positive Integer idServicio,
        @Schema(type = "string", format = "date-time", example = "2026-09-21T08:30:00-05:00",
                description = "ISO-8601 con offset; debe coincidir con un turno disponible")
        @NotNull OffsetDateTime fechaHoraInicio) {

    public CrearReservaComando aComando() {
        return new CrearReservaComando(idServicio, fechaHoraInicio);
    }
}

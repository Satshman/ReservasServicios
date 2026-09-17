package co.reservas.adapters.in.web.agenda;

import co.reservas.application.port.in.agenda.CrearHorarioComando;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;

public record CrearHorarioRequest(
        @ArraySchema(schema = @Schema(example = "1", description = "Día ISO: 1 = lunes … 7 = domingo"),
                arraySchema = @Schema(example = "[1, 2, 3, 4, 5]"))
        @NotEmpty Set<@NotNull @Min(1) @Max(7) Integer> diasSemana,
        @Schema(type = "string", example = "08:00") @NotNull LocalTime horaInicio,
        @Schema(type = "string", example = "12:00") @NotNull LocalTime horaFin) {

    public CrearHorarioComando aComando() {
        return new CrearHorarioComando(diasSemana, horaInicio, horaFin);
    }
}

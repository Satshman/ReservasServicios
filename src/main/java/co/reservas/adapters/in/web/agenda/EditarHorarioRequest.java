package co.reservas.adapters.in.web.agenda;

import co.reservas.application.port.in.agenda.EditarHorarioComando;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record EditarHorarioRequest(
        @Schema(example = "1", description = "Día ISO: 1 = lunes … 7 = domingo") @NotNull @Min(1) @Max(7)
        Integer diaSemana,
        @Schema(type = "string", example = "09:00") @NotNull LocalTime horaInicio,
        @Schema(type = "string", example = "11:00") @NotNull LocalTime horaFin) {

    public EditarHorarioComando aComando() {
        return new EditarHorarioComando(diaSemana, horaInicio, horaFin);
    }
}

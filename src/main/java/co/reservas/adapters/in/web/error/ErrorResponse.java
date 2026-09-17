package co.reservas.adapters.in.web.error;

import co.reservas.domain.shared.CodigoError;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Cuerpo uniforme de error")
public record ErrorResponse(
        @Schema(example = "VALIDACION_FALLIDA") CodigoError errorCode,
        @Schema(example = "La solicitud contiene datos inválidos.") String message,
        List<Object> details,
        @Schema(example = "3f2a9c1e4b7d4e8f") String traceId,
        Instant timestamp) {
}

package co.reservas.adapters.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "contacto@sonrisafeliz.co") @NotBlank @Size(max = 255) String email,
        @Schema(example = "clave-muy-segura-2026") @NotBlank @Size(max = 128) String password) {
}

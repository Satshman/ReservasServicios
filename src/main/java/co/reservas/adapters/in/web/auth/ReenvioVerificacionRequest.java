package co.reservas.adapters.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReenvioVerificacionRequest(
        @Schema(example = "contacto@sonrisafeliz.co") @NotBlank @Email @Size(max = 255) String email) {

    public ReenvioVerificacionRequest {
        email = email == null ? null : email.trim();
    }
}

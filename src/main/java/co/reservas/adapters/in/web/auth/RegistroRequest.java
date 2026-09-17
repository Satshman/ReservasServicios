package co.reservas.adapters.in.web.auth;

import co.reservas.adapters.in.web.servicio.ServicioRequest;
import co.reservas.application.port.in.auth.RegistrarUsuarioComando;
import co.reservas.domain.usuario.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Registro de cliente o proveedor. Un proveedor debe incluir al menos un servicio.")
public record RegistroRequest(
        @Schema(example = "Clínica Sonrisa Feliz") @NotBlank @Size(min = 2, max = 100) String nombreCompleto,
        @Schema(example = "contacto@sonrisafeliz.co") @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "clave-muy-segura-2026", minLength = 12, maxLength = 64)
        @NotNull @Size(min = 12, max = 64) String password,
        @Schema(example = "PROVEEDOR", allowableValues = {"CLIENTE", "PROVEEDOR"})
        @NotBlank @Pattern(regexp = "CLIENTE|PROVEEDOR", message = "debe ser CLIENTE o PROVEEDOR") String rol,
        @Schema(example = "3001234567") @Size(max = 20) String telefono,
        @Schema(example = "Sonrisa Feliz", description = "Solo proveedor. Por defecto, el nombre completo")
        @Size(max = 100) String nombreComercial,
        @Schema(example = "America/Bogota", description = "Solo proveedor. ID de zona IANA")
        @Size(max = 50) String zonaHoraria,
        @Schema(description = "Solo proveedor: servicios que ofrece") @Valid List<@NotNull ServicioRequest> servicios) {

    public RegistroRequest {
        email = email == null ? null : email.trim();
    }

    public RegistrarUsuarioComando aComando() {
        return new RegistrarUsuarioComando(nombreCompleto, email, password, Rol.valueOf(rol), telefono,
                nombreComercial, zonaHoraria,
                servicios == null ? List.of() : servicios.stream().map(ServicioRequest::aComando).toList());
    }
}

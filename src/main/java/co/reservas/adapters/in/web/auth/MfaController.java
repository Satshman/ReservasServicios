package co.reservas.adapters.in.web.auth;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.application.port.in.auth.MfaAdminUseCase;
import co.reservas.application.port.in.auth.SesionResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesión de administradores con segundo factor. {@code POST /auth/login} rechaza las cuentas ADMIN con
 * {@code 403 ACCESO_DENEGADO}; deben usar estos dos pasos.
 */
@RestController
@RequestMapping(ApiRutas.V1 + "/auth/mfa")
@Tag(name = "Autenticación", description = "Registro, verificación de cuenta, inicio de sesión, rotación y revocación de tokens")
public class MfaController {

    private final MfaAdminUseCase mfa;

    public MfaController(MfaAdminUseCase mfa) {
        this.mfa = mfa;
    }

    @PostMapping("/solicitud")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Paso 1 del inicio de sesión de administrador: validar contraseña y enviar código MFA",
            description = "Envía por correo un código de 6 dígitos, vigente 5 minutos y de un solo uso. Los errores de "
                    + "contraseña cuentan para el bloqueo de la cuenta. Errores: 401 CREDENCIALES_INVALIDAS, "
                    + "403 ACCESO_DENEGADO (la cuenta no es de administrador), 423 CUENTA_BLOQUEADA.")
    public MensajeResponse solicitar(@Valid @RequestBody SolicitudMfaRequest solicitud) {
        mfa.solicitarCodigo(solicitud.email(), solicitud.password());
        return new MensajeResponse("Enviamos un código de verificación a tu correo. Vence en 5 minutos.");
    }

    @PostMapping("/verificacion")
    @Operation(summary = "Paso 2 del inicio de sesión de administrador: verificar el código y emitir los tokens",
            description = "Devuelve accessToken y refreshToken con rol ADMIN. Un código incorrecto, vencido o ya usado "
                    + "cuenta para el bloqueo de la cuenta. Errores: 400 VALIDACION_FALLIDA, "
                    + "401 CREDENCIALES_INVALIDAS, 423 CUENTA_BLOQUEADA.")
    public SesionResultado verificar(@Valid @RequestBody VerificacionMfaRequest solicitud) {
        return mfa.verificarCodigo(solicitud.email(), solicitud.codigo());
    }

    public record SolicitudMfaRequest(
            @Schema(example = "admin@reservas.co") @NotBlank @Size(max = 255) String email,
            @Schema(example = "clave-muy-segura-2026") @NotBlank @Size(max = 128) String password) {
    }

    public record VerificacionMfaRequest(
            @Schema(example = "admin@reservas.co") @NotBlank @Size(max = 255) String email,
            @Schema(example = "123456") @NotBlank
            @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos") String codigo) {
    }
}

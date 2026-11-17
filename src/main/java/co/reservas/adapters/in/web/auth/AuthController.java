package co.reservas.adapters.in.web.auth;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.application.port.in.auth.GestionarSesionTokenUseCase;
import co.reservas.application.port.in.auth.IniciarSesionUseCase;
import co.reservas.application.port.in.auth.ReenviarVerificacionUseCase;
import co.reservas.application.port.in.auth.RegistrarUsuarioUseCase;
import co.reservas.application.port.in.auth.RegistroResultado;
import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.in.auth.VerificarCuentaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRutas.V1 + "/auth")
@Tag(name = "Autenticación", description = "Registro, verificación de cuenta, inicio de sesión, rotación y revocación de tokens")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuario;
    private final VerificarCuentaUseCase verificarCuenta;
    private final ReenviarVerificacionUseCase reenviarVerificacion;
    private final IniciarSesionUseCase iniciarSesion;
    private final GestionarSesionTokenUseCase sesionToken;

    public AuthController(RegistrarUsuarioUseCase registrarUsuario, VerificarCuentaUseCase verificarCuenta,
                          ReenviarVerificacionUseCase reenviarVerificacion, IniciarSesionUseCase iniciarSesion) {
        this(registrarUsuario, verificarCuenta, reenviarVerificacion, iniciarSesion, null);
    }

    @Autowired
    public AuthController(RegistrarUsuarioUseCase registrarUsuario, VerificarCuentaUseCase verificarCuenta,
                          ReenviarVerificacionUseCase reenviarVerificacion, IniciarSesionUseCase iniciarSesion,
                          GestionarSesionTokenUseCase sesionToken) {
        this.registrarUsuario = registrarUsuario;
        this.verificarCuenta = verificarCuenta;
        this.reenviarVerificacion = reenviarVerificacion;
        this.iniciarSesion = iniciarSesion;
        this.sesionToken = sesionToken;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cliente o proveedor",
            description = "Errores: 400 VALIDACION_FALLIDA, 400 SERVICIO_REQUERIDO, 404 CATEGORIA_NO_ENCONTRADA, "
                    + "409 EMAIL_YA_REGISTRADO.")
    public RegistroResultado registrar(@Valid @RequestBody RegistroRequest solicitud) {
        return registrarUsuario.registrar(solicitud.aComando());
    }

    @GetMapping("/verificacion")
    @Operation(summary = "Verificar cuenta con el token recibido por correo",
            description = "Errores: 400 TOKEN_VERIFICACION_INVALIDO.")
    public MensajeResponse verificar(@RequestParam String token) {
        verificarCuenta.verificar(token);
        return new MensajeResponse("Cuenta verificada. Ya puedes iniciar sesión.");
    }

    @PostMapping("/verificacion/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Reenviar correo de verificación",
            description = "Responde 202 exista o no el correo, para no revelar cuentas registradas.")
    public MensajeResponse reenviar(@Valid @RequestBody ReenvioVerificacionRequest solicitud) {
        reenviarVerificacion.reenviar(solicitud.email());
        return new MensajeResponse(
                "Si el correo corresponde a una cuenta pendiente de verificación, recibirás un nuevo enlace.");
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Errores: 401 CREDENCIALES_INVALIDAS, 403 CUENTA_NO_VERIFICADA, 403 CUENTA_INACTIVA, "
                    + "423 CUENTA_BLOQUEADA.")
    public SesionResultado login(@Valid @RequestBody LoginRequest solicitud) {
        return iniciarSesion.iniciarSesion(solicitud.email(), solicitud.password());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotar token de acceso mediante refresh token",
            description = "Invalida el refresh token recibido y emite un nuevo par accessToken + refreshToken. "
                    + "Errores: 401 CREDENCIALES_INVALIDAS.")
    public SesionResultado refresh(@Valid @RequestBody RefreshTokenRequest solicitud) {
        return sesionToken.rotar(solicitud.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar refresh token")
    public MensajeResponse logout(@Valid @RequestBody RefreshTokenRequest solicitud) {
        sesionToken.revocar(solicitud.refreshToken());
        return new MensajeResponse("Sesión cerrada correctamente.");
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "El token de refresco es obligatorio") String refreshToken
    ) {
    }
}
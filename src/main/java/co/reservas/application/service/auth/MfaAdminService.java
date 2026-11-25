package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.MfaAdminUseCase;
import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.CodigoMfaRepositoryPort;
import co.reservas.application.port.out.auth.NotificacionPort;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.CodigoMfa;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.PoliticaBloqueo;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Inicio de sesión de administradores en dos pasos: contraseña y código de un solo uso enviado por correo.
 *
 * <p>Los intentos fallidos de contraseña y de código suman al mismo contador que usa el inicio de sesión normal
 * ({@link PoliticaBloqueo}): cinco errores bloquean la cuenta, así que el código de 6 dígitos no se puede adivinar
 * por fuerza bruta. El contador solo se reinicia al completar los dos pasos.
 */
@Service
public class MfaAdminService implements MfaAdminUseCase {

    private static final Logger log = LoggerFactory.getLogger(MfaAdminService.class);
    private static final Duration VIGENCIA_MFA = Duration.ofMinutes(5);
    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort passwordHasher;
    private final CodigoMfaRepositoryPort codigosMfa;
    private final NotificacionPort notificaciones;
    private final TokenEmisorPort tokenEmisor;
    private final SesionTokenService sesionTokenService;
    private final PoliticaBloqueo politicaBloqueo;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String hashFicticio;

    @SuppressWarnings("java:S107")
    public MfaAdminService(UsuarioRepositoryPort usuarios,
                           PasswordHasherPort passwordHasher,
                           CodigoMfaRepositoryPort codigosMfa,
                           NotificacionPort notificaciones,
                           TokenEmisorPort tokenEmisor,
                           SesionTokenService sesionTokenService,
                           PoliticaBloqueo politicaBloqueo,
                           Clock clock) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.codigosMfa = codigosMfa;
        this.notificaciones = notificaciones;
        this.tokenEmisor = tokenEmisor;
        this.sesionTokenService = sesionTokenService;
        this.politicaBloqueo = politicaBloqueo;
        this.clock = clock;
        this.hashFicticio = passwordHasher.hashear("contrasena-ficticia-tiempo-constante");
    }

    /**
     * Los intentos fallidos se confirman aunque la respuesta sea un error.
     */
    @Override
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public void solicitarCodigo(String email, String password) {
        Instant ahora = Instant.now(clock);
        Optional<Usuario> encontrado = usuarios.buscarPorEmailParaActualizar(Usuario.normalizarEmail(email));
        if (encontrado.isEmpty()) {
            // Hash ficticio: la respuesta tarda lo mismo exista o no el correo (no revela cuentas).
            passwordHasher.coincide(password, hashFicticio);
            throw credencialesInvalidas();
        }
        Usuario usuario = encontrado.get();
        boolean passwordCorrecta = passwordHasher.coincide(password, usuario.passwordHash());
        if (usuario.estaBloqueado(ahora)) {
            registrarEvento("MFA_FALLIDO", usuario.id());
            throw passwordCorrecta ? cuentaBloqueada() : credencialesInvalidas();
        }
        if (!passwordCorrecta) {
            registrarIntentoFallido(usuario, ahora);
            throw credencialesInvalidas();
        }
        if (usuario.rol() != Rol.ADMIN) {
            throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO,
                    "El desafío MFA está reservado para cuentas de administrador.");
        }
        if (usuario.estado() != EstadoUsuario.ACTIVO) {
            throw credencialesInvalidas();
        }

        String codigoClaro = String.format("%06d", secureRandom.nextInt(1_000_000));
        // Solo vale el último código: pedir varios no da más probabilidades de acertar antes del bloqueo.
        codigosMfa.invalidarActivos(usuario.id(), ahora);
        codigosMfa.guardar(CodigoMfa.nuevo(usuario.id(), codigoClaro, ahora.plus(VIGENCIA_MFA), ahora));
        notificaciones.enviarCodigoMfa(usuario.email(), usuario.nombreCompleto(), codigoClaro);
        registrarEvento("MFA_SOLICITADO", usuario.id());
    }

    @Override
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public SesionResultado verificarCodigo(String email, String codigo) {
        Instant ahora = Instant.now(clock);
        Usuario usuario = usuarios.buscarPorEmailParaActualizar(Usuario.normalizarEmail(email))
                .filter(encontrado -> encontrado.rol() == Rol.ADMIN && encontrado.estado() == EstadoUsuario.ACTIVO)
                .orElseThrow(MfaAdminService::credencialesInvalidas);
        if (usuario.estaBloqueado(ahora)) {
            registrarEvento("MFA_FALLIDO", usuario.id());
            throw cuentaBloqueada();
        }
        CodigoMfa desafio = codigo != null && codigo.matches("\\d{6}")
                ? codigosMfa.buscarUltimoActivo(usuario.id(), TokenVerificacion.calcularHash(codigo))
                        .filter(c -> c.esValido(ahora))
                        .orElse(null)
                : null;
        if (desafio == null) {
            registrarIntentoFallido(usuario, ahora);
            throw credencialesInvalidas();
        }

        codigosMfa.guardar(desafio.marcarUsado(ahora));
        if (usuario.requiereReinicioDeIntentos()) {
            usuario = usuarios.guardar(usuario.conIntentosReiniciados());
        }
        TokenEmisorPort.TokenAcceso access = tokenEmisor.emitir(usuario);
        String refresh = sesionTokenService.emitirTokenRefresco(usuario.id(), ahora);
        registrarEvento("MFA_VERIFICADO", usuario.id());

        return new SesionResultado(access.valor(), TIPO_TOKEN, access.expiraEnSegundos(),
                usuario.rol(), usuario.rol().rutaPanel(), refresh);
    }

    private void registrarIntentoFallido(Usuario usuario, Instant ahora) {
        Usuario actualizado = usuarios.guardar(politicaBloqueo.registrarIntentoFallido(usuario, ahora));
        registrarEvento("MFA_FALLIDO", usuario.id());
        if (actualizado.estaBloqueado(ahora)) {
            registrarEvento("CUENTA_BLOQUEADA", usuario.id());
        }
    }

    private static void registrarEvento(String evento, Integer idUsuario) {
        log.atInfo()
                .addKeyValue("evento", evento)
                .addKeyValue("idUsuario", idUsuario)
                .log("Evento de seguridad");
    }

    private static ExcepcionNegocio cuentaBloqueada() {
        return new ExcepcionNegocio(CodigoError.CUENTA_BLOQUEADA,
                "La cuenta está bloqueada temporalmente por intentos fallidos. Intenta más tarde.");
    }

    private static ExcepcionNegocio credencialesInvalidas() {
        return new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS,
                "Credenciales o código de verificación MFA inválidos.");
    }
}

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
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaAdminService(UsuarioRepositoryPort usuarios,
                           PasswordHasherPort passwordHasher,
                           CodigoMfaRepositoryPort codigosMfa,
                           NotificacionPort notificaciones,
                           TokenEmisorPort tokenEmisor,
                           SesionTokenService sesionTokenService,
                           Clock clock) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.codigosMfa = codigosMfa;
        this.notificaciones = notificaciones;
        this.tokenEmisor = tokenEmisor;
        this.sesionTokenService = sesionTokenService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void solicitarCodigo(String email, String password) {
        Instant ahora = Instant.now(clock);
        Usuario usuario = usuarios.buscarPorEmailParaActualizar(Usuario.normalizarEmail(email))
                .orElseThrow(MfaAdminService::credencialesInvalidas);
        if (!passwordHasher.coincide(password, usuario.passwordHash())) {
            throw credencialesInvalidas();
        }
        if (usuario.rol() != Rol.ADMIN) {
            throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO,
                    "El desafío MFA está reservado para cuentas de administrador.");
        }
        if (usuario.estado() != EstadoUsuario.ACTIVO || usuario.estaBloqueado(ahora)) {
            throw credencialesInvalidas();
        }

        String codigoClaro = String.format("%06d", secureRandom.nextInt(1_000_000));
        codigosMfa.guardar(CodigoMfa.nuevo(usuario.id(), codigoClaro, ahora.plus(VIGENCIA_MFA), ahora));
        notificaciones.enviarCodigoMfa(usuario.email(), usuario.nombreCompleto(), codigoClaro);

        log.atInfo()
                .addKeyValue("evento", "MFA_SOLICITADO")
                .addKeyValue("idUsuario", usuario.id())
                .log("Código MFA generado para administrador");
    }

    @Override
    @Transactional
    public SesionResultado verificarCodigo(String email, String codigo) {
        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw credencialesInvalidas();
        }
        Instant ahora = Instant.now(clock);
        Usuario usuario = usuarios.buscarPorEmailParaActualizar(Usuario.normalizarEmail(email))
                .orElseThrow(MfaAdminService::credencialesInvalidas);
        if (usuario.rol() != Rol.ADMIN || usuario.estado() != EstadoUsuario.ACTIVO) {
            throw credencialesInvalidas();
        }

        String hash = TokenVerificacion.calcularHash(codigo);
        CodigoMfa desafío = codigosMfa.buscarUltimoActivo(usuario.id(), hash)
                .filter(c -> c.esValido(ahora))
                .orElseThrow(MfaAdminService::credencialesInvalidas);

        codigosMfa.guardar(desafío.marcarUsado(ahora));
        TokenEmisorPort.TokenAcceso access = tokenEmisor.emitir(usuario);
        String refresh = sesionTokenService.emitirTokenRefresco(usuario.id(), ahora);

        log.atInfo()
                .addKeyValue("evento", "MFA_VERIFICADO")
                .addKeyValue("idUsuario", usuario.id())
                .log("Administrador autenticado con segundo factor MFA");

        return new SesionResultado(access.valor(), TIPO_TOKEN, access.expiraEnSegundos(),
                usuario.rol(), usuario.rol().rutaPanel(), refresh);
    }

    private static ExcepcionNegocio credencialesInvalidas() {
        return new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS,
                "Credenciales o código de verificación MFA inválidos.");
    }
}
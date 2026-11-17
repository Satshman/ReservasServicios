package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.IniciarSesionUseCase;
import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.PoliticaBloqueo;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class IniciarSesionService implements IniciarSesionUseCase {

    private static final Logger log = LoggerFactory.getLogger(IniciarSesionService.class);
    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort passwordHasher;
    private final TokenEmisorPort tokenEmisor;
    private final PoliticaBloqueo politicaBloqueo;
    private final Clock clock;
    private final String hashFicticio;

    public IniciarSesionService(UsuarioRepositoryPort usuarios, PasswordHasherPort passwordHasher,
                                TokenEmisorPort tokenEmisor, PoliticaBloqueo politicaBloqueo, Clock clock) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.tokenEmisor = tokenEmisor;
        this.politicaBloqueo = politicaBloqueo;
        this.clock = clock;
        this.hashFicticio = passwordHasher.hashear("contrasena-ficticia-tiempo-constante");
    }

    /**
     * Los intentos fallidos y el bloqueo se confirman aunque la respuesta sea un error.
     */
    @Override
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public SesionResultado iniciarSesion(String email, String password) {
        Instant ahora = Instant.now(clock);
        Optional<Usuario> encontrado = usuarios.buscarPorEmailParaActualizar(Usuario.normalizarEmail(email));
        if (encontrado.isEmpty()) {
            passwordHasher.coincide(password, hashFicticio);
            registrarEvento("LOGIN_FALLIDO", null);
            throw credencialesInvalidas();
        }
        Usuario usuario = encontrado.get();
        boolean passwordCorrecta = passwordHasher.coincide(password, usuario.passwordHash());

        if (usuario.estaBloqueado(ahora)) {
            registrarEvento("LOGIN_FALLIDO", usuario.id());
            if (passwordCorrecta) {
                throw new ExcepcionNegocio(CodigoError.CUENTA_BLOQUEADA,
                        "La cuenta está bloqueada temporalmente por intentos fallidos. Intenta más tarde.");
            }
            throw credencialesInvalidas();
        }
        if (!passwordCorrecta) {
            Usuario actualizado = usuarios.guardar(politicaBloqueo.registrarIntentoFallido(usuario, ahora));
            registrarEvento("LOGIN_FALLIDO", usuario.id());
            if (actualizado.estaBloqueado(ahora)) {
                registrarEvento("CUENTA_BLOQUEADA", usuario.id());
            }
            throw credencialesInvalidas();
        }
        if (usuario.requiereReinicioDeIntentos()) {
            usuario = usuarios.guardar(usuario.conIntentosReiniciados());
        }
        validarEstado(usuario);

        TokenEmisorPort.TokenAcceso token = tokenEmisor.emitir(usuario);
        registrarEvento("LOGIN_EXITOSO", usuario.id());
        return new SesionResultado(token.valor(), TIPO_TOKEN, token.expiraEnSegundos(), usuario.rol(),
                usuario.rol().rutaPanel());
    }

    private static void validarEstado(Usuario usuario) {
        if (usuario.estado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw new ExcepcionNegocio(CodigoError.CUENTA_NO_VERIFICADA,
                    "La cuenta aún no ha sido verificada. Revisa tu correo electrónico.");
        }
        if (usuario.estado() == EstadoUsuario.INACTIVO) {
            throw new ExcepcionNegocio(CodigoError.CUENTA_INACTIVA, "La cuenta está inactiva.");
        }
    }

    private static ExcepcionNegocio credencialesInvalidas() {
        return new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "Correo electrónico o contraseña incorrectos.");
    }

    private static void registrarEvento(String evento, Integer idUsuario) {
        log.atInfo()
                .addKeyValue("evento", evento)
                .addKeyValue("idUsuario", idUsuario)
                .log("Evento de seguridad");
    }
}

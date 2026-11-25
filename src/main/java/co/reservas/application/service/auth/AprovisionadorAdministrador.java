package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.AprovisionarAdministradorUseCase;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Crea la primera cuenta de administrador a partir de la configuración del despliegue. El registro público nunca
 * crea administradores, y la contraseña no se guarda en migraciones ni en el repositorio.
 *
 * <p>Es idempotente: si ya existe una cuenta con ese correo no la modifica, así que reiniciar la aplicación con las
 * mismas variables no cambia nada.
 */
@Service
public class AprovisionadorAdministrador implements AprovisionarAdministradorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AprovisionadorAdministrador.class);
    static final int PASSWORD_MINIMO = 12;
    static final int PASSWORD_MAXIMO = 64;
    static final String NOMBRE_POR_DEFECTO = "Administrador";

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    public AprovisionadorAdministrador(UsuarioRepositoryPort usuarios, PasswordHasherPort passwordHasher,
                                       Clock clock) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean aprovisionar(String email, String nombreCompleto, String password) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (emailNormalizado.isBlank() || !emailNormalizado.contains("@")) {
            throw new IllegalStateException("APP_ADMIN_EMAIL debe ser un correo electrónico válido");
        }
        if (password == null || password.length() < PASSWORD_MINIMO || password.length() > PASSWORD_MAXIMO) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD debe tener entre 12 y 64 caracteres");
        }
        if (usuarios.existeEmail(emailNormalizado)) {
            log.atInfo().addKeyValue("evento", "ADMIN_YA_EXISTE").log("La cuenta de administrador ya existe");
            return false;
        }
        String nombre = nombreCompleto == null || nombreCompleto.isBlank() ? NOMBRE_POR_DEFECTO : nombreCompleto.trim();
        Instant ahora = Instant.now(clock);
        Usuario admin = usuarios.guardar(new Usuario(null, Rol.ADMIN, EstadoUsuario.ACTIVO, null, null, nombre,
                emailNormalizado, null, passwordHasher.hashear(password), 0, null, ahora));
        log.atInfo()
                .addKeyValue("evento", "ADMIN_APROVISIONADO")
                .addKeyValue("idUsuario", admin.id())
                .log("Cuenta de administrador creada desde la configuración");
        return true;
    }
}

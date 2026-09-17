package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.RegistrarUsuarioComando;
import co.reservas.application.port.in.auth.RegistrarUsuarioUseCase;
import co.reservas.application.port.in.auth.RegistroResultado;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.RegistroServicios;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    static final int PASSWORD_MINIMO = 12;
    static final int PASSWORD_MAXIMO = 64;
    private static final Logger log = LoggerFactory.getLogger(RegistrarUsuarioService.class);

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort passwordHasher;
    private final RegistroServicios registroServicios;
    private final SolicitadorVerificacion solicitadorVerificacion;
    private final Clock clock;

    public RegistrarUsuarioService(UsuarioRepositoryPort usuarios, PasswordHasherPort passwordHasher,
                                   RegistroServicios registroServicios,
                                   SolicitadorVerificacion solicitadorVerificacion, Clock clock) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.registroServicios = registroServicios;
        this.solicitadorVerificacion = solicitadorVerificacion;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegistroResultado registrar(RegistrarUsuarioComando comando) {
        validarForma(comando);
        String email = Usuario.normalizarEmail(comando.email());
        if (usuarios.existeEmail(email)) {
            throw new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO,
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }
        Instant ahora = Instant.now(clock);
        Usuario usuario = usuarios.guardar(Usuario.registrar(comando.rol(), comando.nombreCompleto(), email,
                comando.telefono(), passwordHasher.hashear(comando.password()), ahora));

        if (comando.rol() == Rol.CLIENTE) {
            usuarios.guardarCliente(new Cliente(null, usuario.id()));
        } else {
            Proveedor proveedor = usuarios.guardarProveedor(Proveedor.nuevo(usuario.id(), comando.nombreComercial(),
                    usuario.nombreCompleto(), comando.zonaHoraria()));
            registroServicios.guardar(proveedor.id(), comando.servicios());
        }
        solicitadorVerificacion.solicitar(usuario, ahora);

        log.atInfo()
                .addKeyValue("evento", "USUARIO_REGISTRADO")
                .addKeyValue("idUsuario", usuario.id())
                .addKeyValue("rol", usuario.rol())
                .log("Usuario registrado");
        return new RegistroResultado(usuario.id(), usuario.email(), usuario.rol(), usuario.estado(),
                "Registro exitoso. Revisa tu correo electrónico para verificar la cuenta.");
    }

    private void validarForma(RegistrarUsuarioComando comando) {
        if (comando.rol() == null || comando.rol() == Rol.ADMIN) {
            throw ExcepcionNegocio.validacion("rol", "Debe ser CLIENTE o PROVEEDOR");
        }
        String password = comando.password();
        if (password == null || password.length() < PASSWORD_MINIMO || password.length() > PASSWORD_MAXIMO) {
            throw ExcepcionNegocio.validacion("password", "Debe tener entre 12 y 64 caracteres");
        }
        if (comando.rol() == Rol.CLIENTE) {
            if (!comando.servicios().isEmpty()) {
                throw ExcepcionNegocio.validacion("servicios", "Un cliente no puede registrar servicios");
            }
            return;
        }
        if (comando.servicios().isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_REQUERIDO,
                    "Un proveedor debe registrar al menos un servicio.");
        }
        Proveedor.resolverZona(comando.zonaHoraria());
        registroServicios.validar(comando.servicios(), indice -> "servicios[" + indice + "].");
    }
}

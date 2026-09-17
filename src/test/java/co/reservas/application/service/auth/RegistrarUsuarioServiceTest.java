package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.RegistrarUsuarioComando;
import co.reservas.application.port.in.auth.RegistroResultado;
import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.RegistroServicios;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final String PASSWORD = "clave-suficientemente-larga";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private RegistroServicios registroServicios;
    @Mock
    private SolicitadorVerificacion solicitador;

    private RegistrarUsuarioService servicio;

    @BeforeEach
    void configurar() {
        servicio = new RegistrarUsuarioService(usuarios, hasher, registroServicios, solicitador,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static RegistrarUsuarioComando comando(Rol rol, List<NuevoServicioComando> servicios) {
        return new RegistrarUsuarioComando("Ana Gómez", "Ana@Prueba.co", PASSWORD, rol, null, null, null, servicios);
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un proveedor con servicios, cuando se registra, entonces guarda usuario, proveedor, servicios y solicita verificación")
    void registraProveedor() {
        // Arrange
        NuevoServicioComando nuevoServicio = new NuevoServicioComando("Consulta", null, 1, 30, 1);
        when(hasher.hashear(PASSWORD)).thenReturn("hash");
        when(usuarios.guardar(any())).thenAnswer(invocacion -> {
            Usuario u = invocacion.getArgument(0);
            return new Usuario(5, u.rol(), u.estado(), null, null, u.nombreCompleto(), u.email(), null,
                    u.passwordHash(), 0, null, u.creadoEn());
        });
        when(usuarios.guardarProveedor(any())).thenAnswer(invocacion -> {
            Proveedor p = invocacion.getArgument(0);
            return new Proveedor(9, p.idUsuario(), p.nombreComercial(), p.zonaHoraria());
        });

        // Act
        RegistroResultado resultado = servicio.registrar(comando(Rol.PROVEEDOR, List.of(nuevoServicio)));

        // Assert
        assertThat(resultado.id()).isEqualTo(5);
        assertThat(resultado.email()).isEqualTo("ana@prueba.co");
        assertThat(resultado.estado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        verify(registroServicios).guardar(9, List.of(nuevoServicio));
        verify(solicitador).solicitar(any(Usuario.class), eq(AHORA));
        verify(usuarios, never()).guardarCliente(any());
    }

    @Test
    @DisplayName("Dado un correo existente, cuando se registra, entonces responde EMAIL_YA_REGISTRADO sin guardar nada")
    void correoExistente() {
        // Arrange
        when(usuarios.existeEmail("ana@prueba.co")).thenReturn(true);

        // Act - Assert
        assertCodigo(() -> servicio.registrar(comando(Rol.CLIENTE, List.of())), CodigoError.EMAIL_YA_REGISTRADO);
        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(solicitador);
    }

    @Test
    @DisplayName("Dado un proveedor sin servicios, cuando se registra, entonces responde SERVICIO_REQUERIDO")
    void proveedorSinServicios() {
        // Arrange - Act - Assert
        assertCodigo(() -> servicio.registrar(comando(Rol.PROVEEDOR, List.of())), CodigoError.SERVICIO_REQUERIDO);
        verifyNoInteractions(usuarios);
    }

    @Test
    @DisplayName("Dado un cliente con servicios, un rol ADMIN o una contraseña corta, cuando se registra, entonces responde VALIDACION_FALLIDA")
    void validacionesDeForma() {
        // Arrange
        NuevoServicioComando nuevoServicio = new NuevoServicioComando("Consulta", null, 1, 30, 1);
        RegistrarUsuarioComando claveCorta = new RegistrarUsuarioComando("Ana", "a@b.co", "corta", Rol.CLIENTE, null,
                null, null, null);

        // Act - Assert
        assertCodigo(() -> servicio.registrar(comando(Rol.CLIENTE, List.of(nuevoServicio))),
                CodigoError.VALIDACION_FALLIDA);
        assertCodigo(() -> servicio.registrar(comando(Rol.ADMIN, List.of())), CodigoError.VALIDACION_FALLIDA);
        assertCodigo(() -> servicio.registrar(claveCorta), CodigoError.VALIDACION_FALLIDA);
        verifyNoInteractions(usuarios);
    }

    @Test
    @DisplayName("Dado un cliente válido, cuando se registra, entonces crea su perfil de cliente")
    void registraCliente() {
        // Arrange
        when(hasher.hashear(PASSWORD)).thenReturn("hash");
        when(usuarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        RegistroResultado resultado = servicio.registrar(comando(Rol.CLIENTE, List.of()));

        // Assert
        assertThat(resultado.rol()).isEqualTo(Rol.CLIENTE);
        verify(usuarios).guardarCliente(any());
        verify(registroServicios, never()).guardar(anyInt(), any());
    }
}

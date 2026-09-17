package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.PoliticaBloqueo;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IniciarSesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final String EMAIL = "ana@prueba.co";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private TokenEmisorPort tokenEmisor;

    private IniciarSesionService servicio;

    @BeforeEach
    void configurar() {
        when(hasher.hashear(anyString())).thenReturn("hash-ficticio");
        servicio = new IniciarSesionService(usuarios, hasher, tokenEmisor,
                new PoliticaBloqueo(5, Duration.ofMinutes(15)), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Usuario usuario(EstadoUsuario estado, int intentos, Instant bloqueadoHasta) {
        return new Usuario(10, Rol.PROVEEDOR, estado, null, null, "Ana", EMAIL, null, "hash-real", intentos,
                bloqueadoHasta, AHORA);
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un correo inexistente, cuando inicia sesión, entonces verifica un hash ficticio y responde CREDENCIALES_INVALIDAS")
    void correoInexistente() {
        // Arrange
        when(usuarios.buscarPorEmailParaActualizar(EMAIL)).thenReturn(Optional.empty());

        // Act - Assert
        assertCodigo(() -> servicio.iniciarSesion(" ANA@prueba.co ", "cualquier-clave"),
                CodigoError.CREDENCIALES_INVALIDAS);
        verify(hasher).coincide("cualquier-clave", "hash-ficticio");
    }

    @Test
    @DisplayName("Dado una cuenta activa y la contraseña correcta, cuando inicia sesión, entonces reinicia intentos y emite el token")
    void exito() {
        // Arrange
        when(usuarios.buscarPorEmailParaActualizar(EMAIL))
                .thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO, 2, null)));
        when(hasher.coincide("clave", "hash-real")).thenReturn(true);
        when(usuarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tokenEmisor.emitir(any())).thenReturn(new TokenEmisorPort.TokenAcceso("jwt", 1800));

        // Act
        SesionResultado resultado = servicio.iniciarSesion(EMAIL, "clave");

        // Assert
        assertThat(resultado).isEqualTo(new SesionResultado("jwt", "Bearer", 1800, Rol.PROVEEDOR, "/panel/proveedor"));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().intentosFallidos()).isZero();
    }

    @Test
    @DisplayName("Dado una contraseña incorrecta en el quinto intento, cuando inicia sesión, entonces bloquea la cuenta y responde CREDENCIALES_INVALIDAS")
    void quintoFalloBloquea() {
        // Arrange
        when(usuarios.buscarPorEmailParaActualizar(EMAIL))
                .thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO, 4, null)));
        when(hasher.coincide("mala", "hash-real")).thenReturn(false);
        when(usuarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        assertCodigo(() -> servicio.iniciarSesion(EMAIL, "mala"), CodigoError.CREDENCIALES_INVALIDAS);

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().bloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Dado una cuenta bloqueada, cuando inicia sesión, entonces responde CUENTA_BLOQUEADA con la contraseña correcta y CREDENCIALES_INVALIDAS con otra, sin sumar intentos")
    void cuentaBloqueada() {
        // Arrange
        when(usuarios.buscarPorEmailParaActualizar(EMAIL))
                .thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO, 0, AHORA.plusSeconds(60))));
        when(hasher.coincide(eq("buena"), anyString())).thenReturn(true);
        when(hasher.coincide(eq("mala"), anyString())).thenReturn(false);

        // Act - Assert
        assertCodigo(() -> servicio.iniciarSesion(EMAIL, "buena"), CodigoError.CUENTA_BLOQUEADA);
        assertCodigo(() -> servicio.iniciarSesion(EMAIL, "mala"), CodigoError.CREDENCIALES_INVALIDAS);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado una cuenta pendiente o inactiva, cuando inicia sesión con la contraseña correcta, entonces responde 403 con el código del estado")
    void estadosNoActivos() {
        // Arrange
        when(hasher.coincide("buena", "hash-real")).thenReturn(true);
        when(usuarios.buscarPorEmailParaActualizar(EMAIL))
                .thenReturn(Optional.of(usuario(EstadoUsuario.PENDIENTE_VERIFICACION, 0, null)))
                .thenReturn(Optional.of(usuario(EstadoUsuario.INACTIVO, 0, null)));

        // Act - Assert
        assertCodigo(() -> servicio.iniciarSesion(EMAIL, "buena"), CodigoError.CUENTA_NO_VERIFICADA);
        assertCodigo(() -> servicio.iniciarSesion(EMAIL, "buena"), CodigoError.CUENTA_INACTIVA);
        verify(tokenEmisor, never()).emitir(any());
    }
}

package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.CodigoMfaRepositoryPort;
import co.reservas.application.port.out.auth.NotificacionPort;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.CodigoMfa;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaAdminServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private CodigoMfaRepositoryPort codigosMfa;
    @Mock
    private NotificacionPort notificaciones;
    @Mock
    private TokenEmisorPort tokenEmisor;
    @Mock
    private SesionTokenService sesionTokenService;

    private final Instant ahora = Instant.parse("2026-11-13T12:00:00Z");
    private final Clock clock = Clock.fixed(ahora, ZoneOffset.UTC);
    private MfaAdminService service;

    private final Usuario admin = new Usuario(
            99, Rol.ADMIN, EstadoUsuario.ACTIVO, null, null,
            "Admin Principal", "admin@reservas.co", null, "hash-admin", 0, null, ahora);

    @BeforeEach
    void setUp() {
        service = new MfaAdminService(
                usuarios, passwordHasher, codigosMfa, notificaciones, tokenEmisor, sesionTokenService, clock);
    }

    @Test
    @DisplayName("Dado un administrador activo con credenciales válidas, cuando solicita MFA y verifica el código recibido, entonces obtiene su sesión autenticada")
    void solicitaYVerificaCodigoMfaParaAdmin() {
        // Arrange
        when(usuarios.buscarPorEmailParaActualizar("admin@reservas.co")).thenReturn(Optional.of(admin));
        when(passwordHasher.coincide("AdminClaveSegura123", "hash-admin")).thenReturn(true);
        when(codigosMfa.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act 1: solicitar código
        service.solicitarCodigo("admin@reservas.co", "AdminClaveSegura123");

        ArgumentCaptor<String> captorCodigo = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).enviarCodigoMfa(eq("admin@reservas.co"), eq("Admin Principal"), captorCodigo.capture());
        String codigoEnviado = captorCodigo.getValue();

        // Arrange 2: verificar código
        CodigoMfa almacenado = new CodigoMfa(
                1, 99, TokenVerificacion.calcularHash(codigoEnviado), ahora.plusSeconds(300), null, ahora);
        when(codigosMfa.buscarUltimoActivo(99, TokenVerificacion.calcularHash(codigoEnviado)))
                .thenReturn(Optional.of(almacenado));
        when(tokenEmisor.emitir(admin)).thenReturn(new TokenEmisorPort.TokenAcceso("jwt-admin", 1800));
        when(sesionTokenService.emitirTokenRefresco(99, ahora)).thenReturn("refresh-admin");

        // Act 2
        SesionResultado sesion = service.verificarCodigo("admin@reservas.co", codigoEnviado);

        // Assert
        assertEquals("jwt-admin", sesion.accessToken());
        assertEquals(Rol.ADMIN, sesion.rol());
        assertEquals("refresh-admin", sesion.refreshToken());
    }

    @Test
    @DisplayName("Dado un usuario que no es ADMIN, cuando intenta solicitar un código MFA, entonces el sistema responde con ACCESO_DENEGADO")
    void rechazaMfaParaUsuarioNoAdmin() {
        // Arrange
        Usuario cliente = new Usuario(
                2, Rol.CLIENTE, EstadoUsuario.ACTIVO, null, null,
                "Cliente", "cliente@reservas.co", null, "hash", 0, null, ahora);
        when(usuarios.buscarPorEmailParaActualizar("cliente@reservas.co")).thenReturn(Optional.of(cliente));
        when(passwordHasher.coincide("ClaveCliente123", "hash")).thenReturn(true);

        // Act & Assert
        assertThrows(ExcepcionNegocio.class,
                () -> service.solicitarCodigo("cliente@reservas.co", "ClaveCliente123"));
    }
}
package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.auth.TokenRefrescoRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.TokenRefresco;
import co.reservas.domain.usuario.TokenVerificacion;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionTokenServiceTest {

    @Mock
    private TokenRefrescoRepositoryPort tokensRefresco;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenEmisorPort tokenEmisor;

    private final Instant ahora = Instant.parse("2026-11-13T10:00:00Z");
    private final Clock clock = Clock.fixed(ahora, ZoneOffset.UTC);
    private SesionTokenService service;

    private final Usuario usuarioActivo = new Usuario(
            1, Rol.CLIENTE, EstadoUsuario.ACTIVO, null, null,
            "Cliente Prueba", "cliente@example.com", null, "hash", 0, null, ahora);

    @BeforeEach
    void setUp() {
        service = new SesionTokenService(tokensRefresco, usuarios, tokenEmisor, clock);
    }

    @Test
    @DisplayName("Dado un refresh token activo, cuando se solicita rotar la sesión, entonces revoca el token anterior y emite un nuevo accessToken y refreshToken")
    void rotaTokenActivo() {
        // Arrange
        String tokenClaro = "token-refresco-valido";
        TokenRefresco almacenado = new TokenRefresco(
                10, 1, TokenVerificacion.calcularHash(tokenClaro), ahora.plusSeconds(3600), null, ahora);
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(tokenClaro)))
                .thenReturn(Optional.of(almacenado));
        when(usuarios.buscarPorId(1)).thenReturn(Optional.of(usuarioActivo));
        when(tokenEmisor.emitir(usuarioActivo)).thenReturn(new TokenEmisorPort.TokenAcceso("nuevo-jwt", 1800));
        when(tokensRefresco.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        SesionResultado resultado = service.rotar(tokenClaro);

        // Assert
        assertEquals("nuevo-jwt", resultado.accessToken());
        assertNotNull(resultado.refreshToken());
    }

    @Test
    @DisplayName("Dado un refresh token ya revocado, cuando se intenta reutilizar, entonces revoca todos los tokens activos del usuario y rechaza la solicitud")
    void detectaReutilizacionDeTokenRevocado() {
        // Arrange
        String tokenRobado = "token-ya-usado";
        TokenRefresco revocado = new TokenRefresco(
                10, 1, TokenVerificacion.calcularHash(tokenRobado),
                ahora.plusSeconds(3600), ahora.minusSeconds(60), ahora.minusSeconds(120));
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(tokenRobado)))
                .thenReturn(Optional.of(revocado));

        // Act & Assert
        assertThrows(ExcepcionNegocio.class, () -> service.rotar(tokenRobado));
        verify(tokensRefresco).revocarActivosDeUsuario(1, ahora);
    }
}
package co.reservas.application.service.auth;

import co.reservas.application.port.in.auth.SesionResultado;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.auth.TokenRefrescoRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionTokenServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T10:00:00Z");
    private static final String EN_CLARO = "token-de-refresco-en-claro";
    private static final int ID_USUARIO = 10;

    @Mock
    private TokenRefrescoRepositoryPort tokensRefresco;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private TokenEmisorPort tokenEmisor;

    private SesionTokenService servicio;

    @BeforeEach
    void configurar() {
        servicio = new SesionTokenService(tokensRefresco, usuarios, tokenEmisor,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Usuario usuario(EstadoUsuario estado) {
        return new Usuario(ID_USUARIO, Rol.PROVEEDOR, estado, null, null, "Ana", "ana@prueba.co", null,
                "hash-real", 0, null, AHORA);
    }

    /** Token vivo: vence dentro de dos días y no está revocado. */
    private static TokenRefresco tokenVigente() {
        return new TokenRefresco(7, ID_USUARIO, TokenVerificacion.calcularHash(EN_CLARO),
                AHORA.plusSeconds(172_800), null, AHORA.minusSeconds(60));
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un token de refresco vigente, cuando rota la sesión, entonces revoca el anterior y entrega un par nuevo")
    void rotacionExitosa() {
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(tokenVigente()));
        when(usuarios.buscarPorId(ID_USUARIO)).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(tokenEmisor.emitir(any())).thenReturn(new TokenEmisorPort.TokenAcceso("nuevo-access", 900));

        SesionResultado resultado = servicio.rotar(EN_CLARO);

        assertThat(resultado.accessToken()).isEqualTo("nuevo-access");
        assertThat(resultado.tokenType()).isEqualTo("Bearer");
        assertThat(resultado.expiresIn()).isEqualTo(900);
        assertThat(resultado.rol()).isEqualTo(Rol.PROVEEDOR);
        assertThat(resultado.redirectTo()).isEqualTo("/panel/proveedor");
        // El refresco entregado es nuevo, no el que se acaba de usar.
        assertThat(resultado.refreshToken()).isNotBlank().isNotEqualTo(EN_CLARO);

        ArgumentCaptor<TokenRefresco> guardados = ArgumentCaptor.forClass(TokenRefresco.class);
        verify(tokensRefresco, times(2)).guardar(guardados.capture());
        assertThat(guardados.getAllValues().get(0).revocadoEn()).isEqualTo(AHORA);
        assertThat(guardados.getAllValues().get(1).revocadoEn()).isNull();
        assertThat(guardados.getAllValues().get(1).idUsuario()).isEqualTo(ID_USUARIO);
    }

    @Test
    @DisplayName("Dado un token ya revocado, cuando se reutiliza, entonces revoca todos los activos del usuario y responde CREDENCIALES_INVALIDAS")
    void reutilizacionDeTokenRevocado() {
        TokenRefresco revocado = tokenVigente().revocar(AHORA.minusSeconds(30));
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(revocado));

        assertCodigo(() -> servicio.rotar(EN_CLARO), CodigoError.CREDENCIALES_INVALIDAS);

        // Reutilizar un token revocado se trata como robo de credenciales: cae toda la familia.
        verify(tokensRefresco).revocarActivosDeUsuario(ID_USUARIO, AHORA);
        verify(tokensRefresco, never()).guardar(any());
        verify(tokenEmisor, never()).emitir(any());
    }

    @Test
    @DisplayName("Dado un token expirado, cuando rota la sesión, entonces responde CREDENCIALES_INVALIDAS sin emitir nada")
    void tokenExpirado() {
        TokenRefresco expirado = new TokenRefresco(7, ID_USUARIO, TokenVerificacion.calcularHash(EN_CLARO),
                AHORA.minusSeconds(1), null, AHORA.minusSeconds(600));
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(expirado));

        assertCodigo(() -> servicio.rotar(EN_CLARO), CodigoError.CREDENCIALES_INVALIDAS);

        verify(tokensRefresco, never()).revocarActivosDeUsuario(any(), any());
        verify(tokenEmisor, never()).emitir(any());
    }

    @Test
    @DisplayName("Dado un token desconocido, cuando rota la sesión, entonces responde CREDENCIALES_INVALIDAS")
    void tokenDesconocido() {
        when(tokensRefresco.buscarPorHash(any())).thenReturn(Optional.empty());

        assertCodigo(() -> servicio.rotar(EN_CLARO), CodigoError.CREDENCIALES_INVALIDAS);

        verify(tokenEmisor, never()).emitir(any());
    }

    @Test
    @DisplayName("Dado un token en blanco o nulo, cuando rota la sesión, entonces responde CREDENCIALES_INVALIDAS sin consultar el repositorio")
    void tokenVacio() {
        assertCodigo(() -> servicio.rotar(null), CodigoError.CREDENCIALES_INVALIDAS);
        assertCodigo(() -> servicio.rotar("   "), CodigoError.CREDENCIALES_INVALIDAS);

        verify(tokensRefresco, never()).buscarPorHash(any());
    }

    @Test
    @DisplayName("Dado un usuario que ya no está activo, cuando rota la sesión con un token vigente, entonces responde CREDENCIALES_INVALIDAS")
    void usuarioNoActivo() {
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(tokenVigente()));
        when(usuarios.buscarPorId(ID_USUARIO))
                .thenReturn(Optional.of(usuario(EstadoUsuario.PENDIENTE_VERIFICACION)));

        assertCodigo(() -> servicio.rotar(EN_CLARO), CodigoError.CREDENCIALES_INVALIDAS);

        verify(tokensRefresco, never()).guardar(any());
        verify(tokenEmisor, never()).emitir(any());
    }

    @Test
    @DisplayName("Dado un token vigente, cuando se revoca la sesión, entonces lo guarda revocado con la fecha actual")
    void revocacionExitosa() {
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(tokenVigente()));

        servicio.revocar(EN_CLARO);

        ArgumentCaptor<TokenRefresco> guardado = ArgumentCaptor.forClass(TokenRefresco.class);
        verify(tokensRefresco).guardar(guardado.capture());
        assertThat(guardado.getValue().revocadoEn()).isEqualTo(AHORA);
        assertThat(guardado.getValue().id()).isEqualTo(7);
    }

    @Test
    @DisplayName("Dado un token ya revocado, cuando se revoca otra vez, entonces no vuelve a escribir")
    void revocacionIdempotente() {
        when(tokensRefresco.buscarPorHash(TokenVerificacion.calcularHash(EN_CLARO)))
                .thenReturn(Optional.of(tokenVigente().revocar(AHORA.minusSeconds(30))));

        servicio.revocar(EN_CLARO);

        verify(tokensRefresco, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado un token en blanco o nulo, cuando se revoca la sesión, entonces no falla y no consulta el repositorio")
    void revocacionDeTokenVacio() {
        servicio.revocar(null);
        servicio.revocar("  ");

        verify(tokensRefresco, never()).buscarPorHash(any());
        verify(tokensRefresco, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado un usuario, cuando se emite un token de refresco, entonces lo guarda con hash, vigencia de 7 días y sin revocar")
    void emisionDeTokenDeRefresco() {
        String enClaro = servicio.emitirTokenRefresco(ID_USUARIO, AHORA);

        ArgumentCaptor<TokenRefresco> guardado = ArgumentCaptor.forClass(TokenRefresco.class);
        verify(tokensRefresco).guardar(guardado.capture());
        TokenRefresco token = guardado.getValue();
        assertThat(enClaro).isNotBlank();
        // Lo que se persiste es el hash, nunca el token en claro.
        assertThat(token.tokenHash()).isEqualTo(TokenVerificacion.calcularHash(enClaro)).isNotEqualTo(enClaro);
        assertThat(token.expiraEn()).isEqualTo(AHORA.plusSeconds(604_800));
        assertThat(token.revocadoEn()).isNull();
        assertThat(token.estaActivo(AHORA)).isTrue();
    }

    @Test
    @DisplayName("Dadas dos emisiones seguidas, cuando se comparan los tokens, entonces son distintos")
    void cadaEmisionEsUnica() {
        String primero = servicio.emitirTokenRefresco(ID_USUARIO, AHORA);
        String segundo = servicio.emitirTokenRefresco(ID_USUARIO, AHORA);

        assertThat(primero).isNotEqualTo(segundo);
        verify(tokensRefresco, times(2)).guardar(any());
    }
}

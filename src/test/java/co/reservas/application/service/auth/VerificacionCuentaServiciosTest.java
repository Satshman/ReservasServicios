package co.reservas.application.service.auth;

import co.reservas.application.port.out.auth.NotificacionPort;
import co.reservas.application.port.out.auth.TokenVerificacionRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.TokenVerificacion;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionCuentaServiciosTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private TokenVerificacionRepositoryPort tokens;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private NotificacionPort notificaciones;

    private static Usuario usuario(EstadoUsuario estado) {
        return new Usuario(3, Rol.CLIENTE, estado, null, null, "Ana", "ana@prueba.co", null, "hash", 0, null, AHORA);
    }

    @Test
    @DisplayName("Dado un token válido de una cuenta pendiente, cuando se verifica, entonces activa la cuenta y marca el token como usado")
    void verificaCuentaPendiente() {
        // Arrange
        String plano = "token-plano";
        TokenVerificacion token = TokenVerificacion.emitir(3, TokenVerificacion.calcularHash(plano), AHORA);
        when(tokens.buscarPorHash(token.tokenHash())).thenReturn(Optional.of(token));
        when(usuarios.buscarPorId(3)).thenReturn(Optional.of(usuario(EstadoUsuario.PENDIENTE_VERIFICACION)));
        VerificarCuentaService servicio = new VerificarCuentaService(tokens, usuarios, RELOJ);

        // Act
        servicio.verificar(plano);

        // Assert
        ArgumentCaptor<Usuario> activado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(activado.capture());
        assertThat(activado.getValue().estado()).isEqualTo(EstadoUsuario.ACTIVO);
        ArgumentCaptor<TokenVerificacion> usado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokens).guardar(usado.capture());
        assertThat(usado.getValue().usadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Dado un token vacío, demasiado largo o de una cuenta inactiva, cuando se verifica, entonces responde TOKEN_VERIFICACION_INVALIDO")
    void tokensInvalidos() {
        // Arrange
        String plano = "token-inactivo";
        TokenVerificacion token = TokenVerificacion.emitir(3, TokenVerificacion.calcularHash(plano), AHORA);
        when(tokens.buscarPorHash(token.tokenHash())).thenReturn(Optional.of(token));
        when(usuarios.buscarPorId(3)).thenReturn(Optional.of(usuario(EstadoUsuario.INACTIVO)));
        VerificarCuentaService servicio = new VerificarCuentaService(tokens, usuarios, RELOJ);

        // Act - Assert
        for (String invalido : new String[]{" ", "x".repeat(129), plano}) {
            assertThatThrownBy(() -> servicio.verificar(invalido))
                    .isInstanceOfSatisfying(ExcepcionNegocio.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.TOKEN_VERIFICACION_INVALIDO));
        }
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado una cuenta pendiente, cuando pide reenvío, entonces invalida tokens anteriores y emite uno nuevo; si ya está activa no hace nada")
    void reenvio() {
        // Arrange
        SolicitadorVerificacion solicitador = new SolicitadorVerificacion(tokens, eventos);
        ReenviarVerificacionService servicio = new ReenviarVerificacionService(usuarios, tokens, solicitador, RELOJ);
        when(usuarios.buscarPorEmail("ana@prueba.co"))
                .thenReturn(Optional.of(usuario(EstadoUsuario.PENDIENTE_VERIFICACION)))
                .thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));

        // Act
        servicio.reenviar("ANA@prueba.co");
        servicio.reenviar("ana@prueba.co");

        // Assert
        verify(tokens).invalidarPendientes(3, AHORA);
        verify(tokens).guardar(any(TokenVerificacion.class));
        ArgumentCaptor<VerificacionCuentaSolicitada> evento = ArgumentCaptor.forClass(VerificacionCuentaSolicitada.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().idUsuario()).isEqualTo(3);
        assertThat(evento.getValue().toString()).doesNotContain("ana@prueba.co");
    }

    @Test
    @DisplayName("Dado que el envío de correo falla, cuando se notifica tras el commit, entonces el error se registra y no se propaga")
    void falloDeCorreoNoSePropaga() {
        // Arrange
        NotificadorVerificacionCuenta notificador = new NotificadorVerificacionCuenta(notificaciones);
        VerificacionCuentaSolicitada evento = new VerificacionCuentaSolicitada(3, "ana@prueba.co", "Ana", "token");
        doThrow(new IllegalStateException("SMTP caído"))
                .when(notificaciones).enviarVerificacionCuenta("ana@prueba.co", "Ana", "token");

        // Act - Assert
        assertThatCode(() -> notificador.alSolicitarVerificacion(evento)).doesNotThrowAnyException();
        verifyNoInteractions(tokens);
    }
}

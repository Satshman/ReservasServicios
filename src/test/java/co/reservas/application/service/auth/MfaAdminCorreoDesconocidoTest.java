package co.reservas.application.service.auth;

import co.reservas.application.port.out.auth.CodigoMfaRepositoryPort;
import co.reservas.application.port.out.auth.NotificacionPort;
import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.auth.TokenEmisorPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.PoliticaBloqueo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MFA - correo desconocido")
class MfaAdminCorreoDesconocidoTest {

    @Test
    @DisplayName("Dado un correo que no existe, cuando solicita MFA, entonces compara contra un hash ficticio (tiempo constante) y responde 401 sin enviar código")
    void correoDesconocidoUsaHashFicticio() {
        // Arrange
        UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        CodigoMfaRepositoryPort codigos = mock(CodigoMfaRepositoryPort.class);
        NotificacionPort notificaciones = mock(NotificacionPort.class);
        when(hasher.hashear(any())).thenReturn("hash-ficticio");
        when(usuarios.buscarPorEmailParaActualizar("nadie@reservas.co")).thenReturn(Optional.empty());
        MfaAdminService service = new MfaAdminService(usuarios, hasher, codigos, notificaciones,
                mock(TokenEmisorPort.class), mock(SesionTokenService.class),
                new PoliticaBloqueo(5, Duration.ofMinutes(15)),
                Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC));

        // Act - Assert
        assertThatThrownBy(() -> service.solicitarCodigo("nadie@reservas.co", "una-clave-cualquiera"))
                .isInstanceOf(ExcepcionNegocio.class)
                .extracting(e -> ((ExcepcionNegocio) e).getCodigo())
                .isEqualTo(CodigoError.CREDENCIALES_INVALIDAS);
        verify(hasher).coincide("una-clave-cualquiera", "hash-ficticio");
        verify(codigos, never()).guardar(any());
        verify(notificaciones, never()).enviarCodigoMfa(any(), any(), any());
    }
}

package co.reservas.domain.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaBloqueoTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private final PoliticaBloqueo politica = new PoliticaBloqueo(5, Duration.ofMinutes(15));

    private static Usuario usuarioConIntentos(int intentos) {
        return new Usuario(1, Rol.CLIENTE, EstadoUsuario.ACTIVO, null, null, "Ana", "ana@prueba.co", null, "hash",
                intentos, null, AHORA);
    }

    @Test
    @DisplayName("Dado un usuario con 3 intentos fallidos, cuando falla otra vez, entonces suma un intento sin bloquear")
    void sumaIntentoSinBloquear() {
        // Arrange
        Usuario usuario = usuarioConIntentos(3);

        // Act
        Usuario resultado = politica.registrarIntentoFallido(usuario, AHORA);

        // Assert
        assertThat(resultado.intentosFallidos()).isEqualTo(4);
        assertThat(resultado.estaBloqueado(AHORA)).isFalse();
    }

    @Test
    @DisplayName("Dado un usuario con 4 intentos fallidos, cuando llega al quinto, entonces se bloquea 15 minutos y el contador vuelve a 0")
    void bloqueaAlQuintoIntento() {
        // Arrange
        Usuario usuario = usuarioConIntentos(4);

        // Act
        Usuario resultado = politica.registrarIntentoFallido(usuario, AHORA);

        // Assert
        assertThat(resultado.intentosFallidos()).isZero();
        assertThat(resultado.bloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(resultado.estaBloqueado(AHORA.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(resultado.estaBloqueado(AHORA.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    @DisplayName("Dado una configuración inválida, cuando se crea la política, entonces se rechaza")
    void rechazaConfiguracionInvalida() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> new PoliticaBloqueo(0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaBloqueo(5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package co.reservas.domain.usuario;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");

    @Test
    @DisplayName("Dado un registro nuevo, cuando se crea el usuario, entonces normaliza correo y nombre y queda pendiente de verificación")
    void registrarNormaliza() {
        // Arrange - Act
        Usuario usuario = Usuario.registrar(Rol.CLIENTE, "  Ana Gómez ", "  ANA@Prueba.CO ", " ", "hash", AHORA);

        // Assert
        assertThat(usuario.email()).isEqualTo("ana@prueba.co");
        assertThat(usuario.nombreCompleto()).isEqualTo("Ana Gómez");
        assertThat(usuario.telefono()).isNull();
        assertThat(usuario.estado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(usuario.intentosFallidos()).isZero();
    }

    @Test
    @DisplayName("Dado el rol ADMIN o un nombre muy corto, cuando se registra, entonces se rechaza con VALIDACION_FALLIDA")
    void registroInvalido() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> Usuario.registrar(Rol.ADMIN, "Admin", "a@b.co", null, "hash", AHORA))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION_FALLIDA);
                    assertThat(e.getDetalles()).containsExactly(
                            new DetalleCampo("rol", "Solo se permite registrar CLIENTE o PROVEEDOR"));
                });
        assertThatThrownBy(() -> Usuario.registrar(Rol.CLIENTE, "A", "a@b.co", null, "hash", AHORA))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    @DisplayName("Dado un usuario bloqueado con intentos, cuando se activa y reinicia, entonces queda activo y sin bloqueo")
    void activarYReiniciar() {
        // Arrange
        Usuario usuario = Usuario.registrar(Rol.PROVEEDOR, "Proveedor", "p@b.co", "300", "hash", AHORA)
                .conIntentosFallidos(2, AHORA.plusSeconds(60));

        // Act
        Usuario resultado = usuario.activar().conIntentosReiniciados();

        // Assert
        assertThat(usuario.requiereReinicioDeIntentos()).isTrue();
        assertThat(resultado.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(resultado.requiereReinicioDeIntentos()).isFalse();
        assertThat(resultado.estaBloqueado(AHORA)).isFalse();
    }

    @Test
    @DisplayName("Dado un proveedor sin nombre comercial ni zona, cuando se crea, entonces usa el nombre completo y America/Bogota")
    void proveedorConValoresPorDefecto() {
        // Arrange - Act
        Proveedor proveedor = Proveedor.nuevo(7, " ", "Clínica Norte", null);

        // Assert
        assertThat(proveedor.nombreComercial()).isEqualTo("Clínica Norte");
        assertThat(proveedor.zonaHoraria()).isEqualTo(ZoneId.of("America/Bogota"));
    }

    @Test
    @DisplayName("Dado una zona que no es un ID IANA, cuando se crea el proveedor, entonces se rechaza el campo zonaHoraria")
    void proveedorConZonaInvalida() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> Proveedor.nuevo(7, "Norte", "Clínica", "-05:00"))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getDetalles())
                        .extracting(detalle -> ((DetalleCampo) detalle).campo())
                        .containsExactly("zonaHoraria"));
        assertThatThrownBy(() -> Proveedor.nuevo(7, "x".repeat(101), "Clínica", "America/Lima"))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    @DisplayName("Dado un token emitido, cuando se evalúa su vigencia, entonces solo es válido sin usar y antes de 24 horas")
    void vigenciaTokenVerificacion() {
        // Arrange
        String plano = TokenVerificacion.generarValorPlano();
        TokenVerificacion token = TokenVerificacion.emitir(1, TokenVerificacion.calcularHash(plano), AHORA);

        // Act - Assert
        assertThat(plano).hasSize(43);
        assertThat(token.tokenHash()).hasSize(64).isEqualTo(TokenVerificacion.calcularHash(plano));
        assertThat(token.esValido(AHORA.plusSeconds(3600))).isTrue();
        assertThat(token.esValido(AHORA.plus(TokenVerificacion.VIGENCIA))).isFalse();
        assertThat(token.marcarUsado(AHORA).esValido(AHORA)).isFalse();
    }
}

package co.reservas.application.service.auth;

import co.reservas.application.port.out.auth.PasswordHasherPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.usuario.EstadoUsuario;
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
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Aprovisionamiento del administrador inicial")
class AprovisionadorAdministradorTest {

    private static final String CLAVE = "clave-admin-segura-2026";

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private PasswordHasherPort passwordHasher;

    private final Instant ahora = Instant.parse("2026-11-24T12:00:00Z");
    private AprovisionadorAdministrador aprovisionador;

    @BeforeEach
    void setUp() {
        aprovisionador = new AprovisionadorAdministrador(usuarios, passwordHasher, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Dado un correo nuevo, cuando se aprovisiona, entonces crea un ADMIN activo con el correo normalizado y la clave cifrada")
    void creaAdministradorActivo() {
        // Arrange
        when(usuarios.existeEmail("admin@reservas.co")).thenReturn(false);
        when(passwordHasher.hashear(CLAVE)).thenReturn("hash");
        when(usuarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        boolean creado = aprovisionador.aprovisionar("  Admin@Reservas.co ", "  ", CLAVE);

        // Assert
        assertThat(creado).isTrue();
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        Usuario admin = captor.getValue();
        assertThat(admin.rol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(admin.email()).isEqualTo("admin@reservas.co");
        assertThat(admin.nombreCompleto()).isEqualTo(AprovisionadorAdministrador.NOMBRE_POR_DEFECTO);
        assertThat(admin.passwordHash()).isEqualTo("hash");
        assertThat(admin.creadoEn()).isEqualTo(ahora);
    }

    @Test
    @DisplayName("Dado un correo ya registrado, cuando se aprovisiona, entonces no modifica nada")
    void noModificaUnaCuentaExistente() {
        // Arrange
        when(usuarios.existeEmail("admin@reservas.co")).thenReturn(true);

        // Act
        boolean creado = aprovisionador.aprovisionar("admin@reservas.co", "Admin", CLAVE);

        // Assert
        assertThat(creado).isFalse();
        verify(usuarios, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado un correo inválido o una contraseña fuera de 12 a 64 caracteres, cuando se aprovisiona, entonces falla al arrancar")
    void rechazaConfiguracionInvalida() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> aprovisionador.aprovisionar("sin-arroba", "Admin", CLAVE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_ADMIN_EMAIL");
        assertThatThrownBy(() -> aprovisionador.aprovisionar("admin@reservas.co", "Admin", "corta"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_ADMIN_PASSWORD");
        assertThatThrownBy(() -> aprovisionador.aprovisionar("admin@reservas.co", "Admin", null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> aprovisionador.aprovisionar("admin@reservas.co", "Admin", "x".repeat(65)))
                .isInstanceOf(IllegalStateException.class);
        verify(usuarios, never()).guardar(any());
    }
}

package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorPropiedadReservaTest {

    private static final Instant INICIO = Instant.parse("2026-10-12T15:00:00Z");
    private static final Reserva RESERVA = new Reserva(5, 8, 7, EstadoReserva.CONFIRMADA, INICIO,
            INICIO.plusSeconds(1800), INICIO.minus(Duration.ofDays(7)), Set.of());
    private static final Reserva RESERVA_DE_SERVICIO_INEXISTENTE = new Reserva(6, 8, 8, EstadoReserva.CONFIRMADA,
            INICIO, INICIO.plusSeconds(1800), INICIO.minus(Duration.ofDays(7)), Set.of());
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ServicioRepositoryPort servicios;

    private VerificadorPropiedad verificador;

    @BeforeEach
    void configurar() {
        verificador = new VerificadorPropiedad(usuarios, servicios);
    }

    private void assertDenegado(UsuarioAutenticado usuario) {
        assertDenegado(usuario, RESERVA);
    }

    private void assertDenegado(UsuarioAutenticado usuario, Reserva reserva) {
        assertThatThrownBy(() -> verificador.exigirAccesoAReserva(usuario, reserva))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.ACCESO_DENEGADO));
    }

    @Test
    @DisplayName("Dado el cliente que hizo la reserva, cuando se verifica el acceso, entonces se permite")
    void clienteDueno() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(new Cliente(8, 30)));

        // Act - Assert
        assertThatCode(() -> verificador.exigirAccesoAReserva(new UsuarioAutenticado(30, Rol.CLIENTE), RESERVA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dado otro cliente o un usuario sin perfil de cliente, cuando se verifica el acceso, entonces responde ACCESO_DENEGADO")
    void clienteAjenoOSinPerfil() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(31)).thenReturn(Optional.of(new Cliente(99, 31)));
        when(usuarios.buscarClientePorUsuario(32)).thenReturn(Optional.empty());

        // Act - Assert
        assertDenegado(new UsuarioAutenticado(31, Rol.CLIENTE));
        assertDenegado(new UsuarioAutenticado(32, Rol.CLIENTE));
    }

    @Test
    @DisplayName("Dado el proveedor dueño del servicio reservado, cuando se verifica el acceso, entonces se permite")
    void proveedorDueno() {
        // Arrange
        when(usuarios.buscarProveedorPorUsuario(20))
                .thenReturn(Optional.of(new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"))));
        when(servicios.buscarPorId(7)).thenReturn(Optional.of(SERVICIO));

        // Act - Assert
        assertThatCode(() -> verificador.exigirAccesoAReserva(new UsuarioAutenticado(20, Rol.PROVEEDOR), RESERVA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dado un proveedor de otro negocio, un proveedor sin perfil o una reserva de un servicio inexistente, cuando se verifica el acceso, entonces responde ACCESO_DENEGADO")
    void proveedorAjeno() {
        // Arrange
        when(usuarios.buscarProveedorPorUsuario(21))
                .thenReturn(Optional.of(new Proveedor(4, 21, "Otro", ZoneId.of("America/Bogota"))));
        when(usuarios.buscarProveedorPorUsuario(22)).thenReturn(Optional.empty());
        when(usuarios.buscarProveedorPorUsuario(20))
                .thenReturn(Optional.of(new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"))));
        when(servicios.buscarPorId(7)).thenReturn(Optional.of(SERVICIO));
        when(servicios.buscarPorId(8)).thenReturn(Optional.empty());

        // Act - Assert
        assertDenegado(new UsuarioAutenticado(21, Rol.PROVEEDOR));
        assertDenegado(new UsuarioAutenticado(22, Rol.PROVEEDOR));
        assertDenegado(new UsuarioAutenticado(20, Rol.PROVEEDOR), RESERVA_DE_SERVICIO_INEXISTENTE);
    }

    @Test
    @DisplayName("Dado un administrador, cuando se verifica el acceso a una reserva, entonces responde ACCESO_DENEGADO sin consultar perfiles")
    void administrador() {
        // Arrange - Act - Assert
        assertDenegado(new UsuarioAutenticado(1, Rol.ADMIN));
        verifyNoInteractions(usuarios, servicios);
    }
}

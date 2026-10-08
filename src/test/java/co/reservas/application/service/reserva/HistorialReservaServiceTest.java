package co.reservas.application.service.reserva;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reserva.FiltroHistorialReservas;
import co.reservas.application.port.in.reserva.HistorialReservaResultado;
import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.application.port.out.reserva.CriterioBusquedaReservas;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.application.service.usuario.AutoresCambio;
import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialReservaServiceTest {

    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(30, Rol.CLIENTE);
    private static final UsuarioAutenticado DUENO = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Cliente PERFIL_CLIENTE = new Cliente(5, 30);
    private static final Proveedor PROVEEDOR = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final Servicio CONSULTA = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(60), 1);
    private static final Servicio TERAPIA = new Servicio(8, 3, 1, EstadoServicio.ACTIVO, "Terapia", null,
            Duration.ofMinutes(60), 1);
    private static final Instant CREADA = Instant.parse("2026-10-05T13:00:00Z");
    /** Lunes 2026-10-12 a las 19:00 en Bogotá, que en UTC ya es martes. */
    private static final Instant LUNES_NOCHE = Instant.parse("2026-10-13T00:00:00Z");
    private static final Instant MARTES_MANANA = Instant.parse("2026-10-13T13:00:00Z");

    @Mock
    private ReservaRepositoryPort reservas;
    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private VerificadorPropiedad verificador;

    private HistorialReservaService servicio;

    @BeforeEach
    void configurar() {
        servicio = new HistorialReservaService(reservas, servicios, usuarios, verificador, new AutoresCambio(usuarios));
    }

    private static Reserva reserva(int id, Servicio servicioReservado, Instant inicio, EstadoReserva estado) {
        return new Reserva(id, PERFIL_CLIENTE.id(), servicioReservado.id(), estado, inicio,
                inicio.plus(servicioReservado.duracion()), CREADA, Set.of());
    }

    private static Usuario usuario(int id, Rol rol, String nombre) {
        return new Usuario(id, rol, EstadoUsuario.ACTIVO, null, null, nombre, nombre + "@prueba.co", null, "hash", 0,
                null, CREADA);
    }

    private void conCatalogos() {
        when(servicios.buscarPorIds(any())).thenReturn(List.of(CONSULTA, TERAPIA));
        when(usuarios.buscarProveedoresPorIds(List.of(3))).thenReturn(List.of(PROVEEDOR));
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un cliente con una reserva cancelada por el proveedor, cuando consulta su historial, entonces ve cada cambio con su autor en orden cronológico")
    void clienteVeCambiosConAutor() {
        // Arrange
        Reserva cancelada = reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CANCELADA);
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(PERFIL_CLIENTE));
        when(reservas.buscar(any())).thenReturn(List.of(cancelada));
        conCatalogos();
        Instant cancelacion = CREADA.plus(Duration.ofHours(2));
        when(reservas.listarHistorial(List.of(11))).thenReturn(List.of(
                new HistorialReserva(1, 11, null, EstadoReserva.CONFIRMADA, 30, CREADA),
                new HistorialReserva(2, 11, EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA, 20, cancelacion)));
        when(usuarios.buscarPorIds(List.of(30, 20))).thenReturn(List.of(usuario(30, Rol.CLIENTE, "Ana"),
                usuario(20, Rol.PROVEEDOR, "Luis")));

        // Act
        List<HistorialReservaResultado> historial = servicio.consultar(CLIENTE, FiltroHistorialReservas.sinFiltros());

        // Assert
        assertThat(historial).singleElement().satisfies(resultado -> {
            assertThat(resultado.idReserva()).isEqualTo(11);
            assertThat(resultado.nombreServicio()).isEqualTo("Consulta");
            assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(resultado.fechaHoraInicio()).hasToString("2026-10-12T19:00-05:00");
            assertThat(resultado.cambios()).hasSize(2);
            assertThat(resultado.cambios().get(0).estadoAnterior()).isNull();
            assertThat(resultado.cambios().get(1).realizadoPor())
                    .isEqualTo(new AutorCambio(20, "Luis", Rol.PROVEEDOR));
            assertThat(resultado.cambios().get(1).fechaCambio()).isEqualTo(cancelacion);
        });
        ArgumentCaptor<CriterioBusquedaReservas> criterio = ArgumentCaptor.forClass(CriterioBusquedaReservas.class);
        verify(reservas).buscar(criterio.capture());
        assertThat(criterio.getValue()).isEqualTo(new CriterioBusquedaReservas(5, null, null, null, null));
    }

    @Test
    @DisplayName("Dado un rango de fechas, cuando se filtra, entonces se comparan las fechas locales del proveedor y no las de UTC")
    void rangoEnZonaDelProveedor() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(PERFIL_CLIENTE));
        when(reservas.buscar(any())).thenReturn(List.of(reserva(12, TERAPIA, MARTES_MANANA, EstadoReserva.CONFIRMADA),
                reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CONFIRMADA)));
        conCatalogos();
        LocalDate lunes = LocalDate.of(2026, 10, 12);

        // Act
        List<HistorialReservaResultado> historial = servicio.consultar(CLIENTE,
                new FiltroHistorialReservas(null, EstadoReserva.CONFIRMADA, lunes, lunes));

        // Assert
        assertThat(historial).extracting(HistorialReservaResultado::idReserva).containsExactly(11);
        ArgumentCaptor<CriterioBusquedaReservas> criterio = ArgumentCaptor.forClass(CriterioBusquedaReservas.class);
        verify(reservas).buscar(criterio.capture());
        assertThat(criterio.getValue().estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(criterio.getValue().inicioDesde()).isEqualTo(Instant.parse("2026-10-11T00:00:00Z"));
        assertThat(criterio.getValue().inicioHasta()).isEqualTo(Instant.parse("2026-10-14T00:00:00Z"));
    }

    @Test
    @DisplayName("Dado un cliente sin reservas, cuando consulta su historial, entonces recibe una lista vacía sin error")
    void historialVacio() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(PERFIL_CLIENTE));
        when(reservas.buscar(any())).thenReturn(List.of());

        // Act
        List<HistorialReservaResultado> historial = servicio.consultar(CLIENTE, FiltroHistorialReservas.sinFiltros());

        // Assert
        assertThat(historial).isEmpty();
        verify(reservas, never()).listarHistorial(any());
    }

    @Test
    @DisplayName("Dado una fecha hasta anterior a desde, cuando se consulta, entonces responde VALIDACION_FALLIDA sin consultar reservas")
    void rangoInvertido() {
        // Arrange
        FiltroHistorialReservas filtro = new FiltroHistorialReservas(null, null, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 10));

        // Act - Assert
        assertCodigo(() -> servicio.consultar(CLIENTE, filtro), CodigoError.VALIDACION_FALLIDA);
        verifyNoInteractions(reservas);
    }

    @Test
    @DisplayName("Dado un proveedor sin filtro de servicio, cuando consulta el historial, entonces busca en todos sus servicios")
    void proveedorBuscaEnSusServicios() {
        // Arrange
        when(verificador.proveedorDe(DUENO)).thenReturn(PROVEEDOR);
        when(servicios.listarPorProveedor(3)).thenReturn(List.of(CONSULTA, TERAPIA));
        when(reservas.buscar(any())).thenReturn(List.of());

        // Act
        servicio.consultar(DUENO, new FiltroHistorialReservas(null, EstadoReserva.CANCELADA, null, null));

        // Assert
        verify(reservas).buscar(new CriterioBusquedaReservas(null, Set.of(7, 8), EstadoReserva.CANCELADA, null,
                null));
    }

    @Test
    @DisplayName("Dado un proveedor que filtra por un servicio propio, cuando consulta, entonces solo busca en ese servicio")
    void proveedorFiltraServicioPropio() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 8)).thenReturn(new ServicioPropio(TERAPIA, PROVEEDOR));
        when(reservas.buscar(any())).thenReturn(List.of());

        // Act
        servicio.consultar(DUENO, new FiltroHistorialReservas(8, null, null, null));

        // Assert
        verify(reservas).buscar(new CriterioBusquedaReservas(null, Set.of(8), null, null, null));
    }

    @Test
    @DisplayName("Dado un servicio de otro proveedor o un usuario ADMIN, cuando consulta el historial, entonces responde ACCESO_DENEGADO")
    void accesoDenegado() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 99)).thenThrow(VerificadorPropiedad.accesoDenegado("ajeno"));
        UsuarioAutenticado admin = new UsuarioAutenticado(1, Rol.ADMIN);

        // Act - Assert
        assertCodigo(() -> servicio.consultar(DUENO, new FiltroHistorialReservas(99, null, null, null)),
                CodigoError.ACCESO_DENEGADO);
        assertCodigo(() -> servicio.consultar(admin, FiltroHistorialReservas.sinFiltros()),
                CodigoError.ACCESO_DENEGADO);
        verifyNoInteractions(reservas);
    }

    @Test
    @DisplayName("Dado una reserva de otro cliente o inexistente, cuando se consulta su historial, entonces responde 403 o 404")
    void historialDeUnaReserva() {
        // Arrange
        Reserva ajena = reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CONFIRMADA);
        when(reservas.buscarPorId(11)).thenReturn(Optional.of(ajena));
        when(reservas.buscarPorId(404)).thenReturn(Optional.empty());
        doThrow(VerificadorPropiedad.accesoDenegado("ajena")).when(verificador).exigirAccesoAReserva(CLIENTE, ajena);

        // Act - Assert
        assertCodigo(() -> servicio.consultarReserva(CLIENTE, 11), CodigoError.ACCESO_DENEGADO);
        assertCodigo(() -> servicio.consultarReserva(CLIENTE, 404), CodigoError.RESERVA_NO_ENCONTRADA);
        verify(reservas, never()).listarHistorial(any());
    }

    @Test
    @DisplayName("Dado un usuario con rol CLIENTE sin perfil de cliente, cuando consulta el historial, entonces responde ACCESO_DENEGADO sin consultar reservas")
    void clienteSinPerfil() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.empty());

        // Act - Assert
        assertCodigo(() -> servicio.consultar(CLIENTE, FiltroHistorialReservas.sinFiltros()),
                CodigoError.ACCESO_DENEGADO);
        verifyNoInteractions(reservas);
    }

    @Test
    @DisplayName("Dado un cliente que filtra por servicio, cuando consulta su historial, entonces solo busca sus reservas de ese servicio")
    void clienteFiltraPorServicio() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(PERFIL_CLIENTE));
        when(reservas.buscar(any())).thenReturn(List.of());

        // Act
        servicio.consultar(CLIENTE, new FiltroHistorialReservas(8, null, null, null));

        // Assert
        verify(reservas).buscar(new CriterioBusquedaReservas(5, Set.of(8), null, null, null));
        verifyNoInteractions(verificador);
    }

    @Test
    @DisplayName("Dado un proveedor que filtra por un servicio inexistente, cuando consulta el historial, entonces responde SERVICIO_NO_ENCONTRADO")
    void proveedorFiltraServicioInexistente() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 404)).thenThrow(VerificadorPropiedad.servicioNoEncontrado());

        // Act - Assert
        assertCodigo(() -> servicio.consultar(DUENO, new FiltroHistorialReservas(404, null, null, null)),
                CodigoError.SERVICIO_NO_ENCONTRADO);
        verifyNoInteractions(reservas);
    }

    @Test
    @DisplayName("Dado reservas de varios clientes en los servicios del proveedor, cuando consulta el historial, entonces conserva el orden de la más reciente a la más antigua y muestra quién hizo cada cambio")
    void proveedorVeReservasDeVariosClientes() {
        // Arrange
        Reserva deOtroCliente = new Reserva(12, 6, TERAPIA.id(), EstadoReserva.CONFIRMADA, MARTES_MANANA,
                MARTES_MANANA.plus(TERAPIA.duracion()), CREADA, Set.of());
        Reserva canceladaPorProveedor = reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CANCELADA);
        Instant cancelacion = CREADA.plus(Duration.ofHours(1));
        when(verificador.proveedorDe(DUENO)).thenReturn(PROVEEDOR);
        when(servicios.listarPorProveedor(3)).thenReturn(List.of(CONSULTA, TERAPIA));
        when(reservas.buscar(any())).thenReturn(List.of(deOtroCliente, canceladaPorProveedor));
        conCatalogos();
        when(reservas.listarHistorial(List.of(12, 11))).thenReturn(List.of(
                new HistorialReserva(1, 11, null, EstadoReserva.CONFIRMADA, 30, CREADA),
                new HistorialReserva(2, 12, null, EstadoReserva.CONFIRMADA, 31, CREADA),
                new HistorialReserva(3, 11, EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA, 20, cancelacion)));
        when(usuarios.buscarPorIds(any())).thenReturn(List.of(usuario(30, Rol.CLIENTE, "Ana"),
                usuario(31, Rol.CLIENTE, "Beto"), usuario(20, Rol.PROVEEDOR, "Luis")));

        // Act
        List<HistorialReservaResultado> historial = servicio.consultar(DUENO, FiltroHistorialReservas.sinFiltros());

        // Assert
        assertThat(historial).extracting(HistorialReservaResultado::idReserva).containsExactly(12, 11);
        assertThat(historial).extracting(HistorialReservaResultado::idCliente).containsExactly(6, 5);
        assertThat(historial.get(0).cambios()).singleElement()
                .satisfies(cambio -> assertThat(cambio.realizadoPor().nombre()).isEqualTo("Beto"));
        assertThat(historial.get(1).cambios()).extracting(cambio -> cambio.realizadoPor().rol())
                .containsExactly(Rol.CLIENTE, Rol.PROVEEDOR);
        assertThat(historial.get(1).cambios().get(1).estadoNuevo()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Dado solo la fecha desde, cuando se filtra, entonces no hay límite superior y se excluyen las reservas anteriores en la zona del proveedor")
    void soloDesde() {
        // Arrange
        when(usuarios.buscarClientePorUsuario(30)).thenReturn(Optional.of(PERFIL_CLIENTE));
        when(reservas.buscar(any())).thenReturn(List.of(reserva(12, TERAPIA, MARTES_MANANA, EstadoReserva.CONFIRMADA),
                reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CONFIRMADA)));
        conCatalogos();

        // Act
        List<HistorialReservaResultado> historial = servicio.consultar(CLIENTE,
                new FiltroHistorialReservas(null, null, LocalDate.of(2026, 10, 13), null));

        // Assert
        assertThat(historial).extracting(HistorialReservaResultado::idReserva).containsExactly(12);
        ArgumentCaptor<CriterioBusquedaReservas> criterio = ArgumentCaptor.forClass(CriterioBusquedaReservas.class);
        verify(reservas).buscar(criterio.capture());
        assertThat(criterio.getValue().inicioDesde()).isEqualTo(Instant.parse("2026-10-12T00:00:00Z"));
        assertThat(criterio.getValue().inicioHasta()).isNull();
    }

    @Test
    @DisplayName("Dado la reserva propia, cuando se consulta su historial, entonces incluye su creación")
    void historialDeReservaPropia() {
        // Arrange
        Reserva propia = reserva(11, CONSULTA, LUNES_NOCHE, EstadoReserva.CONFIRMADA);
        when(reservas.buscarPorId(11)).thenReturn(Optional.of(propia));
        conCatalogos();
        when(reservas.listarHistorial(List.of(11))).thenReturn(List.of(
                new HistorialReserva(1, 11, null, EstadoReserva.CONFIRMADA, 30, CREADA)));
        when(usuarios.buscarPorIds(List.of(30))).thenReturn(List.of(usuario(30, Rol.CLIENTE, "Ana")));

        // Act
        HistorialReservaResultado resultado = servicio.consultarReserva(CLIENTE, 11);

        // Assert
        verify(verificador).exigirAccesoAReserva(CLIENTE, propia);
        assertThat(resultado.cambios()).singleElement().satisfies(cambio -> {
            assertThat(cambio.estadoNuevo()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(cambio.realizadoPor().nombre()).isEqualTo("Ana");
        });
    }
}

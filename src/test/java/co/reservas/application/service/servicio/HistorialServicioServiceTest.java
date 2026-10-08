package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.servicio.CambioServicioResultado;
import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.in.servicio.ServicioResultado;
import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.usuario.AutoresCambio;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.HistorialServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.servicio.TipoCambioServicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import co.reservas.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-11: consulta del historial de servicios y registro de los cambios de estado.
 */
@ExtendWith(MockitoExtension.class)
class HistorialServicioServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final UsuarioAutenticado DUENO = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Proveedor PROVEEDOR = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final Servicio CONSULTA = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);
    private static final Servicio TERAPIA = new Servicio(8, 3, 1, EstadoServicio.ACTIVO, "Terapia", null,
            Duration.ofMinutes(60), 1);
    private static final Usuario LUIS = new Usuario(20, Rol.PROVEEDOR, EstadoUsuario.ACTIVO, null, null, "Luis",
            "luis@prueba.co", null, "hash", 0, null, AHORA);

    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private CatalogoRepositoryPort catalogos;
    @Mock
    private VerificadorPropiedad verificador;

    private HistorialServicioService historial;

    @BeforeEach
    void configurar() {
        historial = new HistorialServicioService(servicios, verificador, new AutoresCambio(usuarios));
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un proveedor con varios servicios, cuando consulta el historial sin filtro, entonces ve los cambios de todos con el nombre del servicio y su autor")
    void historialDeTodosLosServicios() {
        // Arrange
        when(verificador.proveedorDe(DUENO)).thenReturn(PROVEEDOR);
        when(servicios.listarPorProveedor(3)).thenReturn(List.of(CONSULTA, TERAPIA));
        when(servicios.listarHistorial(Set.of(7, 8))).thenReturn(List.of(
                new HistorialServicio(1, 7, TipoCambioServicio.CREACION, null, "Consulta · 30 min · capacidad 1", 20,
                        AHORA),
                new HistorialServicio(2, 8, TipoCambioServicio.CREACION, null, "Terapia · 60 min · capacidad 1", 20,
                        AHORA)));
        when(usuarios.buscarPorIds(List.of(20))).thenReturn(List.of(LUIS));

        // Act
        List<CambioServicioResultado> cambios = historial.consultar(DUENO, null);

        // Assert
        assertThat(cambios).extracting(CambioServicioResultado::nombreServicio).containsExactly("Consulta", "Terapia");
        assertThat(cambios).extracting(CambioServicioResultado::realizadoPor)
                .containsOnly(new AutorCambio(20, "Luis", Rol.PROVEEDOR));
    }

    @Test
    @DisplayName("Dado un servicio recién creado, cuando se filtra por él, entonces el historial muestra solo su creación")
    void servicioRecienCreado() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 7)).thenReturn(new ServicioPropio(CONSULTA, PROVEEDOR));
        when(servicios.listarHistorial(Set.of(7))).thenReturn(List.of(new HistorialServicio(1, 7,
                TipoCambioServicio.CREACION, null, "Consulta · 30 min · capacidad 1", 20, AHORA)));
        when(usuarios.buscarPorIds(List.of(20))).thenReturn(List.of(LUIS));

        // Act
        List<CambioServicioResultado> cambios = historial.consultar(DUENO, 7);

        // Assert
        assertThat(cambios).singleElement().satisfies(cambio -> {
            assertThat(cambio.tipoCambio()).isEqualTo(TipoCambioServicio.CREACION);
            assertThat(cambio.valorAnterior()).isNull();
            assertThat(cambio.fechaCambio()).isEqualTo(AHORA);
        });
    }

    @Test
    @DisplayName("Dado un servicio de otro proveedor, cuando consulta su historial, entonces responde ACCESO_DENEGADO")
    void servicioAjeno() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 99)).thenThrow(VerificadorPropiedad.accesoDenegado("ajeno"));

        // Act - Assert
        assertCodigo(() -> historial.consultar(DUENO, 99), CodigoError.ACCESO_DENEGADO);
        verify(servicios, never()).listarHistorial(any());
    }

    @Test
    @DisplayName("Dado un servicio inexistente, cuando el proveedor filtra el historial por él, entonces responde SERVICIO_NO_ENCONTRADO")
    void servicioInexistente() {
        // Arrange
        when(verificador.servicioPropio(DUENO, 404)).thenThrow(VerificadorPropiedad.servicioNoEncontrado());

        // Act - Assert
        assertCodigo(() -> historial.consultar(DUENO, 404), CodigoError.SERVICIO_NO_ENCONTRADO);
        verify(servicios, never()).listarHistorial(any());
    }

    @Test
    @DisplayName("Dado un usuario sin perfil de proveedor, cuando consulta el historial de servicios, entonces responde ACCESO_DENEGADO")
    void usuarioSinPerfilDeProveedor() {
        // Arrange
        UsuarioAutenticado cliente = new UsuarioAutenticado(30, Rol.CLIENTE);
        when(verificador.proveedorDe(cliente)).thenThrow(VerificadorPropiedad.accesoDenegado("no es proveedor"));

        // Act - Assert
        assertCodigo(() -> historial.consultar(cliente, null), CodigoError.ACCESO_DENEGADO);
        verify(servicios, never()).listarHistorial(any());
    }

    @Test
    @DisplayName("Dado varios servicios nuevos, cuando se guardan, entonces cada uno registra su evento CREACION con la oferta y el usuario que lo creó")
    void registroDeCreacion() {
        // Arrange
        RegistroServicios registro = new RegistroServicios(servicios, catalogos);
        when(servicios.guardar(any())).thenReturn(CONSULTA, TERAPIA);
        List<NuevoServicioComando> comandos = List.of(new NuevoServicioComando("Consulta", null, 1, 30, 1),
                new NuevoServicioComando("Terapia", null, 1, 60, 1));

        // Act
        List<Servicio> guardados = registro.guardar(3, comandos, 20, AHORA);

        // Assert
        assertThat(guardados).containsExactly(CONSULTA, TERAPIA);
        verify(servicios).registrarHistorial(new HistorialServicio(null, 7, TipoCambioServicio.CREACION, null,
                "Consulta · 30 min · capacidad 1", 20, AHORA));
        verify(servicios).registrarHistorial(new HistorialServicio(null, 8, TipoCambioServicio.CREACION, null,
                "Terapia · 60 min · capacidad 1", 20, AHORA));
    }

    @Test
    @DisplayName("Dado un servicio de otro proveedor, cuando intenta cambiar su estado, entonces responde ACCESO_DENEGADO sin guardar ni registrar historial")
    void cambiarEstadoServicioAjeno() {
        // Arrange
        ServicioService servicioService = new ServicioService(servicios, usuarios,
                new RegistroServicios(servicios, catalogos), verificador, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(verificador.servicioPropioBloqueado(DUENO, 99)).thenThrow(VerificadorPropiedad.accesoDenegado("ajeno"));

        // Act - Assert
        assertCodigo(() -> servicioService.cambiarEstado(DUENO, 99, EstadoServicio.INACTIVO),
                CodigoError.ACCESO_DENEGADO);
        verify(servicios, never()).guardar(any());
        verify(servicios, never()).registrarHistorial(any());
    }

    @Test
    @DisplayName("Dado un servicio inactivo propio, cuando se reactiva, entonces el historial registra INACTIVO → ACTIVO")
    void reactivarServicio() {
        // Arrange
        ServicioService servicioService = new ServicioService(servicios, usuarios,
                new RegistroServicios(servicios, catalogos), verificador, Clock.fixed(AHORA, ZoneOffset.UTC));
        Servicio inactivo = CONSULTA.conEstado(EstadoServicio.INACTIVO);
        when(verificador.servicioPropioBloqueado(DUENO, 7)).thenReturn(new ServicioPropio(inactivo, PROVEEDOR));
        when(servicios.guardar(CONSULTA)).thenReturn(CONSULTA);

        // Act
        ServicioResultado resultado = servicioService.cambiarEstado(DUENO, 7, EstadoServicio.ACTIVO);

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoServicio.ACTIVO);
        verify(servicios).registrarHistorial(new HistorialServicio(null, 7, TipoCambioServicio.ESTADO_CAMBIADO,
                "INACTIVO", "ACTIVO", 20, AHORA));
    }

    @Test
    @DisplayName("Dado un servicio activo propio, cuando se inactiva, entonces se guarda y se registra el cambio de estado; repetirlo no registra nada")
    void cambiarEstado() {
        // Arrange
        ServicioService servicioService = new ServicioService(servicios, usuarios,
                new RegistroServicios(servicios, catalogos), verificador, Clock.fixed(AHORA, ZoneOffset.UTC));
        Servicio inactivo = CONSULTA.conEstado(EstadoServicio.INACTIVO);
        when(verificador.servicioPropioBloqueado(DUENO, 7)).thenReturn(new ServicioPropio(CONSULTA, PROVEEDOR))
                .thenReturn(new ServicioPropio(inactivo, PROVEEDOR));
        when(servicios.guardar(inactivo)).thenReturn(inactivo);

        // Act
        ServicioResultado resultado = servicioService.cambiarEstado(DUENO, 7, EstadoServicio.INACTIVO);
        servicioService.cambiarEstado(DUENO, 7, EstadoServicio.INACTIVO);

        // Assert
        assertThat(resultado.estado()).isEqualTo(EstadoServicio.INACTIVO);
        verify(servicios).registrarHistorial(HistorialServicio.cambioDeEstado(EstadoServicio.ACTIVO, inactivo, 20,
                AHORA));
        verify(servicios).guardar(any());
    }
}

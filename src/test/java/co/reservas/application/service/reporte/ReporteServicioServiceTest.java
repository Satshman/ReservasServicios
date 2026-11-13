package co.reservas.application.service.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.FiltroReporteServicios;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.MetricaServicio;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.ReporteDesempenoServicios;
import co.reservas.application.port.out.reporte.ReporteServicioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServicioServiceTest {

    @Mock
    private VerificadorPropiedad verificadorPropiedad;
    @Mock
    private ReporteServicioRepositoryPort reporteRepository;

    private ReporteServicioService service;

    private final UsuarioAutenticado usuarioProveedor = new UsuarioAutenticado(10, Rol.PROVEEDOR);
    private final Proveedor proveedor = new Proveedor(1, 10, "ClÃ­nica Central", ZoneId.of("America/Bogota"));

    @BeforeEach
    void setUp() {
        service = new ReporteServicioService(verificadorPropiedad, reporteRepository);
    }

    @Test
    @DisplayName("Dado un proveedor con mÃºltiples servicios, cuando genera el reporte de desempeÃ±o, entonces identifica el servicio mÃ¡s solicitado y conserva el orden de mayor a menor reservas")
    void generaReporteOrdenadoEIdentificaMasSolicitado() {
        // Arrange
        when(verificadorPropiedad.proveedorDe(usuarioProveedor)).thenReturn(proveedor);
        List<MetricaServicio> metricas = List.of(
                new MetricaServicio(10, "Limpieza Dental", "ACTIVO", 8L, 2L, 25.0),
                new MetricaServicio(11, "Ortodoncia", "ACTIVO", 3L, 0L, 0.0),
                new MetricaServicio(12, "Blanqueamiento", "ACTIVO", 0L, 0L, 0.0)
        );
        when(reporteRepository.obtenerMetricasPorProveedor(eq(1), any(), any())).thenReturn(metricas);

        FiltroReporteServicios filtro = new FiltroReporteServicios(
                1, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        // Act
        ReporteDesempenoServicios reporte = service.generar(usuarioProveedor, filtro);

        // Assert
        assertNotNull(reporte.servicioMasSolicitado());
        assertEquals(10, reporte.servicioMasSolicitado().idServicio());
        assertEquals("Limpieza Dental", reporte.servicioMasSolicitado().nombreServicio());
        assertEquals(8L, reporte.servicioMasSolicitado().totalReservas());
        assertEquals(3, reporte.servicios().size());
        assertEquals(0L, reporte.servicios().get(2).totalReservas());
    }

    @Test
    @DisplayName("Dado un proveedor cuyos servicios no tienen reservas en el periodo, cuando genera el reporte, entonces muestra mÃ©tricas en cero y servicioMasSolicitado nulo sin error")
    void serviciosSinReservasEnCero() {
        // Arrange
        when(verificadorPropiedad.proveedorDe(usuarioProveedor)).thenReturn(proveedor);
        List<MetricaServicio> metricas = List.of(
                new MetricaServicio(10, "Limpieza Dental", "ACTIVO", 0L, 0L, 0.0)
        );
        when(reporteRepository.obtenerMetricasPorProveedor(eq(1), any(), any())).thenReturn(metricas);

        FiltroReporteServicios filtro = new FiltroReporteServicios(1, null, null);

        // Act
        ReporteDesempenoServicios reporte = service.generar(usuarioProveedor, filtro);

        // Assert
        assertNull(reporte.servicioMasSolicitado());
        assertEquals(1, reporte.servicios().size());
        assertEquals(0L, reporte.servicios().getFirst().totalReservas());
        assertEquals(0.0, reporte.servicios().getFirst().tasaCancelacion());
    }

    @Test
    @DisplayName("Dado un proveedor autenticado, cuando intenta generar el reporte de servicios de otro proveedor, entonces el sistema responde con ACCESO_DENEGADO")
    void rechazaReporteDeOtroProveedor() {
        // Arrange
        when(verificadorPropiedad.proveedorDe(usuarioProveedor)).thenReturn(proveedor);
        FiltroReporteServicios filtro = new FiltroReporteServicios(999, null, null);

        // Act & Assert
        ExcepcionNegocio ex = assertThrows(ExcepcionNegocio.class,
                () -> service.generar(usuarioProveedor, filtro));
        assertEquals(CodigoError.ACCESO_DENEGADO, ex.getCodigo());
    }

    @Test
    @DisplayName("Dada una fecha final anterior a la inicial, cuando se solicita el reporte de servicios, entonces responde con VALIDACION_FALLIDA")
    void rechazaRangoFechasInvalido() {
        // Arrange
        FiltroReporteServicios filtro = new FiltroReporteServicios(
                1, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10));

        // Act & Assert
        ExcepcionNegocio ex = assertThrows(ExcepcionNegocio.class,
                () -> service.generar(usuarioProveedor, filtro));
        assertEquals(CodigoError.VALIDACION_FALLIDA, ex.getCodigo());
    }
}

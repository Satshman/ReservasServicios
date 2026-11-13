package co.reservas.application.service.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase.FiltroReporteOcupacion;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase.ReporteOcupacion;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.reporte.TipoDesgloseOcupacion;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteOcupacionServiceTest {

    @Mock
    private VerificadorPropiedad verificadorPropiedad;
    @Mock
    private AgendaRepositoryPort agenda;
    @Mock
    private ReservaRepositoryPort reservas;

    private ReporteOcupacionService service;

    private final UsuarioAutenticado usuarioProveedor = new UsuarioAutenticado(10, Rol.PROVEEDOR);
    private final Proveedor proveedor = new Proveedor(1, 10, "ClÃ­nica Central", ZoneId.of("America/Bogota"));
    private final Servicio servicio = new Servicio(
            100, 1, 1, EstadoServicio.ACTIVO, "Consulta General", "Desc", Duration.ofMinutes(60), 2);

    @BeforeEach
    void setUp() {
        service = new ReporteOcupacionService(verificadorPropiedad, agenda, reservas);
    }

    @Test
    @DisplayName("Dado un servicio con agenda y reservas confirmadas, cuando se solicita el reporte por dÃ­a, entonces calcula el porcentaje de ocupaciÃ³n")
    void calculaOcupacionPorDia() {
        // Arrange
        LocalDate lunes = LocalDate.of(2026, 11, 16);
        ZoneId zona = ZoneId.of("America/Bogota");
        HorarioDisponible bloque = new HorarioDisponible(
                1, 100, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        Instant turno8am = ZonedDateTime.of(lunes, LocalTime.of(8, 0), zona).toInstant();
        when(verificadorPropiedad.servicioPropio(usuarioProveedor, 100))
                .thenReturn(new ServicioPropio(servicio, proveedor));
        when(agenda.listarPorServicio(100)).thenReturn(List.of(bloque));
        when(reservas.contarConfirmadasPorTurno(eq(100), any(), any()))
                .thenReturn(Map.of(turno8am, 1L));

        FiltroReporteOcupacion filtro = new FiltroReporteOcupacion(100, lunes, lunes, TipoDesgloseOcupacion.DIA);

        // Act
        ReporteOcupacion reporte = service.generar(usuarioProveedor, filtro);

        // Assert
        assertEquals(1L, reporte.totalReservasConfirmadas());
        assertEquals(4L, reporte.totalCapacidadDisponible());
        assertEquals(25.0, reporte.porcentajeOcupacionGlobal());
        assertEquals(1, reporte.porDia().size());
        assertEquals(25.0, reporte.porDia().getFirst().porcentajeOcupacion());
        assertTrue(reporte.porBloqueHorario().isEmpty());
    }

    @Test
    @DisplayName("Dado un servicio con bloques horarios, cuando se solicita el reporte por bloque horario sin reservas, entonces muestra 0% de ocupaciÃ³n sin error")
    void calculaOcupacionCeroPorBloqueHorarioSinReservas() {
        // Arrange
        LocalDate lunes = LocalDate.of(2026, 11, 16);
        HorarioDisponible bloque = new HorarioDisponible(
                1, 100, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));

        when(verificadorPropiedad.servicioPropio(usuarioProveedor, 100))
                .thenReturn(new ServicioPropio(servicio, proveedor));
        when(agenda.listarPorServicio(100)).thenReturn(List.of(bloque));
        when(reservas.contarConfirmadasPorTurno(eq(100), any(), any())).thenReturn(Map.of());

        FiltroReporteOcupacion filtro = new FiltroReporteOcupacion(
                100, lunes, lunes, TipoDesgloseOcupacion.BLOQUE_HORARIO);

        // Act
        ReporteOcupacion reporte = service.generar(usuarioProveedor, filtro);

        // Assert
        assertEquals(0L, reporte.totalReservasConfirmadas());
        assertEquals(4L, reporte.totalCapacidadDisponible());
        assertEquals(0.0, reporte.porcentajeOcupacionGlobal());
        assertEquals(1, reporte.porBloqueHorario().size());
        assertEquals(0.0, reporte.porBloqueHorario().getFirst().porcentajeOcupacion());
    }

    @Test
    @DisplayName("Dada una fecha final anterior a la inicial, cuando se solicita el reporte de ocupaciÃ³n, entonces rechaza la solicitud con VALIDACION_FALLIDA")
    void rechazaFechaFinalAnteriorAInicial() {
        // Arrange
        LocalDate desde = LocalDate.of(2026, 11, 20);
        LocalDate hasta = LocalDate.of(2026, 11, 15);
        FiltroReporteOcupacion filtro = new FiltroReporteOcupacion(100, desde, hasta, TipoDesgloseOcupacion.DIA);

        // Act & Assert
        ExcepcionNegocio ex = assertThrows(ExcepcionNegocio.class,
                () -> service.generar(usuarioProveedor, filtro));
        assertEquals(CodigoError.VALIDACION_FALLIDA, ex.getCodigo());
    }

    @Test
    @DisplayName("Dado un servicio que pertenece a otro proveedor, cuando se solicita el reporte de ocupaciÃ³n, entonces responde con ACCESO_DENEGADO")
    void rechazaServicioDeOtroProveedor() {
        // Arrange
        LocalDate fecha = LocalDate.of(2026, 11, 16);
        when(verificadorPropiedad.servicioPropio(usuarioProveedor, 999))
                .thenThrow(VerificadorPropiedad.accesoDenegado("El servicio no pertenece al proveedor autenticado."));
        FiltroReporteOcupacion filtro = new FiltroReporteOcupacion(999, fecha, fecha, TipoDesgloseOcupacion.DIA);

        // Act & Assert
        ExcepcionNegocio ex = assertThrows(ExcepcionNegocio.class,
                () -> service.generar(usuarioProveedor, filtro));
        assertEquals(CodigoError.ACCESO_DENEGADO, ex.getCodigo());
    }
}

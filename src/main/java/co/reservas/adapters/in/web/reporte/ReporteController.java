package co.reservas.adapters.in.web.reporte;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase.FiltroReporteOcupacion;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase.ReporteOcupacion;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.FiltroReporteServicios;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.ReporteDesempenoServicios;
import co.reservas.domain.reporte.TipoDesgloseOcupacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiRutas.V1 + "/reportes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reportes", description = "Reportes de ocupación y desempeño de servicios (HU-12, HU-13)")
public class ReporteController {

    private final GenerarReporteOcupacionUseCase reporteOcupacion;
    private final GenerarReporteServiciosUseCase reporteServicios;

    public ReporteController(GenerarReporteOcupacionUseCase reporteOcupacion,
                             GenerarReporteServiciosUseCase reporteServicios) {
        this.reporteOcupacion = reporteOcupacion;
        this.reporteServicios = reporteServicios;
    }

    @GetMapping("/ocupacion")
    @Operation(summary = "Reporte de ocupación de un servicio por día o bloque horario (PROVEEDOR, HU-12)",
            description = "Calcula el porcentaje de ocupación (reservas confirmadas sobre capacidad disponible) "
                    + "desglosado por DIA o BLOQUE_HORARIO. Errores: 400 VALIDACION_FALLIDA (rango inválido), "
                    + "403 ACCESO_DENEGADO (servicio de otro proveedor), 404 SERVICIO_NO_ENCONTRADO.")
    public ReporteOcupacion ocupacion(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam Integer idServicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "DIA") TipoDesgloseOcupacion desglose) {
        return reporteOcupacion.generar(
                SesionActual.de(jwt),
                new FiltroReporteOcupacion(idServicio, desde, hasta, desglose));
    }

    @GetMapping("/servicios")
    @Operation(summary = "Reporte de desempeño de mis servicios (PROVEEDOR, HU-13)",
            description = "Muestra por servicio el total de reservas, canceladas, tasa de cancelación y cuál fue el "
                    + "más solicitado en el periodo, ordenados de mayor a menor número de reservas. Errores: "
                    + "400 VALIDACION_FALLIDA (rango inválido), 403 ACCESO_DENEGADO (otro proveedor).")
    public ReporteDesempenoServicios servicios(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer idProveedor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteServicios.generar(
                SesionActual.de(jwt),
                new FiltroReporteServicios(idProveedor, desde, hasta));
    }
}
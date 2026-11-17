package co.reservas.adapters.in.web.reserva;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.reserva.ConsultarHistorialReservasUseCase;
import co.reservas.application.port.in.reserva.FiltroHistorialReservas;
import co.reservas.application.port.in.reserva.HistorialReservaResultado;
import co.reservas.domain.reserva.EstadoReserva;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1 + "/reservas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Historial", description = "Historial de reservas y de servicios (HU-10, HU-11, HU-14)")
public class HistorialReservaController {

    private final ConsultarHistorialReservasUseCase historial;

    public HistorialReservaController(ConsultarHistorialReservasUseCase historial) {
        this.historial = historial;
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial de reservas (CLIENTE: las mías, HU-10; PROVEEDOR: las de mis servicios, HU-14)",
            description = "Reservas de la más reciente a la más antigua por fecha de inicio, cada una con sus cambios "
                    + "de estado (anterior, nuevo, quién y cuándo). Filtros opcionales: idServicio, estado (estado "
                    + "actual) y desde/hasta (fechas de inicio, inclusivas, en la zona del proveedor). Sin reservas "
                    + "responde una lista vacía. Errores: 400 VALIDACION_FALLIDA (hasta anterior a desde), "
                    + "400 SOLICITUD_INVALIDA (parámetro mal formado), 403 ACCESO_DENEGADO (servicio de otro "
                    + "proveedor), 404 SERVICIO_NO_ENCONTRADO.")
    public List<HistorialReservaResultado> consultar(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(required = false) EstadoReserva estado,
            @Parameter(example = "2026-10-01") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(example = "2026-10-31") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return historial.consultar(SesionActual.de(jwt), new FiltroHistorialReservas(idServicio, estado, desde, hasta));
    }

    @GetMapping("/{idReserva}/historial")
    @Operation(summary = "Historial de una reserva (CLIENTE dueño o PROVEEDOR dueño del servicio)",
            description = "Errores: 403 ACCESO_DENEGADO (reserva de otro cliente o de un servicio ajeno), "
                    + "404 RESERVA_NO_ENCONTRADA.")
    public HistorialReservaResultado consultarReserva(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Integer idReserva) {
        return historial.consultarReserva(SesionActual.de(jwt), idReserva);
    }
}

package co.reservas.adapters.in.web.reserva;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.reserva.ConsultarDisponibilidadUseCase;
import co.reservas.application.port.in.reserva.ConsultarReservasUseCase;
import co.reservas.application.port.in.reserva.CrearReservaUseCase;
import co.reservas.application.port.in.reserva.DisponibilidadResultado;
import co.reservas.application.port.in.reserva.ReservaResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1)
@Tag(name = "Reservas", description = "Disponibilidad y reservas de turnos (HU-07, HU-09)")
public class ReservaController {

    private final CrearReservaUseCase crearReserva;
    private final ConsultarReservasUseCase consultarReservas;
    private final ConsultarDisponibilidadUseCase consultarDisponibilidad;

    public ReservaController(CrearReservaUseCase crearReserva, ConsultarReservasUseCase consultarReservas,
                             ConsultarDisponibilidadUseCase consultarDisponibilidad) {
        this.crearReserva = crearReserva;
        this.consultarReservas = consultarReservas;
        this.consultarDisponibilidad = consultarDisponibilidad;
    }

    @GetMapping("/servicios/{idServicio}/disponibilidad")
    @Operation(summary = "Consultar turnos disponibles",
            description = "desde/hasta son fechas en la zona del proveedor; rango máximo de 31 días. Errores: "
                    + "400 VALIDACION_FALLIDA, 404 SERVICIO_NO_ENCONTRADO, 422 SERVICIO_NO_DISPONIBLE.")
    public DisponibilidadResultado disponibilidad(
            @PathVariable Integer idServicio,
            @Parameter(example = "2026-09-21") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @Parameter(example = "2026-09-27") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {
        return consultarDisponibilidad.consultar(idServicio, desde, hasta);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear una reserva (CLIENTE)",
            description = "Errores: 404 SERVICIO_NO_ENCONTRADO, 422 SERVICIO_NO_DISPONIBLE, 422 RESERVA_EN_EL_PASADO, "
                    + "422 HORARIO_NO_DISPONIBLE, 409 TURNO_SIN_CUPO, 409 RESERVA_SOLAPADA, "
                    + "409 RECURSO_NO_DISPONIBLE. Los errores de turno incluyen sugerencias en details.")
    public ReservaResultado crear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody ReservaRequest solicitud) {
        return crearReserva.crear(SesionActual.de(jwt), solicitud.aComando());
    }

    @GetMapping("/reservas/mias")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mis reservas (CLIENTE)")
    public List<ReservaResultado> mias(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return consultarReservas.consultarMias(SesionActual.de(jwt));
    }

    @GetMapping("/servicios/{idServicio}/reservas")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reservas de un servicio (PROVEEDOR dueño)",
            description = "Errores: 403 ACCESO_DENEGADO, 404 SERVICIO_NO_ENCONTRADO.")
    public List<ReservaResultado> porServicio(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                              @PathVariable Integer idServicio) {
        return consultarReservas.consultarPorServicio(SesionActual.de(jwt), idServicio);
    }
}

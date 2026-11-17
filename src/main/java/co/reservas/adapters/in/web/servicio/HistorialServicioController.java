package co.reservas.adapters.in.web.servicio;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.servicio.CambioServicioResultado;
import co.reservas.application.port.in.servicio.ConsultarHistorialServiciosUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1 + "/servicios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Historial", description = "Historial de reservas y de servicios (HU-10, HU-11, HU-14)")
public class HistorialServicioController {

    private final ConsultarHistorialServiciosUseCase historial;

    public HistorialServicioController(ConsultarHistorialServiciosUseCase historial) {
        this.historial = historial;
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial de cambios de mis servicios (PROVEEDOR, HU-11)",
            description = "Cambios en orden cronológico: creación, bloques de agenda creados, editados o eliminados, "
                    + "recursos asignados y cambios de estado, con el valor anterior y el nuevo, quién lo hizo y "
                    + "cuándo. Sin idServicio incluye todos los servicios del proveedor. Errores: "
                    + "403 ACCESO_DENEGADO (servicio de otro proveedor), 404 SERVICIO_NO_ENCONTRADO.")
    public List<CambioServicioResultado> consultar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(required = false) Integer idServicio) {
        return historial.consultar(SesionActual.de(jwt), idServicio);
    }
}

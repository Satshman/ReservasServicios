package co.reservas.adapters.in.web.recurso;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.recurso.GestionarRecursosUseCase;
import co.reservas.application.port.in.recurso.RecursoResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recursos", description = "Recursos compartidos entre servicios de un proveedor (HU-09)")
public class RecursoController {

    private final GestionarRecursosUseCase recursos;

    public RecursoController(GestionarRecursosUseCase recursos) {
        this.recursos = recursos;
    }

    @PostMapping("/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un recurso (PROVEEDOR)",
            description = "Errores: 404 TIPO_RECURSO_NO_ENCONTRADO, 409 RECURSO_YA_REGISTRADO.")
    public RecursoResultado crear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody RecursoRequest solicitud) {
        return recursos.crear(SesionActual.de(jwt), solicitud.aComando());
    }

    @GetMapping("/recursos")
    @Operation(summary = "Listar los recursos del proveedor autenticado")
    public List<RecursoResultado> listar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return recursos.listar(SesionActual.de(jwt));
    }

    @PutMapping("/servicios/{idServicio}/recursos")
    @Operation(summary = "Asignar recursos a un servicio (PROVEEDOR dueño)",
            description = "Reemplaza las asignaciones. Errores: 403 ACCESO_DENEGADO, 404 SERVICIO_NO_ENCONTRADO, "
                    + "404 RECURSO_NO_ENCONTRADO, 400 VALIDACION_FALLIDA (recurso inactivo).")
    public List<RecursoResultado> asignar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Integer idServicio,
                                          @Valid @RequestBody AsignarRecursosRequest solicitud) {
        return recursos.asignarAServicio(SesionActual.de(jwt), idServicio, solicitud.idsRecursos());
    }
}

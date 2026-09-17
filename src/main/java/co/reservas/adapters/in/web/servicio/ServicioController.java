package co.reservas.adapters.in.web.servicio;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.servicio.GestionarServiciosUseCase;
import co.reservas.application.port.in.servicio.ServicioResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1 + "/servicios")
@Tag(name = "Servicios", description = "Servicios ofrecidos por los proveedores")
public class ServicioController {

    private final GestionarServiciosUseCase servicios;

    public ServicioController(GestionarServiciosUseCase servicios) {
        this.servicios = servicios;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear un servicio (PROVEEDOR)",
            description = "Errores: 400 VALIDACION_FALLIDA, 404 CATEGORIA_NO_ENCONTRADA, 409 SERVICIO_YA_REGISTRADO.")
    public ResponseEntity<ServicioResultado> crear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody ServicioRequest solicitud) {
        ServicioResultado creado = servicios.crear(SesionActual.de(jwt), solicitud.aComando());
        return ResponseEntity.created(URI.create(ApiRutas.V1 + "/servicios/" + creado.id())).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar servicios activos", description = "Filtros opcionales por proveedor y categoría.")
    public List<ServicioResultado> listar(@RequestParam(required = false) Integer idProveedor,
                                          @RequestParam(required = false) Integer idCategoria) {
        return servicios.listar(idProveedor, idCategoria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un servicio", description = "Errores: 404 SERVICIO_NO_ENCONTRADO.")
    public ServicioResultado consultar(@PathVariable Integer id) {
        return servicios.consultar(id);
    }
}

package co.reservas.adapters.in.web.agenda;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase;
import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase.CrearExcepcionComando;
import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase.ExcepcionResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1)
@Tag(name = "Agenda", description = "Gestión de bloques horarios y excepciones de disponibilidad")
public class ExcepcionDisponibilidadController {

    private final GestionarExcepcionesDisponibilidadUseCase excepciones;

    public ExcepcionDisponibilidadController(GestionarExcepcionesDisponibilidadUseCase excepciones) {
        this.excepciones = excepciones;
    }

    @PostMapping("/servicios/{idServicio}/excepciones")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Registrar una excepción de disponibilidad en un servicio (PROVEEDOR)")
    public ExcepcionResultado crear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Integer idServicio,
                                    @Valid @RequestBody CrearExcepcionRequest request) {
        return excepciones.crear(
                SesionActual.de(jwt),
                idServicio,
                new CrearExcepcionComando(request.fecha(), request.horaInicio(), request.horaFin(), request.motivo())
        );
    }

    @GetMapping("/servicios/{idServicio}/excepciones")
    @Operation(summary = "Consultar excepciones de disponibilidad de un servicio")
    public List<ExcepcionResultado> listar(@PathVariable Integer idServicio) {
        return excepciones.listarPorServicio(idServicio);
    }

    @DeleteMapping("/excepciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar una excepción de disponibilidad (PROVEEDOR)")
    public void eliminar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                         @PathVariable Integer id) {
        excepciones.eliminar(SesionActual.de(jwt), id);
    }

    public record CrearExcepcionRequest(
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            @Size(max = 50, message = "El motivo no puede superar los 50 caracteres") String motivo
    ) {
    }
}
package co.reservas.adapters.in.web.agenda;

import co.reservas.adapters.in.web.ApiRutas;
import co.reservas.adapters.in.web.auth.SesionActual;
import co.reservas.application.port.in.agenda.CambioHorarioResultado;
import co.reservas.application.port.in.agenda.ConsultarHorariosUseCase;
import co.reservas.application.port.in.agenda.CrearHorarioUseCase;
import co.reservas.application.port.in.agenda.EditarHorarioUseCase;
import co.reservas.application.port.in.agenda.EliminarHorarioUseCase;
import co.reservas.application.port.in.agenda.HorarioResultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiRutas.V1)
@Tag(name = "Agenda", description = "Bloques semanales de atención de cada servicio (HU-04, HU-05, HU-06)")
public class AgendaController {

    private final CrearHorarioUseCase crearHorario;
    private final ConsultarHorariosUseCase consultarHorarios;
    private final EditarHorarioUseCase editarHorario;
    private final EliminarHorarioUseCase eliminarHorario;

    public AgendaController(CrearHorarioUseCase crearHorario, ConsultarHorariosUseCase consultarHorarios,
                            EditarHorarioUseCase editarHorario, EliminarHorarioUseCase eliminarHorario) {
        this.crearHorario = crearHorario;
        this.consultarHorarios = consultarHorarios;
        this.editarHorario = editarHorario;
        this.eliminarHorario = eliminarHorario;
    }

    @PostMapping("/servicios/{idServicio}/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear bloques de agenda (PROVEEDOR dueño)",
            description = "Crea un bloque por día, todo o nada. Errores: 403 ACCESO_DENEGADO, "
                    + "404 SERVICIO_NO_ENCONTRADO, 409 HORARIO_SOLAPADO, 422 BLOQUE_MENOR_A_DURACION.")
    public List<HorarioResultado> crear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Integer idServicio,
                                        @Valid @RequestBody CrearHorarioRequest solicitud) {
        return crearHorario.crear(SesionActual.de(jwt), idServicio, solicitud.aComando());
    }

    @GetMapping("/servicios/{idServicio}/horarios")
    @Operation(summary = "Consultar la agenda de un servicio", description = "Errores: 404 SERVICIO_NO_ENCONTRADO.")
    public List<HorarioResultado> consultar(@PathVariable Integer idServicio) {
        return consultarHorarios.consultar(idServicio);
    }

    @PutMapping("/horarios/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Editar un bloque de agenda (PROVEEDOR dueño)",
            description = "Si afecta reservas confirmadas y confirmar=false responde 409 HORARIO_CON_RESERVAS con "
                    + "la lista; con confirmar=true aplica el cambio y las reservas se mantienen.")
    public CambioHorarioResultado editar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                         @PathVariable Integer id,
                                         @RequestParam(defaultValue = "false") boolean confirmar,
                                         @Valid @RequestBody EditarHorarioRequest solicitud) {
        return editarHorario.editar(SesionActual.de(jwt), id, solicitud.aComando(), confirmar);
    }

    @DeleteMapping("/horarios/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar un bloque de agenda (PROVEEDOR dueño)",
            description = "Si hay reservas confirmadas futuras en el bloque y confirmar=false responde 409 "
                    + "HORARIO_CON_RESERVAS; con confirmar=true elimina el bloque y las reservas se mantienen.")
    public EliminacionHorarioResponse eliminar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Integer id,
                                               @RequestParam(defaultValue = "false") boolean confirmar) {
        CambioHorarioResultado resultado = eliminarHorario.eliminar(SesionActual.de(jwt), id, confirmar);
        return new EliminacionHorarioResponse(resultado.reservasAfectadas());
    }
}

package co.reservas.application.service.reserva;

import co.reservas.application.port.in.reserva.ConsultarDisponibilidadUseCase;
import co.reservas.application.port.in.reserva.DisponibilidadResultado;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DisponibilidadService implements ConsultarDisponibilidadUseCase {

    static final long MAXIMO_DIAS_CONSULTA = 31;

    private final ServicioRepositoryPort servicios;
    private final UsuarioRepositoryPort usuarios;
    private final AgendaRepositoryPort agenda;
    private final RecursoRepositoryPort recursos;
    private final CalculadoraDisponibilidad calculadora;
    private final Clock clock;

    public DisponibilidadService(ServicioRepositoryPort servicios, UsuarioRepositoryPort usuarios,
                                 AgendaRepositoryPort agenda, RecursoRepositoryPort recursos,
                                 CalculadoraDisponibilidad calculadora, Clock clock) {
        this.servicios = servicios;
        this.usuarios = usuarios;
        this.agenda = agenda;
        this.recursos = recursos;
        this.calculadora = calculadora;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResultado consultar(Integer idServicio, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Servicio servicio = servicios.buscarPorId(idServicio).orElseThrow(VerificadorPropiedad::servicioNoEncontrado);
        if (!servicio.estaActivo()) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, "El servicio no está disponible.");
        }
        Proveedor proveedor = usuarios.buscarProveedorPorId(servicio.idProveedor())
                .orElseThrow(() -> new IllegalStateException("Servicio sin proveedor: " + idServicio));
        ContextoAgenda contexto = new ContextoAgenda(servicio, proveedor.zonaHoraria(),
                agenda.listarPorServicio(idServicio), recursos.listarActivosPorServicio(idServicio));
        return new DisponibilidadResultado(idServicio, proveedor.zonaHoraria().getId(),
                calculadora.turnosDisponibles(contexto, desde, hasta, Instant.now(clock)));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null) {
            throw ExcepcionNegocio.validacion("desde", "Es obligatorio");
        }
        if (hasta == null) {
            throw ExcepcionNegocio.validacion("hasta", "Es obligatorio");
        }
        if (hasta.isBefore(desde)) {
            throw ExcepcionNegocio.validacion("hasta", "Debe ser igual o posterior a desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS_CONSULTA) {
            throw ExcepcionNegocio.validacion("hasta", "El rango máximo es de 31 días");
        }
    }
}

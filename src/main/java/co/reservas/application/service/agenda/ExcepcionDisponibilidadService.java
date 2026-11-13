package co.reservas.application.service.agenda;

import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase;
import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.agenda.ExcepcionDisponibilidadRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.ExcepcionDisponibilidad;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExcepcionDisponibilidadService implements GestionarExcepcionesDisponibilidadUseCase {

    private final ExcepcionDisponibilidadRepositoryPort excepciones;
    private final ServicioRepositoryPort servicios;
    private final VerificadorPropiedad verificador;

    public ExcepcionDisponibilidadService(ExcepcionDisponibilidadRepositoryPort excepciones,
                                          ServicioRepositoryPort servicios,
                                          VerificadorPropiedad verificador) {
        this.excepciones = excepciones;
        this.servicios = servicios;
        this.verificador = verificador;
    }

    @Override
    @Transactional
    public ExcepcionResultado crear(UsuarioAutenticado usuario, Integer idServicio, CrearExcepcionComando comando) {
        ServicioPropio propio = verificador.servicioPropio(usuario, idServicio);
        ExcepcionDisponibilidad nueva = ExcepcionDisponibilidad.crear(
                propio.servicio().id(),
                comando.fecha(),
                comando.horaInicio(),
                comando.horaFin(),
                comando.motivo()
        );
        return ExcepcionResultado.de(excepciones.guardar(nueva));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExcepcionResultado> listarPorServicio(Integer idServicio) {
        servicios.buscarPorId(idServicio).orElseThrow(VerificadorPropiedad::servicioNoEncontrado);
        return excepciones.listarPorServicio(idServicio).stream()
                .map(ExcepcionResultado::de)
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(UsuarioAutenticado usuario, Integer idExcepcion) {
        ExcepcionDisponibilidad existente = excepciones.buscarPorId(idExcepcion)
                .orElseThrow(() -> new ExcepcionNegocio(
                        CodigoError.HORARIO_NO_ENCONTRADO, "La excepción de disponibilidad no existe."));
        verificador.servicioPropio(usuario, existente.idServicio());
        excepciones.eliminar(idExcepcion);
    }
}
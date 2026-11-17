package co.reservas.application.port.in.agenda;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.domain.agenda.ExcepcionDisponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GestionarExcepcionesDisponibilidadUseCase {

    ExcepcionResultado crear(UsuarioAutenticado usuario, Integer idServicio, CrearExcepcionComando comando);

    List<ExcepcionResultado> listarPorServicio(Integer idServicio);

    void eliminar(UsuarioAutenticado usuario, Integer idExcepcion);

    record CrearExcepcionComando(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo
    ) {
    }

    record ExcepcionResultado(
            Integer id,
            Integer idServicio,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo
    ) {
        public static ExcepcionResultado de(ExcepcionDisponibilidad e) {
            return new ExcepcionResultado(
                    e.id(), e.idServicio(), e.fecha(), e.horaInicio(), e.horaFin(), e.motivo());
        }
    }
}
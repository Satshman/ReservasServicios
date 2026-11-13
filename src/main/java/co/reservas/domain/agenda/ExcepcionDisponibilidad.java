package co.reservas.domain.agenda;

import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Excepción puntual en la agenda de un servicio (día completo si horaInicio y horaFin son nulas, o franja horaria).
 */
public record ExcepcionDisponibilidad(
        Integer id,
        Integer idServicio,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo
) {

    public static ExcepcionDisponibilidad crear(Integer idServicio, LocalDate fecha, LocalTime horaInicio,
                                                LocalTime horaFin, String motivo) {
        if (fecha == null) {
            throw ExcepcionNegocio.validacion("fecha", "La fecha es obligatoria");
        }
        if ((horaInicio == null) != (horaFin == null)) {
            throw ExcepcionNegocio.validacion("horaInicio",
                    "Debe indicar tanto hora de inicio como hora de fin, o ninguna para el día completo");
        }
        if (horaInicio != null && !horaInicio.isBefore(horaFin)) {
            throw ExcepcionNegocio.validacion("horaFin", "La hora de fin debe ser posterior a la hora de inicio");
        }
        String motivoLimpio = motivo != null && !motivo.isBlank() ? motivo.trim() : null;
        if (motivoLimpio != null && motivoLimpio.length() > 50) {
            throw ExcepcionNegocio.validacion("motivo", "El motivo no puede superar los 50 caracteres");
        }
        return new ExcepcionDisponibilidad(null, idServicio, fecha, horaInicio, horaFin, motivoLimpio);
    }

    public boolean esDiaCompleto() {
        return horaInicio == null && horaFin == null;
    }

    /**
     * Indica si esta excepción bloquea un turno dado en la zona horaria del proveedor.
     */
    public boolean bloquea(Instant inicioTurno, Duration duracion, ZoneId zona) {
        LocalDateTime inicioLocal = LocalDateTime.ofInstant(inicioTurno, zona);
        if (!inicioLocal.toLocalDate().equals(fecha)) {
            return false;
        }
        if (esDiaCompleto()) {
            return true;
        }
        LocalTime inicioHora = inicioLocal.toLocalTime();
        LocalTime finHora = LocalDateTime.ofInstant(inicioTurno.plus(duracion), zona).toLocalTime();
        return inicioHora.isBefore(horaFin) && finHora.isAfter(horaInicio);
    }
}
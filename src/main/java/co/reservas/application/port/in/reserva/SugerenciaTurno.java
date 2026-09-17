package co.reservas.application.port.in.reserva;

import java.time.OffsetDateTime;

/**
 * Turno alternativo sugerido en el detalle de un error de reserva.
 */
public record SugerenciaTurno(String tipo, OffsetDateTime fechaHoraInicio, OffsetDateTime fechaHoraFin) {

    public static final String TIPO = "SUGERENCIA";

    public SugerenciaTurno(OffsetDateTime fechaHoraInicio, OffsetDateTime fechaHoraFin) {
        this(TIPO, fechaHoraInicio, fechaHoraFin);
    }
}

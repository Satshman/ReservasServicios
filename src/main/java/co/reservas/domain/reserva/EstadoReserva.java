package co.reservas.domain.reserva;

/**
 * Estados de tipo {@code RESERVA} en {@code tbl_estados}.
 */
public enum EstadoReserva {
    CONFIRMADA,
    CANCELADA,
    COMPLETADA;

    public static final String TIPO = "RESERVA";
}

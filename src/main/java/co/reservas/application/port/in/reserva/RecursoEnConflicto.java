package co.reservas.application.port.in.reserva;

/**
 * Recurso ocupado reportado en el detalle de {@code RECURSO_NO_DISPONIBLE}.
 */
public record RecursoEnConflicto(String tipo, Integer idRecurso, String nombre) {

    public static final String TIPO = "RECURSO";

    public RecursoEnConflicto(Integer idRecurso, String nombre) {
        this(TIPO, idRecurso, nombre);
    }
}

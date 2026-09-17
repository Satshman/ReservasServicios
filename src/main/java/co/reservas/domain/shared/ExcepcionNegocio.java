package co.reservas.domain.shared;

import java.util.List;

/**
 * Violación de una regla de negocio o de validación, identificada por un {@link CodigoError}.
 */
public class ExcepcionNegocio extends RuntimeException {

    private final CodigoError codigo;
    private final transient List<Object> detalles;

    public ExcepcionNegocio(CodigoError codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ExcepcionNegocio(CodigoError codigo, String mensaje, List<?> detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = List.copyOf(detalles);
    }

    public static ExcepcionNegocio validacion(String campo, String mensaje) {
        return new ExcepcionNegocio(CodigoError.VALIDACION_FALLIDA, "La solicitud contiene datos inválidos.",
                List.of(new DetalleCampo(campo, mensaje)));
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public List<Object> getDetalles() {
        return detalles;
    }
}

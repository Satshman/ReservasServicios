package co.reservas.domain.shared;

import java.util.List;

/**
 * Regla de paginación opcional para listados de historial.
 */
public final class Paginacion {

    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static <T> List<T> aplicar(List<T> elementos, Integer pagina, Integer tamano) {
        if (pagina == null && tamano == null) {
            return elementos;
        }
        int p = pagina != null ? pagina : 0;
        int t = tamano != null ? tamano : TAMANO_POR_DEFECTO;

        if (p < 0) {
            throw ExcepcionNegocio.validacion("pagina", "Debe ser mayor o igual a 0");
        }
        if (t < 1 || t > TAMANO_MAXIMO) {
            throw ExcepcionNegocio.validacion("tamano", "Debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (elementos.isEmpty()) {
            return List.of();
        }
        int desde = Math.min(p * t, elementos.size());
        int hasta = Math.min(desde + t, elementos.size());
        return elementos.subList(desde, hasta);
    }
}
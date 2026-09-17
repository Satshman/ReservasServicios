package co.reservas.adapters.out.persistence;

import co.reservas.domain.shared.ExcepcionNegocio;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Traduce la violación de una restricción única (carrera entre la validación previa y el insert) a un error de
 * negocio. Cualquier otra violación de integridad se propaga sin cambios.
 */
public final class RestriccionesUnicas {

    private RestriccionesUnicas() {
    }

    public static <T> T traducir(Supplier<T> operacion, String restriccion, Supplier<ExcepcionNegocio> error) {
        try {
            return operacion.get();
        } catch (DataIntegrityViolationException e) {
            if (restriccion.equals(nombreRestriccion(e))) {
                throw error.get();
            }
            throw e;
        }
    }

    private static String nombreRestriccion(DataIntegrityViolationException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacion && violacion.getConstraintName() != null) {
                return violacion.getConstraintName().toLowerCase(Locale.ROOT);
            }
            causa = causa.getCause();
        }
        return null;
    }
}

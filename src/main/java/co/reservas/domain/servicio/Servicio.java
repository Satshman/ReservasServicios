package co.reservas.domain.servicio;

import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.Duration;
import java.util.Objects;

/**
 * Servicio ofrecido por un proveedor. Su duración es también la duración de cada turno.
 */
public record Servicio(
        Integer id,
        Integer idProveedor,
        Integer idCategoria,
        EstadoServicio estado,
        String nombre,
        String descripcion,
        Duration duracion,
        int capacidad) {

    public static final int DURACION_MINIMA_MINUTOS = 5;
    public static final int DURACION_MAXIMA_MINUTOS = 480;

    public Servicio {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(duracion, "duracion");
        if (capacidad < 1) {
            throw ExcepcionNegocio.validacion("capacidad", "Debe ser mayor o igual a 1");
        }
    }

    public static Servicio nuevo(Integer idProveedor, Integer idCategoria, String nombre, String descripcion,
                                 Integer duracionMinutos, Integer capacidad) {
        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        if (nombreNormalizado.isEmpty() || nombreNormalizado.length() > 100) {
            throw ExcepcionNegocio.validacion("nombre", "Debe tener entre 1 y 100 caracteres");
        }
        if (duracionMinutos == null || duracionMinutos < DURACION_MINIMA_MINUTOS
                || duracionMinutos > DURACION_MAXIMA_MINUTOS) {
            throw ExcepcionNegocio.validacion("duracionMinutos", "Debe estar entre 5 y 480 minutos");
        }
        String descripcionNormalizada = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        return new Servicio(null, idProveedor, idCategoria, EstadoServicio.ACTIVO, nombreNormalizado,
                descripcionNormalizada, Duration.ofMinutes(duracionMinutos), capacidad == null ? 1 : capacidad);
    }

    public boolean estaActivo() {
        return estado == EstadoServicio.ACTIVO;
    }

    public boolean perteneceA(Integer otroIdProveedor) {
        return Objects.equals(idProveedor, otroIdProveedor);
    }
}

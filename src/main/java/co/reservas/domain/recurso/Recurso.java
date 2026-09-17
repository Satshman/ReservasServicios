package co.reservas.domain.recurso;

import co.reservas.domain.shared.ExcepcionNegocio;

import java.util.Objects;

/**
 * Recurso físico o humano de un proveedor (sala, equipo, personal) que pueden compartir varios servicios.
 */
public record Recurso(Integer id, Integer idProveedor, Integer idTipoRecurso, String nombre, boolean activo) {

    public static Recurso nuevo(Integer idProveedor, Integer idTipoRecurso, String nombre) {
        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        if (nombreNormalizado.isEmpty() || nombreNormalizado.length() > 100) {
            throw ExcepcionNegocio.validacion("nombre", "Debe tener entre 1 y 100 caracteres");
        }
        return new Recurso(null, idProveedor, idTipoRecurso, nombreNormalizado, true);
    }

    public boolean perteneceA(Integer otroIdProveedor) {
        return Objects.equals(idProveedor, otroIdProveedor);
    }
}

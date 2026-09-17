package co.reservas.domain.usuario;

import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Establecimiento que ofrece servicios. Su agenda se interpreta en su zona horaria.
 */
public record Proveedor(Integer id, Integer idUsuario, String nombreComercial, ZoneId zonaHoraria) {

    public static final String ZONA_POR_DEFECTO = "America/Bogota";

    public Proveedor {
        Objects.requireNonNull(nombreComercial, "nombreComercial");
        Objects.requireNonNull(zonaHoraria, "zonaHoraria");
    }

    public static Proveedor nuevo(Integer idUsuario, String nombreComercial, String nombreCompleto,
                                  String zonaHoraria) {
        String nombre = nombreComercial == null || nombreComercial.isBlank()
                ? nombreCompleto.trim()
                : nombreComercial.trim();
        if (nombre.length() > 100) {
            throw ExcepcionNegocio.validacion("nombreComercial", "Debe tener como máximo 100 caracteres");
        }
        return new Proveedor(null, idUsuario, nombre, resolverZona(zonaHoraria));
    }

    public static ZoneId resolverZona(String zonaHoraria) {
        String zona = zonaHoraria == null || zonaHoraria.isBlank() ? ZONA_POR_DEFECTO : zonaHoraria.trim();
        if (zona.length() > 50 || !ZoneId.getAvailableZoneIds().contains(zona)) {
            throw ExcepcionNegocio.validacion("zonaHoraria", "Debe ser un identificador de zona IANA válido");
        }
        return ZoneId.of(zona);
    }
}

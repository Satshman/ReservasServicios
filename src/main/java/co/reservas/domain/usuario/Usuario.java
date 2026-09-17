package co.reservas.domain.usuario;

import co.reservas.domain.shared.ExcepcionNegocio;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Usuario de la plataforma (cliente, proveedor o administrador).
 */
public record Usuario(
        Integer id,
        Rol rol,
        EstadoUsuario estado,
        Integer idTipoDocumento,
        String documento,
        String nombreCompleto,
        String email,
        String telefono,
        String passwordHash,
        int intentosFallidos,
        Instant bloqueadoHasta,
        Instant creadoEn) {

    public Usuario {
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(email, "email");
        if (intentosFallidos < 0) {
            throw new IllegalArgumentException("intentosFallidos no puede ser negativo");
        }
    }

    public static Usuario registrar(Rol rol, String nombreCompleto, String email, String telefono,
                                    String passwordHash, Instant ahora) {
        if (rol == Rol.ADMIN) {
            throw ExcepcionNegocio.validacion("rol", "Solo se permite registrar CLIENTE o PROVEEDOR");
        }
        String nombre = nombreCompleto == null ? "" : nombreCompleto.trim();
        if (nombre.length() < 2 || nombre.length() > 100) {
            throw ExcepcionNegocio.validacion("nombreCompleto", "Debe tener entre 2 y 100 caracteres");
        }
        String telefonoNormalizado = telefono == null || telefono.isBlank() ? null : telefono.trim();
        return new Usuario(null, rol, EstadoUsuario.PENDIENTE_VERIFICACION, null, null, nombre,
                normalizarEmail(email), telefonoNormalizado, passwordHash, 0, null, ahora);
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public Usuario conIntentosFallidos(int intentos, Instant nuevoBloqueoHasta) {
        return new Usuario(id, rol, estado, idTipoDocumento, documento, nombreCompleto, email, telefono,
                passwordHash, intentos, nuevoBloqueoHasta, creadoEn);
    }

    public Usuario conIntentosReiniciados() {
        return conIntentosFallidos(0, null);
    }

    public boolean requiereReinicioDeIntentos() {
        return intentosFallidos > 0 || bloqueadoHasta != null;
    }

    public Usuario activar() {
        return new Usuario(id, rol, EstadoUsuario.ACTIVO, idTipoDocumento, documento, nombreCompleto, email,
                telefono, passwordHash, intentosFallidos, bloqueadoHasta, creadoEn);
    }
}

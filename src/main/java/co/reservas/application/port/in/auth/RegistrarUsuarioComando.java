package co.reservas.application.port.in.auth;

import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.domain.usuario.Rol;

import java.util.List;

public record RegistrarUsuarioComando(
        String nombreCompleto,
        String email,
        String password,
        Rol rol,
        String telefono,
        String nombreComercial,
        String zonaHoraria,
        List<NuevoServicioComando> servicios) {

    public RegistrarUsuarioComando {
        servicios = servicios == null ? List.of() : List.copyOf(servicios);
    }
}

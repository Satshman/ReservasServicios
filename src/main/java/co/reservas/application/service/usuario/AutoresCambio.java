package co.reservas.application.service.usuario;

import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.usuario.Usuario;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resuelve en una sola consulta quién hizo cada cambio de un historial.
 */
@Component
public class AutoresCambio {

    private final UsuarioRepositoryPort usuarios;

    public AutoresCambio(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    public Map<Integer, AutorCambio> porId(Collection<Integer> idsUsuarios) {
        return usuarios.buscarPorIds(idsUsuarios.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Usuario::id,
                        usuario -> new AutorCambio(usuario.id(), usuario.nombreCompleto(), usuario.rol())));
    }
}

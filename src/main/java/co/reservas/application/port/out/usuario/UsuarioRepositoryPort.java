package co.reservas.application.port.out.usuario;

import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    boolean existeEmail(String emailNormalizado);

    Optional<Usuario> buscarPorId(Integer id);

    Optional<Usuario> buscarPorEmail(String emailNormalizado);

    /**
     * Busca el usuario bloqueando su fila ({@code FOR UPDATE}) para actualizar intentos de forma consistente.
     */
    Optional<Usuario> buscarPorEmailParaActualizar(String emailNormalizado);

    Usuario guardar(Usuario usuario);

    Cliente guardarCliente(Cliente cliente);

    Proveedor guardarProveedor(Proveedor proveedor);

    Optional<Cliente> buscarClientePorUsuario(Integer idUsuario);

    /**
     * Busca el cliente bloqueando su fila para serializar sus reservas concurrentes.
     */
    Optional<Cliente> bloquearClientePorUsuario(Integer idUsuario);

    Optional<Proveedor> buscarProveedorPorUsuario(Integer idUsuario);

    Optional<Proveedor> buscarProveedorPorId(Integer idProveedor);

    List<Proveedor> buscarProveedoresPorIds(Collection<Integer> idsProveedores);
}

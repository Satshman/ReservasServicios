package co.reservas.adapters.out.persistence.usuario;

import co.reservas.adapters.out.persistence.CatalogoSistema;
import co.reservas.adapters.out.persistence.RestriccionesUnicas;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Cliente;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Usuario;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class UsuarioRepositoryJpaAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarios;
    private final ClienteJpaRepository clientes;
    private final ProveedorJpaRepository proveedores;
    private final CatalogoSistema catalogo;

    public UsuarioRepositoryJpaAdapter(UsuarioJpaRepository usuarios, ClienteJpaRepository clientes,
                                       ProveedorJpaRepository proveedores, CatalogoSistema catalogo) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.catalogo = catalogo;
    }

    @Override
    public boolean existeEmail(String emailNormalizado) {
        return usuarios.existeEmail(emailNormalizado);
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarios.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return usuarios.buscarPorEmail(emailNormalizado).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmailParaActualizar(String emailNormalizado) {
        return usuarios.bloquearPorEmail(emailNormalizado).map(this::aDominio);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entidad = usuario.id() == null
                ? new UsuarioEntity()
                : usuarios.findById(usuario.id()).orElseGet(UsuarioEntity::new);
        copiar(usuario, entidad);
        UsuarioEntity guardada = RestriccionesUnicas.traducir(() -> usuarios.saveAndFlush(entidad),
                "uk_usuarios_email", () -> new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO,
                        "Ya existe una cuenta registrada con ese correo electrónico."));
        return aDominio(guardada);
    }

    @Override
    public Cliente guardarCliente(Cliente cliente) {
        ClienteEntity guardado = clientes.save(new ClienteEntity(cliente.idUsuario()));
        return new Cliente(guardado.getId(), guardado.getIdUsuario());
    }

    @Override
    public Proveedor guardarProveedor(Proveedor proveedor) {
        ProveedorEntity guardado = proveedores.save(new ProveedorEntity(proveedor.id(), proveedor.idUsuario(),
                proveedor.nombreComercial(), proveedor.zonaHoraria().getId()));
        return aDominio(guardado);
    }

    @Override
    public Optional<Cliente> buscarClientePorUsuario(Integer idUsuario) {
        return clientes.findByIdUsuario(idUsuario).map(c -> new Cliente(c.getId(), c.getIdUsuario()));
    }

    @Override
    public Optional<Cliente> bloquearClientePorUsuario(Integer idUsuario) {
        return clientes.bloquearPorIdUsuario(idUsuario).map(c -> new Cliente(c.getId(), c.getIdUsuario()));
    }

    @Override
    public Optional<Proveedor> buscarProveedorPorUsuario(Integer idUsuario) {
        return proveedores.findByIdUsuario(idUsuario).map(UsuarioRepositoryJpaAdapter::aDominio);
    }

    @Override
    public Optional<Proveedor> buscarProveedorPorId(Integer idProveedor) {
        return proveedores.findById(idProveedor).map(UsuarioRepositoryJpaAdapter::aDominio);
    }

    @Override
    public List<Proveedor> buscarProveedoresPorIds(Collection<Integer> idsProveedores) {
        if (idsProveedores.isEmpty()) {
            return List.of();
        }
        return proveedores.findAllById(idsProveedores).stream().map(UsuarioRepositoryJpaAdapter::aDominio).toList();
    }

    private void copiar(Usuario usuario, UsuarioEntity entidad) {
        entidad.setIdRol(catalogo.idRol(usuario.rol()));
        entidad.setIdEstado(catalogo.idEstado(usuario.estado()));
        entidad.setIdTipoDocumento(usuario.idTipoDocumento());
        entidad.setDocumento(usuario.documento());
        entidad.setNombreCompleto(usuario.nombreCompleto());
        entidad.setEmail(usuario.email());
        entidad.setTelefono(usuario.telefono());
        entidad.setPasswordHash(usuario.passwordHash());
        entidad.setIntentosFallidos((short) usuario.intentosFallidos());
        entidad.setBloqueadoHasta(usuario.bloqueadoHasta());
        entidad.setCreadoEn(usuario.creadoEn());
    }

    private Usuario aDominio(UsuarioEntity entidad) {
        return new Usuario(entidad.getId(), catalogo.rol(entidad.getIdRol()),
                catalogo.estadoUsuario(entidad.getIdEstado()), entidad.getIdTipoDocumento(), entidad.getDocumento(),
                entidad.getNombreCompleto(), entidad.getEmail(), entidad.getTelefono(), entidad.getPasswordHash(),
                entidad.getIntentosFallidos(), entidad.getBloqueadoHasta(), entidad.getCreadoEn());
    }

    private static Proveedor aDominio(ProveedorEntity entidad) {
        return new Proveedor(entidad.getId(), entidad.getIdUsuario(), entidad.getNombreComercial(),
                ZoneId.of(entidad.getZonaHoraria()));
    }
}

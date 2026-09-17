package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Reglas ABAC: un proveedor solo gestiona sus propios servicios.
 */
@Component
public class VerificadorPropiedad {

    private final UsuarioRepositoryPort usuarios;
    private final ServicioRepositoryPort servicios;

    public VerificadorPropiedad(UsuarioRepositoryPort usuarios, ServicioRepositoryPort servicios) {
        this.usuarios = usuarios;
        this.servicios = servicios;
    }

    public Proveedor proveedorDe(UsuarioAutenticado usuario) {
        if (usuario.rol() != Rol.PROVEEDOR) {
            throw accesoDenegado("La operación requiere un proveedor.");
        }
        return usuarios.buscarProveedorPorUsuario(usuario.idUsuario())
                .orElseThrow(() -> accesoDenegado("La operación requiere un proveedor."));
    }

    /**
     * Verifica existencia (404) y propiedad (403) bloqueando la fila del servicio.
     */
    public ServicioPropio servicioPropioBloqueado(UsuarioAutenticado usuario, Integer idServicio) {
        return verificar(usuario, idServicio, servicios::bloquearPorId);
    }

    public ServicioPropio servicioPropio(UsuarioAutenticado usuario, Integer idServicio) {
        return verificar(usuario, idServicio, servicios::buscarPorId);
    }

    private ServicioPropio verificar(UsuarioAutenticado usuario, Integer idServicio,
                                     Function<Integer, Optional<Servicio>> buscador) {
        Servicio servicio = buscador.apply(idServicio).orElseThrow(VerificadorPropiedad::servicioNoEncontrado);
        Proveedor proveedor = proveedorDe(usuario);
        if (!servicio.perteneceA(proveedor.id())) {
            throw accesoDenegado("El servicio no pertenece al proveedor autenticado.");
        }
        return new ServicioPropio(servicio, proveedor);
    }

    public static ExcepcionNegocio servicioNoEncontrado() {
        return new ExcepcionNegocio(CodigoError.SERVICIO_NO_ENCONTRADO, "El servicio no existe.");
    }

    public static ExcepcionNegocio accesoDenegado(String mensaje) {
        return new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO, mensaje);
    }
}

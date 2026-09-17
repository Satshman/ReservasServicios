package co.reservas.adapters.out.persistence;

import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce los enums de sistema (roles y estados) a los ids de sus filas semilla, buscándolos por nombre.
 * Los valores se cargan una vez y se mantienen en memoria porque no cambian en tiempo de ejecución.
 */
@Component
public class CatalogoSistema {

    private final CatalogoSistemaJpaRepository repositorio;
    private volatile Tablas tablas;

    public CatalogoSistema(CatalogoSistemaJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Integer idRol(Rol rol) {
        return buscar(tablas().idsPorRol, rol.name());
    }

    public Rol rol(Integer id) {
        return Rol.valueOf(buscar(tablas().rolesPorId, id));
    }

    public Integer idEstado(EstadoUsuario estado) {
        return idEstado(EstadoUsuario.TIPO, estado.name());
    }

    public Integer idEstado(EstadoServicio estado) {
        return idEstado(EstadoServicio.TIPO, estado.name());
    }

    public Integer idEstado(EstadoReserva estado) {
        return idEstado(EstadoReserva.TIPO, estado.name());
    }

    public Integer idEstadoReservaONulo(EstadoReserva estado) {
        return estado == null ? null : idEstado(estado);
    }

    public EstadoUsuario estadoUsuario(Integer id) {
        return EstadoUsuario.valueOf(buscar(tablas().nombresEstadoPorId, id));
    }

    public EstadoServicio estadoServicio(Integer id) {
        return EstadoServicio.valueOf(buscar(tablas().nombresEstadoPorId, id));
    }

    public EstadoReserva estadoReserva(Integer id) {
        return id == null ? null : EstadoReserva.valueOf(buscar(tablas().nombresEstadoPorId, id));
    }

    private Integer idEstado(String tipo, String nombre) {
        return buscar(tablas().idsEstadoPorClave, tipo + ":" + nombre);
    }

    private static <K, V> V buscar(Map<K, V> mapa, K clave) {
        V valor = mapa.get(clave);
        if (valor == null) {
            throw new IllegalStateException("Valor de catálogo de sistema no encontrado: " + clave);
        }
        return valor;
    }

    private Tablas tablas() {
        Tablas actuales = tablas;
        if (actuales == null) {
            synchronized (this) {
                if (tablas == null) {
                    tablas = cargar();
                }
                actuales = tablas;
            }
        }
        return actuales;
    }

    private Tablas cargar() {
        Tablas nuevas = new Tablas();
        repositorio.listarRoles().forEach(rol -> {
            nuevas.idsPorRol.put(rol.getNombre(), rol.getId());
            nuevas.rolesPorId.put(rol.getId(), rol.getNombre());
        });
        repositorio.listarEstados().forEach(estado -> {
            nuevas.idsEstadoPorClave.put(estado.getTipo() + ":" + estado.getNombre(), estado.getId());
            nuevas.nombresEstadoPorId.put(estado.getId(), estado.getNombre());
        });
        return nuevas;
    }

    private static final class Tablas {
        private final Map<String, Integer> idsPorRol = new HashMap<>();
        private final Map<Integer, String> rolesPorId = new HashMap<>();
        private final Map<String, Integer> idsEstadoPorClave = new HashMap<>();
        private final Map<Integer, String> nombresEstadoPorId = new HashMap<>();
    }
}

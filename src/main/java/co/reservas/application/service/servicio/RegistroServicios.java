package co.reservas.application.service.servicio;

import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Validación y alta de servicios, compartida por el registro de proveedores y la creación de servicios.
 */
@Component
public class RegistroServicios {

    private final ServicioRepositoryPort servicios;
    private final CatalogoRepositoryPort catalogos;

    public RegistroServicios(ServicioRepositoryPort servicios, CatalogoRepositoryPort catalogos) {
        this.servicios = servicios;
        this.catalogos = catalogos;
    }

    /**
     * Valida forma, nombres repetidos y categorías. {@code prefijoCampo} recibe el índice del servicio.
     */
    public void validar(List<NuevoServicioComando> comandos, IntFunction<String> prefijoCampo) {
        Set<String> nombres = new HashSet<>();
        for (int i = 0; i < comandos.size(); i++) {
            NuevoServicioComando comando = comandos.get(i);
            String prefijo = prefijoCampo.apply(i);
            Servicio servicio = construir(null, comando, prefijo);
            if (!nombres.add(servicio.nombre())) {
                throw ExcepcionNegocio.validacion(prefijo + "nombre", "El nombre del servicio está repetido");
            }
            if (!catalogos.existeCategoria(comando.idCategoria())) {
                throw new ExcepcionNegocio(CodigoError.CATEGORIA_NO_ENCONTRADA, "La categoría indicada no existe.",
                        List.of(new DetalleCampo(prefijo + "idCategoria",
                                "No existe la categoría " + comando.idCategoria())));
            }
        }
    }

    public List<Servicio> guardar(Integer idProveedor, List<NuevoServicioComando> comandos) {
        return comandos.stream()
                .map(comando -> servicios.guardar(construir(idProveedor, comando, "")))
                .toList();
    }

    private static Servicio construir(Integer idProveedor, NuevoServicioComando comando, String prefijo) {
        try {
            return Servicio.nuevo(idProveedor, comando.idCategoria(), comando.nombre(), comando.descripcion(),
                    comando.duracionMinutos(), comando.capacidad());
        } catch (ExcepcionNegocio e) {
            List<Object> detalles = e.getDetalles().stream()
                    .map(detalle -> detalle instanceof DetalleCampo campo
                            ? new DetalleCampo(prefijo + campo.campo(), campo.mensaje())
                            : detalle)
                    .toList();
            throw new ExcepcionNegocio(e.getCodigo(), e.getMessage(), detalles);
        }
    }
}

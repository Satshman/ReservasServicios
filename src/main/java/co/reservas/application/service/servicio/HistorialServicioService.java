package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.servicio.CambioServicioResultado;
import co.reservas.application.port.in.servicio.ConsultarHistorialServiciosUseCase;
import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.usuario.AutoresCambio;
import co.reservas.domain.servicio.HistorialServicio;
import co.reservas.domain.servicio.Servicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HistorialServicioService implements ConsultarHistorialServiciosUseCase {

    private final ServicioRepositoryPort servicios;
    private final VerificadorPropiedad verificador;
    private final AutoresCambio autores;

    public HistorialServicioService(ServicioRepositoryPort servicios, VerificadorPropiedad verificador,
                                    AutoresCambio autores) {
        this.servicios = servicios;
        this.verificador = verificador;
        this.autores = autores;
    }

    /**
     * Con un servicio indicado valida existencia (404) y propiedad (403); sin él consulta todos los servicios del
     * proveedor autenticado, activos o no.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CambioServicioResultado> consultar(UsuarioAutenticado usuario, Integer idServicio) {
        List<Servicio> propios = idServicio == null
                ? servicios.listarPorProveedor(verificador.proveedorDe(usuario).id())
                : List.of(verificador.servicioPropio(usuario, idServicio).servicio());
        Map<Integer, Servicio> porId = propios.stream()
                .collect(Collectors.toMap(Servicio::id, Function.identity()));
        List<HistorialServicio> cambios = servicios.listarHistorial(porId.keySet());
        Map<Integer, AutorCambio> autoresPorId = autores.porId(
                cambios.stream().map(HistorialServicio::idUsuario).toList());
        return cambios.stream()
                .map(cambio -> new CambioServicioResultado(cambio.id(), cambio.idServicio(),
                        porId.get(cambio.idServicio()).nombre(), cambio.tipoCambio(), cambio.valorAnterior(),
                        cambio.valorNuevo(), autoresPorId.get(cambio.idUsuario()), cambio.fechaCambio()))
                .toList();
    }
}

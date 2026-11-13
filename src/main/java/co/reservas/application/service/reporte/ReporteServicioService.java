package co.reservas.application.service.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase;
import co.reservas.application.port.out.reporte.ReporteServicioRepositoryPort;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReporteServicioService implements GenerarReporteServiciosUseCase {

    private final VerificadorPropiedad verificadorPropiedad;
    private final ReporteServicioRepositoryPort reporteRepository;

    public ReporteServicioService(VerificadorPropiedad verificadorPropiedad,
                                  ReporteServicioRepositoryPort reporteRepository) {
        this.verificadorPropiedad = verificadorPropiedad;
        this.reporteRepository = reporteRepository;
    }

    @Override
    public ReporteDesempenoServicios generar(UsuarioAutenticado usuario, FiltroReporteServicios filtro) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.hasta().isBefore(filtro.desde())) {
            throw new ExcepcionNegocio(CodigoError.VALIDACION_FALLIDA,
                    "La fecha final no puede ser anterior a la fecha inicial.");
        }

        Proveedor proveedor = verificadorPropiedad.proveedorDe(usuario);
        if (filtro.idProveedor() != null && !filtro.idProveedor().equals(proveedor.id())) {
            throw VerificadorPropiedad.accesoDenegado(
                    "No puede generar el reporte de servicios de otro proveedor.");
        }

        ZoneId zona = proveedor.zonaHoraria();
        Instant instanteDesde = filtro.desde() != null ? filtro.desde().atStartOfDay(zona).toInstant() : null;
        Instant instanteHasta = filtro.hasta() != null ? filtro.hasta().plusDays(1).atStartOfDay(zona).toInstant() : null;

        List<MetricaServicio> metricas = reporteRepository.obtenerMetricasPorProveedor(
                proveedor.id(), instanteDesde, instanteHasta);

        ServicioMasSolicitado masSolicitado = metricas.stream()
                .filter(m -> m.totalReservas() > 0)
                .findFirst()
                .map(m -> new ServicioMasSolicitado(m.idServicio(), m.nombreServicio(), m.totalReservas()))
                .orElse(null);

        return new ReporteDesempenoServicios(
                proveedor.id(),
                filtro.desde(),
                filtro.hasta(),
                masSolicitado,
                metricas
        );
    }
}
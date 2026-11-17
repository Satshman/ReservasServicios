package co.reservas.application.port.in.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.time.LocalDate;
import java.util.List;

public interface GenerarReporteServiciosUseCase {

    ReporteDesempenoServicios generar(UsuarioAutenticado usuario, FiltroReporteServicios filtro);

    record FiltroReporteServicios(
            Integer idProveedor,
            LocalDate desde,
            LocalDate hasta
    ) {
    }

    record MetricaServicio(
            Integer idServicio,
            String nombreServicio,
            String estado,
            long totalReservas,
            long reservasCanceladas,
            double tasaCancelacion
    ) {
    }

    record ServicioMasSolicitado(
            Integer idServicio,
            String nombreServicio,
            long totalReservas
    ) {
    }

    record ReporteDesempenoServicios(
            Integer idProveedor,
            LocalDate desde,
            LocalDate hasta,
            ServicioMasSolicitado servicioMasSolicitado,
            List<MetricaServicio> servicios
    ) {
    }
}
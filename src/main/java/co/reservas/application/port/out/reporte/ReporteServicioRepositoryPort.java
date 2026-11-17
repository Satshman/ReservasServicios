package co.reservas.application.port.out.reporte;

import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.MetricaServicio;

import java.time.Instant;
import java.util.List;

public interface ReporteServicioRepositoryPort {

    List<MetricaServicio> obtenerMetricasPorProveedor(Integer idProveedor, Instant desde, Instant hasta);
}
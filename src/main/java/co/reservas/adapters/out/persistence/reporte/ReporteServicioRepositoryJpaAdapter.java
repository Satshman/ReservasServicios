package co.reservas.adapters.out.persistence.reporte;

import co.reservas.application.port.in.reporte.GenerarReporteServiciosUseCase.MetricaServicio;
import co.reservas.application.port.out.reporte.ReporteServicioRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class ReporteServicioRepositoryJpaAdapter implements ReporteServicioRepositoryPort {

    private static final String SQL_FUNCION_REPORTE = """
            SELECT id_servicio, nombre_servicio, estado_servicio,
                   total_reservas, reservas_canceladas, tasa_cancelacion
            FROM fn_reporte_desempeno_servicios(
                CAST(:idProveedor AS integer),
                CAST(:desde AS timestamptz),
                CAST(:hasta AS timestamptz)
            )
            """;

    private final EntityManager entityManager;

    public ReporteServicioRepositoryJpaAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<MetricaServicio> obtenerMetricasPorProveedor(Integer idProveedor, Instant desde, Instant hasta) {
        OffsetDateTime odtDesde = desde != null ? desde.atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime odtHasta = hasta != null ? hasta.atOffset(ZoneOffset.UTC) : null;

        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(SQL_FUNCION_REPORTE)
                .setParameter("idProveedor", idProveedor)
                .setParameter("desde", odtDesde)
                .setParameter("hasta", odtHasta)
                .getResultList();

        return filas.stream()
                .map(fila -> new MetricaServicio(
                        ((Number) fila[0]).intValue(),
                        (String) fila[1],
                        (String) fila[2],
                        ((Number) fila[3]).longValue(),
                        ((Number) fila[4]).longValue(),
                        fila[5] instanceof BigDecimal bd ? bd.doubleValue() : ((Number) fila[5]).doubleValue()
                ))
                .toList();
    }
}
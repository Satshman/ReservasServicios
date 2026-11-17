-- Función almacenada para el reporte de desempeño de servicios (HU-13)
-- Cumple el lineamiento de base de datos (docs/modelo-datos-y-paquetes.md §5)

CREATE OR REPLACE FUNCTION fn_reporte_desempeno_servicios(
    p_id_proveedor integer,
    p_desde        timestamptz DEFAULT NULL,
    p_hasta        timestamptz DEFAULT NULL
)
RETURNS TABLE (
    id_servicio         integer,
    nombre_servicio     varchar(100),
    estado_servicio     varchar(30),
    total_reservas      bigint,
    reservas_canceladas bigint,
    tasa_cancelacion    numeric(5, 2)
)
LANGUAGE sql
STABLE
AS $$
    SELECT s.id AS id_servicio,
           s.nombre AS nombre_servicio,
           es.nombre AS estado_servicio,
           COUNT(r.id) AS total_reservas,
           COUNT(r.id) FILTER (WHERE er.nombre = 'CANCELADA') AS reservas_canceladas,
           CASE
               WHEN COUNT(r.id) = 0 THEN 0.00
               ELSE ROUND(
                   (COUNT(r.id) FILTER (WHERE er.nombre = 'CANCELADA')::numeric * 100.0) / COUNT(r.id),
                   2
               )
           END AS tasa_cancelacion
    FROM tbl_servicios s
    JOIN tbl_estados es ON es.id = s.id_estado
    LEFT JOIN tbl_reservas r
           ON r.id_servicio = s.id
          AND (p_desde IS NULL OR r.fecha_hora_inicio >= p_desde)
          AND (p_hasta IS NULL OR r.fecha_hora_inicio < p_hasta)
    LEFT JOIN tbl_estados er ON er.id = r.id_estado
    WHERE s.id_proveedor = p_id_proveedor
    GROUP BY s.id, s.nombre, es.nombre
    ORDER BY total_reservas DESC, s.id ASC;
$$;
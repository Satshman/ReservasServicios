-- Historial de servicios (HU-11) e índices para consultar historiales (HU-10, HU-11, HU-14)
-- (docs/modelo-datos-y-paquetes.md §1)

CREATE TABLE tbl_historial_servicios (
    id             integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_servicio    integer     NOT NULL REFERENCES tbl_servicios (id),
    tipo_cambio    varchar(30) NOT NULL,
    valor_anterior text,
    valor_nuevo    text,
    id_usuario     integer     NOT NULL REFERENCES tbl_usuarios (id),
    fecha_cambio   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_historial_servicios_tipo CHECK (tipo_cambio IN ('CREACION', 'HORARIO_CREADO', 'HORARIO_EDITADO',
        'HORARIO_ELIMINADO', 'RECURSOS_ASIGNADOS', 'ESTADO_CAMBIADO')),
    CONSTRAINT ck_historial_servicios_valor CHECK (valor_anterior IS NOT NULL OR valor_nuevo IS NOT NULL)
);

-- Historial de los servicios de un proveedor en orden cronológico (HU-11)
CREATE INDEX ix_historial_servicios_servicio_fecha ON tbl_historial_servicios (id_servicio, fecha_cambio);

-- Cambios de estado de las reservas consultadas (HU-10, HU-14)
CREATE INDEX ix_historial_reservas_reserva_fecha ON tbl_historial_reservas (id_reserva, fecha_cambio);

-- Los servicios creados antes de esta migración reciben su evento de creación, atribuido al proveedor y fechado
-- con su registro. El formato coincide con Servicio.descripcionOferta().
INSERT INTO tbl_historial_servicios (id_servicio, tipo_cambio, valor_nuevo, id_usuario, fecha_cambio)
SELECT s.id,
       'CREACION',
       s.nombre || ' · ' || (extract(epoch FROM s.duracion) / 60)::integer || ' min · capacidad ' || s.capacidad,
       p.id_usuario,
       u.creado_en
FROM tbl_servicios s
JOIN tbl_proveedores p ON p.id = s.id_proveedor
JOIN tbl_usuarios u ON u.id = p.id_usuario;

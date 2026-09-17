-- Datos semilla (docs/modelo-datos-y-paquetes.md §1.3)

INSERT INTO tbl_roles (nombre) VALUES ('CLIENTE'), ('PROVEEDOR'), ('ADMIN');

INSERT INTO tbl_tipos_documento (nombre) VALUES ('CC'), ('CE'), ('TI'), ('PASAPORTE'), ('NIT');

INSERT INTO tbl_tipos_estado (nombre) VALUES ('USUARIO'), ('SERVICIO'), ('RESERVA');

INSERT INTO tbl_estados (id_tipo_estado, nombre)
SELECT te.id, e.nombre
FROM (VALUES ('USUARIO', 'PENDIENTE_VERIFICACION'),
             ('USUARIO', 'ACTIVO'),
             ('USUARIO', 'INACTIVO'),
             ('SERVICIO', 'ACTIVO'),
             ('SERVICIO', 'INACTIVO'),
             ('RESERVA', 'CONFIRMADA'),
             ('RESERVA', 'CANCELADA'),
             ('RESERVA', 'COMPLETADA')) AS e (tipo, nombre)
JOIN tbl_tipos_estado te ON te.nombre = e.tipo;

INSERT INTO tbl_categorias_servicio (nombre)
VALUES ('SALUD'), ('BELLEZA'), ('DEPORTE'), ('EDUCACION'), ('CONSULTORIA'), ('OTRO');

INSERT INTO tbl_tipos_recurso (nombre) VALUES ('SALA'), ('EQUIPO'), ('PERSONAL');

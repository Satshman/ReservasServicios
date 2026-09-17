-- Esquema inicial (docs/modelo-datos-y-paquetes.md §1)

-- Catálogos
CREATE TABLE tbl_roles (
    id     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
);

CREATE TABLE tbl_tipos_documento (
    id     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    CONSTRAINT uk_tipos_documento_nombre UNIQUE (nombre)
);

CREATE TABLE tbl_tipos_estado (
    id     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    CONSTRAINT uk_tipos_estado_nombre UNIQUE (nombre)
);

CREATE TABLE tbl_estados (
    id             integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tipo_estado integer     NOT NULL REFERENCES tbl_tipos_estado (id),
    nombre         varchar(30) NOT NULL,
    CONSTRAINT uk_estados_tipo_nombre UNIQUE (id_tipo_estado, nombre)
);

CREATE TABLE tbl_categorias_servicio (
    id     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    CONSTRAINT uk_categorias_servicio_nombre UNIQUE (nombre)
);

CREATE TABLE tbl_tipos_recurso (
    id     integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    CONSTRAINT uk_tipos_recurso_nombre UNIQUE (nombre)
);

-- Usuarios
CREATE TABLE tbl_usuarios (
    id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_rol            integer      NOT NULL REFERENCES tbl_roles (id),
    id_estado         integer      NOT NULL REFERENCES tbl_estados (id),
    id_tipo_documento integer      REFERENCES tbl_tipos_documento (id),
    documento         varchar(20),
    nombre_completo   varchar(100) NOT NULL,
    email             varchar(255) NOT NULL,
    telefono          varchar(20),
    password_hash     varchar(255) NOT NULL,
    intentos_fallidos smallint     NOT NULL DEFAULT 0,
    bloqueado_hasta   timestamptz,
    creado_en         timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuarios_documento_tipo CHECK ((id_tipo_documento IS NULL) = (documento IS NULL)),
    CONSTRAINT ck_usuarios_intentos_fallidos CHECK (intentos_fallidos >= 0),
    CONSTRAINT uk_usuarios_documento UNIQUE (id_tipo_documento, documento)
);

-- Login y detección de correo duplicado
CREATE UNIQUE INDEX uk_usuarios_email ON tbl_usuarios (lower(email));

CREATE TABLE tbl_tokens_verificacion (
    id         integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario integer     NOT NULL REFERENCES tbl_usuarios (id),
    token_hash varchar(64) NOT NULL,
    expira_en  timestamptz NOT NULL,
    usado_en   timestamptz,
    creado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_tokens_verificacion_hash UNIQUE (token_hash)
);

-- Reenvío e invalidación de tokens
CREATE INDEX ix_tokens_verificacion_usuario ON tbl_tokens_verificacion (id_usuario);

CREATE TABLE tbl_clientes (
    id         integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario integer NOT NULL REFERENCES tbl_usuarios (id),
    CONSTRAINT uk_clientes_usuario UNIQUE (id_usuario)
);

CREATE TABLE tbl_proveedores (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario       integer      NOT NULL REFERENCES tbl_usuarios (id),
    nombre_comercial varchar(100) NOT NULL,
    zona_horaria     varchar(50)  NOT NULL DEFAULT 'America/Bogota',
    CONSTRAINT uk_proveedores_usuario UNIQUE (id_usuario)
);

-- Servicios y agenda
CREATE TABLE tbl_servicios (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proveedor integer      NOT NULL REFERENCES tbl_proveedores (id),
    id_categoria integer      NOT NULL REFERENCES tbl_categorias_servicio (id),
    id_estado    integer      NOT NULL REFERENCES tbl_estados (id),
    nombre       varchar(100) NOT NULL,
    descripcion  text,
    duracion     interval     NOT NULL,
    capacidad    smallint     NOT NULL DEFAULT 1,
    CONSTRAINT uk_servicios_proveedor_nombre UNIQUE (id_proveedor, nombre),
    CONSTRAINT ck_servicios_duracion CHECK (duracion > interval '0' AND duracion <= interval '8 hours'),
    CONSTRAINT ck_servicios_capacidad CHECK (capacidad >= 1)
);

-- Listados del proveedor
CREATE INDEX ix_servicios_proveedor ON tbl_servicios (id_proveedor);

CREATE TABLE tbl_horarios_disponibles (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_servicio integer  NOT NULL REFERENCES tbl_servicios (id),
    dia_semana  smallint NOT NULL,
    hora_inicio time     NOT NULL,
    hora_fin    time     NOT NULL,
    CONSTRAINT ck_horarios_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horarios_rango CHECK (hora_inicio < hora_fin)
);

-- Generar turnos y validar solapes
CREATE INDEX ix_horarios_servicio_dia ON tbl_horarios_disponibles (id_servicio, dia_semana);

CREATE TABLE tbl_excepciones_disponibilidad (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_servicio integer NOT NULL REFERENCES tbl_servicios (id),
    fecha       date    NOT NULL,
    hora_inicio time,
    hora_fin    time,
    motivo      varchar(50),
    CONSTRAINT ck_excepciones_rango CHECK (
        (hora_inicio IS NULL AND hora_fin IS NULL)
        OR (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_inicio < hora_fin)
    )
);

-- Recursos
CREATE TABLE tbl_recursos (
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proveedor    integer      NOT NULL REFERENCES tbl_proveedores (id),
    id_tipo_recurso integer      NOT NULL REFERENCES tbl_tipos_recurso (id),
    nombre          varchar(100) NOT NULL,
    activo          boolean      NOT NULL DEFAULT true,
    CONSTRAINT uk_recursos_proveedor_nombre UNIQUE (id_proveedor, nombre)
);

-- Listados del proveedor
CREATE INDEX ix_recursos_proveedor ON tbl_recursos (id_proveedor);

CREATE TABLE tbl_servicios_recursos (
    id_servicio integer NOT NULL REFERENCES tbl_servicios (id),
    id_recurso  integer NOT NULL REFERENCES tbl_recursos (id),
    PRIMARY KEY (id_servicio, id_recurso)
);

-- Reservas
CREATE TABLE tbl_reservas (
    id                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente        integer     NOT NULL REFERENCES tbl_clientes (id),
    id_servicio       integer     NOT NULL REFERENCES tbl_servicios (id),
    id_estado         integer     NOT NULL REFERENCES tbl_estados (id),
    fecha_hora_inicio timestamptz NOT NULL,
    fecha_hora_fin    timestamptz NOT NULL,
    creado_en         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_reservas_rango CHECK (fecha_hora_fin > fecha_hora_inicio)
);

-- Cupo por turno, disponibilidad y conflictos al editar agenda
CREATE INDEX ix_reservas_servicio_inicio ON tbl_reservas (id_servicio, fecha_hora_inicio);
-- "Mis reservas" y solape del cliente
CREATE INDEX ix_reservas_cliente_inicio ON tbl_reservas (id_cliente, fecha_hora_inicio);

CREATE TABLE tbl_reservas_recursos (
    id_reserva integer NOT NULL REFERENCES tbl_reservas (id),
    id_recurso integer NOT NULL REFERENCES tbl_recursos (id),
    PRIMARY KEY (id_reserva, id_recurso)
);

-- Conflictos de recurso (HU-09)
CREATE INDEX ix_reservas_recursos_recurso ON tbl_reservas_recursos (id_recurso);

CREATE TABLE tbl_historial_reservas (
    id                 integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_reserva         integer     NOT NULL REFERENCES tbl_reservas (id),
    id_estado_anterior integer     REFERENCES tbl_estados (id),
    id_estado_nuevo    integer     NOT NULL REFERENCES tbl_estados (id),
    id_usuario         integer     NOT NULL REFERENCES tbl_usuarios (id),
    fecha_cambio       timestamptz NOT NULL DEFAULT now()
);

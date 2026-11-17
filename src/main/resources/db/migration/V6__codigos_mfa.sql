-- Códigos MFA para autenticación de segundo factor de administradores (Sprint 3)

CREATE TABLE tbl_codigos_mfa (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario  integer     NOT NULL REFERENCES tbl_usuarios (id),
    codigo_hash varchar(64) NOT NULL,
    expira_en   timestamptz NOT NULL,
    usado_en    timestamptz,
    creado_en   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_codigos_mfa_usuario ON tbl_codigos_mfa (id_usuario);
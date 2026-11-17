-- Tokens de refresco para rotación y revocación de sesiones (Sprint 3)
-- (docs/modelo-datos-y-paquetes.md §5)

CREATE TABLE tbl_tokens_refresco (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario  integer     NOT NULL REFERENCES tbl_usuarios (id),
    token_hash  varchar(64) NOT NULL,
    expira_en   timestamptz NOT NULL,
    revocado_en timestamptz,
    creado_en   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_tokens_refresco_hash UNIQUE (token_hash)
);

-- Revocación de sesiones por usuario
CREATE INDEX ix_tokens_refresco_usuario ON tbl_tokens_refresco (id_usuario);
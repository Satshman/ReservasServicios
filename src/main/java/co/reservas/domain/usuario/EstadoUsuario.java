package co.reservas.domain.usuario;

/**
 * Estados de tipo {@code USUARIO} en {@code tbl_estados}.
 */
public enum EstadoUsuario {
    PENDIENTE_VERIFICACION,
    ACTIVO,
    INACTIVO;

    public static final String TIPO = "USUARIO";
}

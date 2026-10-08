package co.reservas.domain.servicio;

/**
 * Tipos de cambio registrados en el historial de un servicio (HU-11). Coinciden con el {@code CHECK} de
 * {@code tbl_historial_servicios.tipo_cambio}.
 */
public enum TipoCambioServicio {
    CREACION,
    HORARIO_CREADO,
    HORARIO_EDITADO,
    HORARIO_ELIMINADO,
    RECURSOS_ASIGNADOS,
    ESTADO_CAMBIADO
}

package co.reservas.domain.shared;

/**
 * Detalle de un error de validación asociado a un campo de la solicitud.
 */
public record DetalleCampo(String campo, String mensaje) {
}

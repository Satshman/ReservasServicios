package co.reservas.application.port.in.servicio;

public record NuevoServicioComando(String nombre, String descripcion, Integer idCategoria, Integer duracionMinutos,
                                   Integer capacidad) {
}

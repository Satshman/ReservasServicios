package co.reservas.application.port.in.servicio;

import co.reservas.domain.servicio.EstadoServicio;

public record ServicioResultado(
        Integer id,
        Integer idProveedor,
        String nombreProveedor,
        String zonaHoraria,
        Integer idCategoria,
        String nombre,
        String descripcion,
        long duracionMinutos,
        int capacidad,
        EstadoServicio estado) {
}

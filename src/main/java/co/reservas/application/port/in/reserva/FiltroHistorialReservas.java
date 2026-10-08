package co.reservas.application.port.in.reserva;

import co.reservas.domain.reserva.EstadoReserva;

import java.time.LocalDate;

/**
 * Filtros opcionales (nulos = sin filtro). {@code estado} es el estado actual de la reserva; {@code desde} y
 * {@code hasta} son fechas de inicio de la reserva, inclusivas, en la zona horaria de su proveedor.
 */
public record FiltroHistorialReservas(Integer idServicio, EstadoReserva estado, LocalDate desde, LocalDate hasta) {

    public static FiltroHistorialReservas sinFiltros() {
        return new FiltroHistorialReservas(null, null, null, null);
    }
}

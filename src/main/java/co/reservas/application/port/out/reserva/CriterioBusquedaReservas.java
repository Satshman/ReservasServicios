package co.reservas.application.port.out.reserva;

import co.reservas.domain.reserva.EstadoReserva;

import java.time.Instant;
import java.util.Set;

/**
 * Criterio de búsqueda de reservas; los campos nulos no filtran. El inicio de la reserva se filtra en
 * [{@code inicioDesde}, {@code inicioHasta}).
 */
public record CriterioBusquedaReservas(Integer idCliente, Set<Integer> idsServicios, EstadoReserva estado,
                                       Instant inicioDesde, Instant inicioHasta) {

    public CriterioBusquedaReservas {
        idsServicios = idsServicios == null ? null : Set.copyOf(idsServicios);
    }
}

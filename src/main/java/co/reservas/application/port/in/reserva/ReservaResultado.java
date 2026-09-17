package co.reservas.application.port.in.reserva;

import co.reservas.application.port.in.recurso.RecursoResultado;
import co.reservas.domain.reserva.EstadoReserva;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public record ReservaResultado(
        Integer id,
        Integer idServicio,
        String nombreServicio,
        Integer idCliente,
        EstadoReserva estado,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        List<RecursoResultado> recursos,
        Instant creadoEn) {
}

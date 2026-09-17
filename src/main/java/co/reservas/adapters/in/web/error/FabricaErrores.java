package co.reservas.adapters.in.web.error;

import co.reservas.domain.shared.CodigoError;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Construye el cuerpo de error con el {@code traceId} de la petición actual.
 */
@Component
public class FabricaErrores {

    public static final String CLAVE_TRACE_ID = "traceId";

    private final Clock clock;

    public FabricaErrores(Clock clock) {
        this.clock = clock;
    }

    public ErrorResponse crear(CodigoError codigo, String mensaje, List<?> detalles) {
        return new ErrorResponse(codigo, mensaje, List.copyOf(detalles), MDC.get(CLAVE_TRACE_ID), Instant.now(clock));
    }
}

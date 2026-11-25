package co.reservas.infrastructure.observabilidad;

import co.reservas.domain.shared.ExcepcionNegocio;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Agrega a la métrica {@code http.server.requests} la etiqueta {@code codigo_error} con el {@code CodigoError} de la
 * regla de negocio que rechazó la petición (por ejemplo {@code TURNO_SIN_CUPO} o {@code CUENTA_BLOQUEADA}).
 *
 * <p>Así las métricas de negocio (reservas rechazadas por sobrecupo, intentos de inicio de sesión fallidos, cuentas
 * bloqueadas) salen de la misma métrica HTTP sin instrumentar los servicios de aplicación. La cardinalidad está
 * acotada: los valores posibles son los del enum {@code CodigoError} más {@value #SIN_CODIGO}.
 */
@Component
public class ConvencionMetricasHttp extends DefaultServerRequestObservationConvention {

    static final String ETIQUETA = "codigo_error";
    static final String SIN_CODIGO = "NINGUNO";

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(KeyValue.of(ETIQUETA, codigoError(context)));
    }

    static String codigoError(ServerRequestObservationContext context) {
        Throwable error = context.getError();
        if (error == null && context.getCarrier() != null) {
            // Las excepciones resueltas por @RestControllerAdvice quedan en este atributo de la petición.
            Object atributo = context.getCarrier().getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
            error = atributo instanceof Throwable lanzada ? lanzada : null;
        }
        return error instanceof ExcepcionNegocio negocio ? negocio.getCodigo().name() : SIN_CODIGO;
    }
}

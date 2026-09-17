package co.reservas.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Propaga o genera el identificador de traza de cada petición: lo publica en el MDC (logs) y en la respuesta.
 * Se ejecuta antes que la cadena de seguridad para que los errores 401/403 también lo incluyan.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Trace-Id";
    public static final String CLAVE_MDC = "traceId";
    private static final Pattern FORMATO_VALIDO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolver(request.getHeader(CABECERA));
        MDC.put(CLAVE_MDC, traceId);
        response.setHeader(CABECERA, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }

    static String resolver(String recibido) {
        if (recibido != null && FORMATO_VALIDO.matcher(recibido).matches()) {
            return recibido;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}

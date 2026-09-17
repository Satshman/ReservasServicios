package co.reservas.infrastructure.security;

import co.reservas.domain.shared.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Principal;

/**
 * Responde 403 ACCESO_DENEGADO cuando el rol del token no autoriza el endpoint.
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorAccesoDenegado.class);

    private final EscritorErrorSeguridad escritor;

    public ManejadorAccesoDenegado(EscritorErrorSeguridad escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        Principal principal = request.getUserPrincipal();
        log.atWarn()
                .addKeyValue("evento", "ACCESO_DENEGADO")
                .addKeyValue("idUsuario", principal == null ? null : principal.getName())
                .addKeyValue("metodo", request.getMethod())
                .addKeyValue("ruta", request.getRequestURI())
                .log("Evento de seguridad");
        escritor.escribir(response, CodigoError.ACCESO_DENEGADO, "No tienes permisos para realizar esta operación.");
    }
}

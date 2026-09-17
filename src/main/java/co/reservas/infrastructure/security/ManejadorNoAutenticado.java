package co.reservas.infrastructure.security;

import co.reservas.domain.shared.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 NO_AUTENTICADO cuando falta el token o no es válido.
 */
@Component
public class ManejadorNoAutenticado implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ManejadorNoAutenticado.class);

    private final EscritorErrorSeguridad escritor;

    public ManejadorNoAutenticado(EscritorErrorSeguridad escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excepcion) throws IOException {
        log.atInfo()
                .addKeyValue("evento", "ACCESO_NO_AUTENTICADO")
                .addKeyValue("metodo", request.getMethod())
                .addKeyValue("ruta", request.getRequestURI())
                .log("Evento de seguridad");
        escritor.escribir(response, CodigoError.NO_AUTENTICADO,
                "Se requiere un token de acceso válido para este recurso.");
    }
}

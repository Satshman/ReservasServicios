package co.reservas.infrastructure.security;

import co.reservas.adapters.in.web.error.CodigosHttp;
import co.reservas.adapters.in.web.error.FabricaErrores;
import co.reservas.domain.shared.CodigoError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Escribe el cuerpo de error uniforme desde los filtros de seguridad, fuera de los controladores.
 */
@Component
public class EscritorErrorSeguridad {

    private final FabricaErrores errores;
    private final JsonMapper jsonMapper;

    public EscritorErrorSeguridad(FabricaErrores errores, JsonMapper jsonMapper) {
        this.errores = errores;
        this.jsonMapper = jsonMapper;
    }

    public void escribir(HttpServletResponse response, CodigoError codigo, String mensaje) throws IOException {
        response.setStatus(CodigosHttp.estado(codigo).value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), errores.crear(codigo, mensaje, List.of()));
    }
}

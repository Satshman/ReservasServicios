package co.reservas.adapters.in.web.auth;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.domain.usuario.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Obtiene la identidad del usuario a partir del JWT validado por Spring Security.
 */
public final class SesionActual {

    public static final String CLAIM_ROL = "rol";

    private SesionActual() {
    }

    public static UsuarioAutenticado de(Jwt jwt) {
        return new UsuarioAutenticado(Integer.valueOf(jwt.getSubject()), Rol.valueOf(jwt.getClaimAsString(CLAIM_ROL)));
    }
}

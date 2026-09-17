package co.reservas.infrastructure.security;

import co.reservas.adapters.in.web.error.FabricaErrores;
import co.reservas.domain.usuario.PoliticaBloqueo;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeguridadTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final String SECRETO = "clave-de-pruebas-unitarias-de-32-bytes!!";
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final SecurityConfig configuracion = new SecurityConfig();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static String token(String emisor, Instant emitido, Duration vigencia) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(SecurityConfig.claveSecreta(SECRETO)));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(emisor).subject("7").claim("rol", "CLIENTE")
                .issuedAt(emitido).expiresAt(emitido.plus(vigencia)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("Dado un token vigente del emisor esperado, cuando se decodifica, entonces es válido y su rol se convierte en autoridad ROLE_")
    void tokenValidoYAutoridades() {
        // Arrange
        JwtDecoder decoder = configuracion.jwtDecoder(SECRETO, "reservas-servicios", RELOJ);

        // Act
        Jwt jwt = decoder.decode(token("reservas-servicios", AHORA, Duration.ofMinutes(30)));
        AbstractAuthenticationToken autenticacion = SecurityConfig.convertidorAutenticacion().convert(jwt);

        // Assert
        assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_CLIENTE")
                .noneMatch(autoridad -> autoridad.startsWith("ROLE_") && !autoridad.equals("ROLE_CLIENTE"));
    }

    @Test
    @DisplayName("Dado un token vencido o de otro emisor, cuando se decodifica, entonces se rechaza")
    void tokenVencidoOEmisorAjeno() {
        // Arrange
        JwtDecoder decoder = configuracion.jwtDecoder(SECRETO, "reservas-servicios", RELOJ);
        String vencido = token("reservas-servicios", AHORA.minus(Duration.ofHours(1)), Duration.ofMinutes(30));
        String ajeno = token("otro-emisor", AHORA, Duration.ofMinutes(30));

        // Act - Assert
        assertThatThrownBy(() -> decoder.decode(vencido)).isInstanceOf(JwtValidationException.class);
        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("Dado una lista de orígenes con espacios y vacíos, cuando se configura CORS, entonces solo se permiten los orígenes declarados en /api")
    void cors() {
        // Arrange
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/v1/servicios");

        // Act
        CorsConfiguration cors = configuracion.corsConfigurationSource(" https://app.example.com, ,http://localhost:3000")
                .getCorsConfiguration(solicitud);

        // Assert
        assertThat(cors.getAllowedOrigins()).containsExactly("https://app.example.com", "http://localhost:3000");
        assertThat(cors.checkOrigin("https://malicioso.example.com")).isNull();
        assertThat(configuracion.jwtEncoder(SECRETO)).isNotNull();
        assertThat(configuracion.politicaBloqueo(5, Duration.ofMinutes(15)))
                .isEqualTo(new PoliticaBloqueo(5, Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Dado una petición sin autenticación o sin permisos, cuando la rechaza Spring Security, entonces responde 401 o 403 con el cuerpo uniforme")
    void manejadoresDeSeguridad() throws Exception {
        // Arrange
        EscritorErrorSeguridad escritor = new EscritorErrorSeguridad(new FabricaErrores(RELOJ), jsonMapper);
        MockHttpServletRequest solicitud = new MockHttpServletRequest("POST", "/api/v1/reservas");
        MockHttpServletResponse sinToken = new MockHttpServletResponse();
        MockHttpServletResponse sinPermiso = new MockHttpServletResponse();

        // Act
        new ManejadorNoAutenticado(escritor).commence(solicitud, sinToken, new BadCredentialsException("x"));
        new ManejadorAccesoDenegado(escritor).handle(solicitud, sinPermiso, new AccessDeniedException("x"));

        // Assert
        JsonNode cuerpo401 = jsonMapper.readTree(sinToken.getContentAsString());
        JsonNode cuerpo403 = jsonMapper.readTree(sinPermiso.getContentAsString());
        assertThat(sinToken.getStatus()).isEqualTo(401);
        assertThat(cuerpo401.get("errorCode").asString()).isEqualTo("NO_AUTENTICADO");
        assertThat(cuerpo401.get("timestamp").asString()).isEqualTo("2026-10-05T13:00:00Z");
        assertThat(sinPermiso.getStatus()).isEqualTo(403);
        assertThat(cuerpo403.get("errorCode").asString()).isEqualTo("ACCESO_DENEGADO");
        assertThat(sinPermiso.getContentType()).startsWith("application/json");
    }
}

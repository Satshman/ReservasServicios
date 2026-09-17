package co.reservas.infrastructure.security;

import co.reservas.domain.usuario.PoliticaBloqueo;
import co.reservas.domain.usuario.Rol;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    static final int BYTES_MINIMOS_SECRETO = 32;
    private static final String V1 = "/api/v1";
    private static final String PROVEEDOR = Rol.PROVEEDOR.name();
    private static final String CLIENTE = Rol.CLIENTE.name();

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, ManejadorNoAutenticado noAutenticado,
                                               ManejadorAccesoDenegado accesoDenegado) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, V1 + "/auth/registro", V1 + "/auth/login",
                                V1 + "/auth/verificacion/reenvio").permitAll()
                        .requestMatchers(HttpMethod.GET, V1 + "/auth/verificacion").permitAll()
                        .requestMatchers(HttpMethod.GET, V1 + "/catalogos/*").permitAll()
                        .requestMatchers(HttpMethod.GET, V1 + "/servicios", V1 + "/servicios/*",
                                V1 + "/servicios/*/horarios", V1 + "/servicios/*/disponibilidad").permitAll()
                        .requestMatchers(HttpMethod.POST, V1 + "/servicios", V1 + "/servicios/*/horarios",
                                V1 + "/recursos").hasRole(PROVEEDOR)
                        .requestMatchers(HttpMethod.PUT, V1 + "/horarios/*", V1 + "/servicios/*/recursos")
                        .hasRole(PROVEEDOR)
                        .requestMatchers(HttpMethod.DELETE, V1 + "/horarios/*").hasRole(PROVEEDOR)
                        .requestMatchers(HttpMethod.GET, V1 + "/recursos", V1 + "/servicios/*/reservas")
                        .hasRole(PROVEEDOR)
                        .requestMatchers(HttpMethod.POST, V1 + "/reservas").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.GET, V1 + "/reservas/mias").hasRole(CLIENTE)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorAutenticacion()))
                        .authenticationEntryPoint(noAutenticado)
                        .accessDeniedHandler(accesoDenegado))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(noAutenticado)
                        .accessDeniedHandler(accesoDenegado));
        return http.build();
    }

    /**
     * Spring Security usa el bean llamado {@code corsConfigurationSource}. Solo se permiten los orígenes
     * configurados en APP_CORS_ALLOWED_ORIGINS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));
        configuracion.setExposedHeaders(List.of("X-Trace-Id", "Location"));
        configuracion.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secreto) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSecreta(secreto)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secreto, @Value("${app.jwt.issuer}") String emisor,
                                 Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveSecreta(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ofSeconds(30));
        vigencia.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(vigencia, new JwtIssuerValidator(emisor)));
        return decoder;
    }

    @Bean
    public PoliticaBloqueo politicaBloqueo(@Value("${app.seguridad.login.max-intentos}") int maxIntentos,
                                           @Value("${app.seguridad.login.duracion-bloqueo}") Duration duracion) {
        return new PoliticaBloqueo(maxIntentos, duracion);
    }

    static JwtAuthenticationConverter convertidorAutenticacion() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    static SecretKey claveSecreta(String secreto) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes para HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}

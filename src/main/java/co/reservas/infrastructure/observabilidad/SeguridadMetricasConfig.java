package co.reservas.infrastructure.observabilidad;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Permite que Prometheus lea {@code /actuator/prometheus} sin token, pero solo cuando la petición llega por el puerto
 * de gestión ({@code management.server.port}). Ese puerto no se publica fuera de la red interna de contenedores, así
 * que el endpoint queda inaccesible desde Internet; por el puerto de la API el actuador no se sirve.
 *
 * <p>Solo se activa si hay puerto de gestión configurado (perfil {@code observabilidad}). Sin él, la cadena principal de
 * {@code SecurityConfig} sigue siendo la única y {@code /actuator/prometheus} exige autenticación.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "management.server", name = "port")
public class SeguridadMetricasConfig {

    static final String RUTA_PROMETHEUS = "/actuator/prometheus";
    /** Propiedad que Spring Boot publica con el puerto real de gestión, también cuando se configura con 0. */
    static final String PROPIEDAD_PUERTO_GESTION = "local.management.port";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain filtroMetricas(HttpSecurity http, Environment entorno) {
        http
                .securityMatcher(prometheusPorPuertoDeGestion(entorno))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll());
        return http.build();
    }

    static RequestMatcher prometheusPorPuertoDeGestion(Environment entorno) {
        return solicitud -> RUTA_PROMETHEUS.equals(ruta(solicitud))
                && solicitud.getLocalPort() == entorno.getProperty(PROPIEDAD_PUERTO_GESTION, Integer.class, -1);
    }

    private static String ruta(HttpServletRequest solicitud) {
        String uri = solicitud.getRequestURI();
        String contexto = solicitud.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length())
                : uri;
    }
}

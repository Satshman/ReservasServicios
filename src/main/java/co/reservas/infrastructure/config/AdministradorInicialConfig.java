package co.reservas.infrastructure.config;

import co.reservas.application.port.in.auth.AprovisionarAdministradorUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Al arrancar, crea la cuenta de administrador si se definió {@code APP_ADMIN_EMAIL} (con {@code APP_ADMIN_PASSWORD}
 * y, opcional, {@code APP_ADMIN_NOMBRE}). Sin correo, o con la variable vacía, no hace nada. El administrador inicia
 * sesión con MFA ({@code /api/v1/auth/mfa/solicitud} y {@code /api/v1/auth/mfa/verificacion}).
 */
@Configuration(proxyBeanMethods = false)
public class AdministradorInicialConfig {

    @Bean
    ApplicationRunner aprovisionarAdministrador(AprovisionarAdministradorUseCase aprovisionador,
                                                @Value("${app.admin.email:}") String email,
                                                @Value("${app.admin.nombre:}") String nombre,
                                                @Value("${app.admin.password:}") String password) {
        return argumentos -> {
            if (!email.isBlank()) {
                aprovisionador.aprovisionar(email, nombre, password);
            }
        };
    }
}

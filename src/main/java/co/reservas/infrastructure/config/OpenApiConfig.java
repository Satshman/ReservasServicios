package co.reservas.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma de Reservas de Servicios")
                        .version("v1")
                        .description("API REST versionada (/api/v1). Todos los errores usan el cuerpo uniforme "
                                + "errorCode, message, details, traceId y timestamp. Para los endpoints protegidos, "
                                + "inicie sesión en POST /api/v1/auth/login y use el accessToken en Authorize."))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}

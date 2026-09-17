package co.reservas.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Las entidades y repositorios JPA viven en el adaptador de persistencia, fuera del paquete de la aplicación.
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = JpaConfig.PAQUETE_PERSISTENCIA)
@EnableJpaRepositories(basePackages = JpaConfig.PAQUETE_PERSISTENCIA)
public class JpaConfig {

    static final String PAQUETE_PERSISTENCIA = "co.reservas.adapters.out.persistence";
}

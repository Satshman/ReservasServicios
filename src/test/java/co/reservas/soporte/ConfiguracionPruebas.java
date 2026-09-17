package co.reservas.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionPruebas {

    /** Lunes 5 de octubre de 2026, 08:00 en America/Bogota. */
    public static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("docker.io/library/postgres:17-alpine")
                .asCompatibleSubstituteFor("postgres"));
    }

    @Bean
    @Primary
    RelojAjustable reloj() {
        return new RelojAjustable(AHORA);
    }

    @Bean
    @Primary
    NotificacionesCapturadas notificacionesCapturadas() {
        return new NotificacionesCapturadas();
    }
}

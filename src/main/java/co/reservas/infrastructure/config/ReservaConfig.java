package co.reservas.infrastructure.config;

import co.reservas.domain.reserva.PoliticaCancelacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ReservaConfig {

    @Bean
    public PoliticaCancelacion politicaCancelacion(
            @Value("${app.reservas.cancelacion.anticipacion-minima-cliente}") Duration anticipacionMinimaCliente) {
        return new PoliticaCancelacion(anticipacionMinimaCliente);
    }
}

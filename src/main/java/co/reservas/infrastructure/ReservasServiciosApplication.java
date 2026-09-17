package co.reservas.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "co.reservas")
public class ReservasServiciosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasServiciosApplication.class, args);
    }
}

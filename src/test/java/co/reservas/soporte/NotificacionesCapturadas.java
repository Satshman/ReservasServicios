package co.reservas.soporte;

import co.reservas.application.port.out.auth.NotificacionPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Doble de prueba del puerto de notificaciones: guarda los correos en memoria en lugar de enviarlos.
 */
public class NotificacionesCapturadas implements NotificacionPort {

    public record CorreoVerificacion(String email, String nombre, String token) {
    }

    private final List<CorreoVerificacion> enviados = new CopyOnWriteArrayList<>();
    private volatile boolean fallar;

    @Override
    public void enviarVerificacionCuenta(String email, String nombre, String tokenPlano) {
        if (fallar) {
            throw new IllegalStateException("Servidor SMTP no disponible (simulado)");
        }
        enviados.add(new CorreoVerificacion(email, nombre, tokenPlano));
    }

    public Optional<CorreoVerificacion> ultimoPara(String email) {
        return enviados.stream().filter(correo -> correo.email().equals(email)).reduce((primero, segundo) -> segundo);
    }

    public long cantidadPara(String email) {
        return enviados.stream().filter(correo -> correo.email().equals(email)).count();
    }

    public void simularFallo(boolean valor) {
        this.fallar = valor;
    }
}

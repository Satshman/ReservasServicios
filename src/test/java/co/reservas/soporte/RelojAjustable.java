package co.reservas.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj fijo que las pruebas pueden adelantar para simular el paso del tiempo.
 */
public class RelojAjustable extends Clock {

    private final Instant inicial;
    private volatile Instant actual;

    public RelojAjustable(Instant inicial) {
        this.inicial = inicial;
        this.actual = inicial;
    }

    public void avanzar(Duration duracion) {
        actual = actual.plus(duracion);
    }

    public void restablecer() {
        actual = inicial;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(actual, zona);
    }

    @Override
    public Instant instant() {
        return actual;
    }
}

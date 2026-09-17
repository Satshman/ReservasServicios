package co.reservas.domain.agenda;

import java.time.Instant;

/**
 * Intervalo reservable generado a partir de un bloque de agenda.
 */
public record Turno(Instant inicio, Instant fin) {
}

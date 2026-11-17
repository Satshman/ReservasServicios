package co.reservas.domain.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo puro de porcentajes de ocupación y tasas de cancelación sin errores de división por cero.
 */
public final class CalculadoraOcupacion {

    private CalculadoraOcupacion() {
    }

    public static double calcularPorcentaje(long reservasConfirmadas, long capacidadDisponible) {
        if (capacidadDisponible <= 0 || reservasConfirmadas <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(reservasConfirmadas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacidadDisponible), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
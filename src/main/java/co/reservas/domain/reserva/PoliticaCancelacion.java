package co.reservas.domain.reserva;

import co.reservas.domain.usuario.Rol;

import java.time.Duration;
import java.util.Objects;

/**
 * Anticipación mínima con la que un cliente puede cancelar su reserva (HU-08). El proveedor dueño del servicio no
 * tiene ventana: puede cancelar mientras la reserva no haya comenzado.
 */
public record PoliticaCancelacion(Duration anticipacionMinimaCliente) {

    public PoliticaCancelacion {
        Objects.requireNonNull(anticipacionMinimaCliente, "anticipacionMinimaCliente");
        if (anticipacionMinimaCliente.isNegative()) {
            throw new IllegalArgumentException("anticipacionMinimaCliente no puede ser negativa");
        }
    }

    public Duration anticipacionPara(Rol rol) {
        return rol == Rol.CLIENTE ? anticipacionMinimaCliente : Duration.ZERO;
    }
}

package co.reservas.application.port.in.servicio;

import co.reservas.application.port.in.usuario.AutorCambio;
import co.reservas.domain.servicio.TipoCambioServicio;

import java.time.Instant;

public record CambioServicioResultado(
        Integer id,
        Integer idServicio,
        String nombreServicio,
        TipoCambioServicio tipoCambio,
        String valorAnterior,
        String valorNuevo,
        AutorCambio realizadoPor,
        Instant fechaCambio) {
}

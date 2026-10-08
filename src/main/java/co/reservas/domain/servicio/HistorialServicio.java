package co.reservas.domain.servicio;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cambio en la oferta de un servicio (HU-11): qué cambió ({@code valorAnterior} → {@code valorNuevo}), quién lo hizo
 * y cuándo. {@code valorAnterior} es nulo cuando algo se crea y {@code valorNuevo} cuando algo se elimina.
 */
public record HistorialServicio(Integer id, Integer idServicio, TipoCambioServicio tipoCambio, String valorAnterior,
                                String valorNuevo, Integer idUsuario, Instant fechaCambio) {

    static final String SIN_RECURSOS = "Sin recursos";

    public HistorialServicio {
        Objects.requireNonNull(idServicio, "idServicio");
        Objects.requireNonNull(tipoCambio, "tipoCambio");
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(fechaCambio, "fechaCambio");
        if (valorAnterior == null && valorNuevo == null) {
            throw new IllegalArgumentException("El cambio debe tener un valor anterior o uno nuevo");
        }
    }

    public static HistorialServicio creacion(Servicio servicio, Integer idUsuario, Instant ahora) {
        return new HistorialServicio(null, servicio.id(), TipoCambioServicio.CREACION, null,
                servicio.descripcionOferta(), idUsuario, ahora);
    }

    /**
     * @param horario descripción del bloque, por ejemplo {@code HorarioDisponible.descripcion()}
     */
    public static HistorialServicio horarioCreado(Integer idServicio, String horario, Integer idUsuario,
                                                  Instant ahora) {
        return new HistorialServicio(null, idServicio, TipoCambioServicio.HORARIO_CREADO, null, horario, idUsuario,
                ahora);
    }

    public static HistorialServicio horarioEditado(Integer idServicio, String anterior, String nuevo,
                                                   Integer idUsuario, Instant ahora) {
        return new HistorialServicio(null, idServicio, TipoCambioServicio.HORARIO_EDITADO, anterior, nuevo,
                idUsuario, ahora);
    }

    public static HistorialServicio horarioEliminado(Integer idServicio, String horario, Integer idUsuario,
                                                     Instant ahora) {
        return new HistorialServicio(null, idServicio, TipoCambioServicio.HORARIO_ELIMINADO, horario, null,
                idUsuario, ahora);
    }

    /**
     * Los nombres se ordenan para que la descripción no dependa del orden de asignación.
     */
    public static HistorialServicio recursosAsignados(Integer idServicio, Collection<String> anteriores,
                                                      Collection<String> nuevos, Integer idUsuario, Instant ahora) {
        return new HistorialServicio(null, idServicio, TipoCambioServicio.RECURSOS_ASIGNADOS,
                describirRecursos(anteriores), describirRecursos(nuevos), idUsuario, ahora);
    }

    public static HistorialServicio cambioDeEstado(EstadoServicio anterior, Servicio servicio, Integer idUsuario,
                                                   Instant ahora) {
        return new HistorialServicio(null, servicio.id(), TipoCambioServicio.ESTADO_CAMBIADO, anterior.name(),
                servicio.estado().name(), idUsuario, ahora);
    }

    static String describirRecursos(Collection<String> nombres) {
        if (nombres.isEmpty()) {
            return SIN_RECURSOS;
        }
        return nombres.stream().sorted().collect(Collectors.joining(", "));
    }
}

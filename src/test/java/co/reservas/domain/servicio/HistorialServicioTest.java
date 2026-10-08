package co.reservas.domain.servicio;

import co.reservas.domain.agenda.HorarioDisponible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistorialServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 2);

    @Test
    @DisplayName("Dado un servicio nuevo, cuando se registra su creación, entonces el valor nuevo resume la oferta y no hay valor anterior")
    void creacion() {
        // Arrange - Act
        HistorialServicio cambio = HistorialServicio.creacion(SERVICIO, 20, AHORA);

        // Assert
        assertThat(cambio.tipoCambio()).isEqualTo(TipoCambioServicio.CREACION);
        assertThat(cambio.valorAnterior()).isNull();
        assertThat(cambio.valorNuevo()).isEqualTo("Consulta · 30 min · capacidad 2");
        assertThat(cambio.idServicio()).isEqualTo(7);
        assertThat(cambio.idUsuario()).isEqualTo(20);
        assertThat(cambio.fechaCambio()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Dado un bloque de agenda, cuando se crea, edita o elimina, entonces el historial describe el día y las horas")
    void cambiosDeHorario() {
        // Arrange
        String lunes = HorarioDisponible.nuevo(1, 7, 1, LocalTime.of(8, 0), LocalTime.of(12, 0)).descripcion();
        String sabado = HorarioDisponible.nuevo(1, 7, 6, LocalTime.of(9, 30), LocalTime.of(13, 0)).descripcion();

        // Act
        HistorialServicio creado = HistorialServicio.horarioCreado(7, lunes, 20, AHORA);
        HistorialServicio editado = HistorialServicio.horarioEditado(7, lunes, sabado, 20, AHORA);
        HistorialServicio eliminado = HistorialServicio.horarioEliminado(7, sabado, 20, AHORA);

        // Assert
        assertThat(lunes).isEqualTo("Lunes 08:00-12:00");
        assertThat(sabado).isEqualTo("Sábado 09:30-13:00");
        assertThat(creado).extracting(HistorialServicio::tipoCambio, HistorialServicio::valorAnterior,
                HistorialServicio::valorNuevo).containsExactly(TipoCambioServicio.HORARIO_CREADO, null, lunes);
        assertThat(editado).extracting(HistorialServicio::tipoCambio, HistorialServicio::valorAnterior,
                HistorialServicio::valorNuevo).containsExactly(TipoCambioServicio.HORARIO_EDITADO, lunes, sabado);
        assertThat(eliminado).extracting(HistorialServicio::tipoCambio, HistorialServicio::valorAnterior,
                HistorialServicio::valorNuevo).containsExactly(TipoCambioServicio.HORARIO_ELIMINADO, sabado, null);
    }

    @Test
    @DisplayName("Dado recursos asignados en cualquier orden, cuando se registra la asignación, entonces los nombres se ordenan y una lista vacía se describe como sin recursos")
    void recursosAsignados() {
        // Arrange - Act
        HistorialServicio cambio = HistorialServicio.recursosAsignados(7, List.of(), List.of("Sala B", "Equipo"),
                20, AHORA);

        // Assert
        assertThat(cambio.tipoCambio()).isEqualTo(TipoCambioServicio.RECURSOS_ASIGNADOS);
        assertThat(cambio.valorAnterior()).isEqualTo("Sin recursos");
        assertThat(cambio.valorNuevo()).isEqualTo("Equipo, Sala B");
    }

    @Test
    @DisplayName("Dado un servicio activo, cuando se inactiva, entonces el historial registra el estado anterior y el nuevo")
    void cambioDeEstado() {
        // Arrange
        Servicio inactivo = SERVICIO.conEstado(EstadoServicio.INACTIVO);

        // Act
        HistorialServicio cambio = HistorialServicio.cambioDeEstado(SERVICIO.estado(), inactivo, 20, AHORA);

        // Assert
        assertThat(inactivo.estaActivo()).isFalse();
        assertThat(inactivo.nombre()).isEqualTo(SERVICIO.nombre());
        assertThat(cambio).extracting(HistorialServicio::tipoCambio, HistorialServicio::valorAnterior,
                HistorialServicio::valorNuevo)
                .containsExactly(TipoCambioServicio.ESTADO_CAMBIADO, "ACTIVO", "INACTIVO");
    }

    @Test
    @DisplayName("Dado un cambio sin valor anterior ni nuevo, cuando se construye, entonces se rechaza")
    void cambioSinValores() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> new HistorialServicio(null, 7, TipoCambioServicio.CREACION, null, null, 20, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

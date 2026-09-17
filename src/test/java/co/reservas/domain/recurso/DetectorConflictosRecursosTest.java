package co.reservas.domain.recurso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorConflictosRecursosTest {

    private static final Instant DIEZ = Instant.parse("2026-10-12T15:00:00Z");

    private static OcupacionRecurso ocupacion(int idRecurso, int idServicio, Instant inicio, int minutos) {
        return new OcupacionRecurso(idRecurso, 100, idServicio, inicio, inicio.plus(Duration.ofMinutes(minutos)));
    }

    @Test
    @DisplayName("Dado un recurso usado de 10:00 a 10:30 por otro servicio, cuando se pide de 10:00 a 11:00, entonces está ocupado")
    void solapeConOtroServicio() {
        // Arrange
        List<OcupacionRecurso> ocupaciones = List.of(ocupacion(1, 10, DIEZ, 30));

        // Act
        var ocupados = DetectorConflictosRecursos.recursosOcupados(List.of(1, 2), 20, DIEZ,
                DIEZ.plus(Duration.ofHours(1)), ocupaciones);

        // Assert
        assertThat(ocupados).containsExactly(1);
    }

    @Test
    @DisplayName("Dado un turno grupal del mismo servicio y el mismo inicio, cuando se pide el recurso, entonces se comparte sin conflicto")
    void turnoGrupalCompartido() {
        // Arrange
        List<OcupacionRecurso> ocupaciones = List.of(ocupacion(1, 10, DIEZ, 60));

        // Act
        var ocupados = DetectorConflictosRecursos.recursosOcupados(List.of(1), 10, DIEZ,
                DIEZ.plus(Duration.ofHours(1)), ocupaciones);

        // Assert
        assertThat(ocupados).isEmpty();
    }

    @Test
    @DisplayName("Dado el mismo servicio con inicios distintos que se solapan, cuando se pide el recurso, entonces sí hay conflicto")
    void mismoServicioDistintoInicio() {
        // Arrange
        List<OcupacionRecurso> ocupaciones = List.of(ocupacion(1, 10, DIEZ.minus(Duration.ofMinutes(30)), 60));

        // Act
        var ocupados = DetectorConflictosRecursos.recursosOcupados(List.of(1), 10, DIEZ,
                DIEZ.plus(Duration.ofHours(1)), ocupaciones);

        // Assert
        assertThat(ocupados).containsExactly(1);
    }

    @Test
    @DisplayName("Dado intervalos contiguos o recursos no requeridos, cuando se evalúa, entonces no hay conflicto")
    void contiguosYNoRequeridos() {
        // Arrange
        List<OcupacionRecurso> ocupaciones = List.of(ocupacion(1, 10, DIEZ.minus(Duration.ofMinutes(30)), 30),
                ocupacion(1, 10, DIEZ.plus(Duration.ofHours(1)), 30), ocupacion(3, 10, DIEZ, 60));

        // Act
        var ocupados = DetectorConflictosRecursos.recursosOcupados(List.of(1, 2), 20, DIEZ,
                DIEZ.plus(Duration.ofHours(1)), ocupaciones);

        // Assert
        assertThat(ocupados).isEmpty();
    }
}

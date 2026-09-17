package co.reservas.domain.servicio;

import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioTest {

    @Test
    @DisplayName("Dado un servicio sin capacidad ni descripción, cuando se crea, entonces queda activo con capacidad 1")
    void valoresPorDefecto() {
        // Arrange - Act
        Servicio servicio = Servicio.nuevo(3, 1, "  Consulta ", " ", 30, null);

        // Assert
        assertThat(servicio.nombre()).isEqualTo("Consulta");
        assertThat(servicio.descripcion()).isNull();
        assertThat(servicio.capacidad()).isEqualTo(1);
        assertThat(servicio.duracion()).isEqualTo(Duration.ofMinutes(30));
        assertThat(servicio.estaActivo()).isTrue();
        assertThat(servicio.perteneceA(3)).isTrue();
        assertThat(servicio.perteneceA(4)).isFalse();
    }

    @ParameterizedTest(name = "duración {0} minutos")
    @ValueSource(ints = {4, 481})
    @DisplayName("Dado una duración fuera de 5..480 minutos, cuando se crea el servicio, entonces se rechaza el campo duracionMinutos")
    void duracionFueraDeRango(int minutos) {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> Servicio.nuevo(3, 1, "Consulta", null, minutos, 1))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getDetalles())
                        .containsExactly(new DetalleCampo("duracionMinutos", "Debe estar entre 5 y 480 minutos")));
    }

    @Test
    @DisplayName("Dado un nombre vacío o una capacidad menor a 1, cuando se crea el servicio, entonces se rechaza")
    void nombreYCapacidadInvalidos() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> Servicio.nuevo(3, 1, " ", null, 30, 1)).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Servicio.nuevo(3, 1, "Consulta", null, 30, 0)).isInstanceOf(ExcepcionNegocio.class);
    }
}

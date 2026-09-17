package co.reservas.domain.agenda;

import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorarioDisponibleTest {

    /** 2026-10-12 es lunes. */
    private static final LocalDateTime LUNES = LocalDateTime.of(2026, 10, 12, 0, 0);

    private static HorarioDisponible bloque(Integer id, int dia, String inicio, String fin) {
        return HorarioDisponible.nuevo(id, 1, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    @ParameterizedTest(name = "{0}-{1} contra 08:00-12:00 → solapa={2}")
    @CsvSource({"07:00,08:00,false", "07:00,08:01,true", "11:59,13:00,true", "12:00,13:00,false", "09:00,10:00,true"})
    @DisplayName("Dado un bloque de 08:00 a 12:00, cuando se compara con otro del mismo día, entonces solo se solapan si comparten tiempo")
    void deteccionDeSolapes(String inicio, String fin, boolean esperado) {
        // Arrange
        HorarioDisponible existente = bloque(1, 1, "08:00", "12:00");
        HorarioDisponible nuevo = bloque(null, 1, inicio, fin);

        // Act
        boolean solapa = nuevo.seSolapaCon(existente);

        // Assert
        assertThat(solapa).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Dado bloques de días distintos o el mismo bloque editado, cuando se validan solapes, entonces no hay conflicto")
    void sinSolapeEntreDiasNiConsigoMismo() {
        // Arrange
        HorarioDisponible lunes = bloque(1, 1, "08:00", "12:00");
        HorarioDisponible martes = bloque(null, 2, "08:00", "12:00");
        HorarioDisponible lunesEditado = bloque(1, 1, "09:00", "13:00");

        // Act - Assert
        assertThatCode(() -> HorarioDisponible.validarSinSolapes(List.of(martes, lunesEditado), List.of(lunes)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> HorarioDisponible.validarSinSolapes(List.of(bloque(null, 1, "11:00", "13:00")),
                List.of(lunes)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.HORARIO_SOLAPADO));
    }

    @Test
    @DisplayName("Dado un servicio de 60 minutos, cuando el bloque dura 45 minutos, entonces se rechaza con BLOQUE_MENOR_A_DURACION")
    void bloqueMenorADuracion() {
        // Arrange
        HorarioDisponible corto = bloque(null, 1, "08:00", "08:45");

        // Act - Assert
        assertThatThrownBy(() -> corto.validarCubreDuracion(Duration.ofMinutes(60)))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.BLOQUE_MENOR_A_DURACION));
        assertThatCode(() -> corto.validarCubreDuracion(Duration.ofMinutes(45))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dado un día fuera de 1..7 o un inicio igual al fin, cuando se crea el bloque, entonces se rechaza con VALIDACION_FALLIDA")
    void formaInvalida() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> bloque(null, 0, "08:00", "12:00")).isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> bloque(null, 1, "08:00", "08:00"))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION_FALLIDA));
    }

    @ParameterizedTest(name = "inicio {0} → turno válido={1}")
    @CsvSource({"08:00,true", "08:30,true", "08:15,false", "11:30,true", "11:45,false", "12:00,false", "07:30,false"})
    @DisplayName("Dado un bloque de 08:00 a 12:00 y turnos de 30 minutos, cuando se evalúa un inicio, entonces solo se admiten inicios alineados que caben")
    void alineacionDeTurnos(String hora, boolean esperado) {
        // Arrange
        HorarioDisponible lunes = bloque(1, 1, "08:00", "12:00");
        LocalDateTime inicio = LUNES.with(LocalTime.parse(hora));

        // Act
        boolean admite = lunes.admiteTurno(inicio, Duration.ofMinutes(30));

        // Assert
        assertThat(admite).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Dado un bloque del lunes, cuando se evalúa un inicio del martes a la misma hora, entonces no lo contiene")
    void otroDiaNoCoincide() {
        // Arrange
        HorarioDisponible lunes = bloque(1, 1, "08:00", "12:00");

        // Act - Assert
        assertThat(lunes.contieneInicio(LUNES.plusDays(1).withHour(9))).isFalse();
        assertThat(lunes.contieneInicio(LUNES.withHour(9))).isTrue();
        assertThat(lunes.contieneIntervalo(LUNES.withHour(11), LUNES.withHour(12).withMinute(1))).isFalse();
    }
}

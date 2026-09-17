package co.reservas.application.service.reserva;

import co.reservas.application.port.in.reserva.SugerenciaTurno;
import co.reservas.application.port.in.reserva.TurnoDisponible;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.recurso.OcupacionRecurso;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadoraDisponibilidadTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final Instant LUNES_0800 = Instant.parse("2026-10-12T13:00:00Z");
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Clase", null,
            Duration.ofMinutes(60), 2);
    private static final List<HorarioDisponible> BLOQUES = List.of(
            new HorarioDisponible(1, 7, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(11, 0)));

    @Mock
    private ReservaRepositoryPort reservas;

    @Test
    @DisplayName("Dado un turno lleno y otro con un cupo ocupado, cuando se calculan turnos, entonces se omite el lleno y se informa el cupo restante")
    void cupos() {
        // Arrange
        CalculadoraDisponibilidad calculadora = new CalculadoraDisponibilidad(reservas);
        ContextoAgenda contexto = new ContextoAgenda(SERVICIO, BOGOTA, BLOQUES, List.of());
        when(reservas.contarConfirmadasPorTurno(eq(7), any(), any()))
                .thenReturn(Map.of(LUNES_0800, 2L, LUNES_0800.plusSeconds(3600), 1L));

        // Act
        List<TurnoDisponible> turnos = calculadora.turnosDisponibles(contexto, LUNES, LUNES, AHORA);

        // Assert
        assertThat(turnos).extracting(TurnoDisponible::cuposDisponibles).containsExactly(1, 2);
        assertThat(turnos.getFirst().fechaHoraInicio().toString()).isEqualTo("2026-10-12T09:00-05:00");
        verify(reservas, never()).listarOcupaciones(anyCollection(), any(), any());
    }

    @Test
    @DisplayName("Dado un recurso ocupado por otro servicio a las 09:00, cuando se calculan turnos, entonces ese turno no está disponible")
    void recursoOcupado() {
        // Arrange
        CalculadoraDisponibilidad calculadora = new CalculadoraDisponibilidad(reservas);
        ContextoAgenda contexto = new ContextoAgenda(SERVICIO, BOGOTA, BLOQUES,
                List.of(new Recurso(4, 3, 1, "Sala", true)));
        Instant nueve = LUNES_0800.plusSeconds(3600);
        when(reservas.contarConfirmadasPorTurno(eq(7), any(), any())).thenReturn(Map.of());
        when(reservas.listarOcupaciones(eq(List.of(4)), any(), any()))
                .thenReturn(List.of(new OcupacionRecurso(4, 80, 99, nueve, nueve.plusSeconds(1800))));

        // Act
        List<TurnoDisponible> turnos = calculadora.turnosDisponibles(contexto, LUNES, LUNES, AHORA);

        // Assert
        assertThat(turnos).extracting(turno -> turno.fechaHoraInicio().toInstant())
                .containsExactly(LUNES_0800, LUNES_0800.plusSeconds(7200));
    }

    @Test
    @DisplayName("Dado una referencia en el futuro, cuando se piden sugerencias, entonces devuelve como máximo 3 turnos desde la referencia")
    void sugerenciasLimitadas() {
        // Arrange
        CalculadoraDisponibilidad calculadora = new CalculadoraDisponibilidad(reservas);
        ContextoAgenda contexto = new ContextoAgenda(SERVICIO, BOGOTA, BLOQUES, List.of());
        when(reservas.contarConfirmadasPorTurno(eq(7), any(), any())).thenReturn(Map.of());

        // Act
        List<SugerenciaTurno> sugerencias = calculadora.sugerencias(contexto, LUNES_0800.plusSeconds(60), AHORA);

        // Assert
        assertThat(sugerencias).hasSize(3);
        assertThat(sugerencias).extracting(s -> s.fechaHoraInicio().toInstant()).containsExactly(
                LUNES_0800.plusSeconds(3600), LUNES_0800.plusSeconds(7200), Instant.parse("2026-10-19T13:00:00Z"));
        assertThat(sugerencias.getFirst().tipo()).isEqualTo("SUGERENCIA");
    }

    @Test
    @DisplayName("Dado un servicio sin agenda, cuando se piden sugerencias, entonces la lista está vacía y no consulta reservas")
    void sinAgenda() {
        // Arrange
        CalculadoraDisponibilidad calculadora = new CalculadoraDisponibilidad(reservas);
        ContextoAgenda contexto = new ContextoAgenda(SERVICIO, BOGOTA, List.of(), List.of());

        // Act
        List<SugerenciaTurno> sugerencias = calculadora.sugerencias(contexto, AHORA.minusSeconds(60), AHORA);

        // Assert
        assertThat(sugerencias).isEmpty();
        verify(reservas, never()).contarConfirmadasPorTurno(any(), any(), any());
    }
}

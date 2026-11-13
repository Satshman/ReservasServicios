package co.reservas.application.port.in.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.domain.reporte.TipoDesgloseOcupacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GenerarReporteOcupacionUseCase {

    ReporteOcupacion generar(UsuarioAutenticado usuario, FiltroReporteOcupacion filtro);

    record FiltroReporteOcupacion(
            Integer idServicio,
            LocalDate desde,
            LocalDate hasta,
            TipoDesgloseOcupacion desglose
    ) {
    }

    record OcupacionPorDia(
            LocalDate fecha,
            long reservasConfirmadas,
            long capacidadDisponible,
            double porcentajeOcupacion
    ) {
    }

    record OcupacionPorBloque(
            LocalDate fecha,
            DayOfWeek diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            long reservasConfirmadas,
            long capacidadDisponible,
            double porcentajeOcupacion
    ) {
    }

    record ReporteOcupacion(
            Integer idServicio,
            String nombreServicio,
            LocalDate desde,
            LocalDate hasta,
            TipoDesgloseOcupacion desglose,
            long totalReservasConfirmadas,
            long totalCapacidadDisponible,
            double porcentajeOcupacionGlobal,
            List<OcupacionPorDia> porDia,
            List<OcupacionPorBloque> porBloqueHorario
    ) {
    }
}
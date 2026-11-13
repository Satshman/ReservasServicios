package co.reservas.application.service.reporte;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.reporte.GenerarReporteOcupacionUseCase;
import co.reservas.application.port.out.agenda.AgendaRepositoryPort;
import co.reservas.application.port.out.reserva.ReservaRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.GeneradorTurnos;
import co.reservas.domain.agenda.HorarioDisponible;
import co.reservas.domain.agenda.Turno;
import co.reservas.domain.reporte.CalculadoraOcupacion;
import co.reservas.domain.reporte.TipoDesgloseOcupacion;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReporteOcupacionService implements GenerarReporteOcupacionUseCase {

    private final VerificadorPropiedad verificadorPropiedad;
    private final AgendaRepositoryPort agenda;
    private final ReservaRepositoryPort reservas;

    public ReporteOcupacionService(VerificadorPropiedad verificadorPropiedad,
                                   AgendaRepositoryPort agenda,
                                   ReservaRepositoryPort reservas) {
        this.verificadorPropiedad = verificadorPropiedad;
        this.agenda = agenda;
        this.reservas = reservas;
    }

    @Override
    public ReporteOcupacion generar(UsuarioAutenticado usuario, FiltroReporteOcupacion filtro) {
        validarFechas(filtro.desde(), filtro.hasta());

        ServicioPropio propio = verificadorPropiedad.servicioPropio(usuario, filtro.idServicio());
        Servicio servicio = propio.servicio();
        ZoneId zona = propio.proveedor().zonaHoraria();
        TipoDesgloseOcupacion desglose = filtro.desglose() != null ? filtro.desglose() : TipoDesgloseOcupacion.DIA;

        List<HorarioDisponible> bloques = agenda.listarPorServicio(servicio.id());
        Instant instanteDesde = filtro.desde().atStartOfDay(zona).toInstant();
        Instant instanteHasta = filtro.hasta().plusDays(1).atStartOfDay(zona).toInstant();

        Map<Instant, Long> confirmadasPorTurno = reservas.contarConfirmadasPorTurno(
                servicio.id(), instanteDesde, instanteHasta);

        List<OcupacionPorDia> porDia = new ArrayList<>();
        List<OcupacionPorBloque> porBloque = new ArrayList<>();
        long totalConfirmadas = 0;
        long totalCapacidad = 0;

        for (LocalDate fecha = filtro.desde(); !fecha.isAfter(filtro.hasta()); fecha = fecha.plusDays(1)) {
            final LocalDate fechaActual = fecha;
            List<HorarioDisponible> bloquesDelDia = bloques.stream()
                    .filter(b -> b.diaSemana() == fechaActual.getDayOfWeek())
                    .sorted(Comparator.comparing(HorarioDisponible::horaInicio))
                    .toList();

            long confirmadasDia = 0;
            long capacidadDia = 0;

            for (HorarioDisponible bloque : bloquesDelDia) {
                List<Turno> turnosBloque = GeneradorTurnos.generar(
                        List.of(bloque), servicio.duracion(), zona, fechaActual, fechaActual);

                long capacidadBloque = (long) turnosBloque.size() * servicio.capacidad();
                long confirmadasBloque = turnosBloque.stream()
                        .mapToLong(t -> confirmadasPorTurno.getOrDefault(t.inicio(), 0L))
                        .sum();

                confirmadasDia += confirmadasBloque;
                capacidadDia += capacidadBloque;

                if (desglose == TipoDesgloseOcupacion.BLOQUE_HORARIO) {
                    porBloque.add(new OcupacionPorBloque(
                            fechaActual,
                            bloque.diaSemana(),
                            bloque.horaInicio(),
                            bloque.horaFin(),
                            confirmadasBloque,
                            capacidadBloque,
                            CalculadoraOcupacion.calcularPorcentaje(confirmadasBloque, capacidadBloque)
                    ));
                }
            }

            totalConfirmadas += confirmadasDia;
            totalCapacidad += capacidadDia;

            if (desglose == TipoDesgloseOcupacion.DIA) {
                porDia.add(new OcupacionPorDia(
                        fechaActual,
                        confirmadasDia,
                        capacidadDia,
                        CalculadoraOcupacion.calcularPorcentaje(confirmadasDia, capacidadDia)
                ));
            }
        }

        return new ReporteOcupacion(
                servicio.id(),
                servicio.nombre(),
                filtro.desde(),
                filtro.hasta(),
                desglose,
                totalConfirmadas,
                totalCapacidad,
                CalculadoraOcupacion.calcularPorcentaje(totalConfirmadas, totalCapacidad),
                desglose == TipoDesgloseOcupacion.DIA ? List.copyOf(porDia) : List.of(),
                desglose == TipoDesgloseOcupacion.BLOQUE_HORARIO ? List.copyOf(porBloque) : List.of()
        );
    }

    private static void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ExcepcionNegocio(CodigoError.VALIDACION_FALLIDA,
                    "Las fechas inicial y final son obligatorias.");
        }
        if (hasta.isBefore(desde)) {
            throw new ExcepcionNegocio(CodigoError.VALIDACION_FALLIDA,
                    "La fecha final no puede ser anterior a la fecha inicial.");
        }
    }
}
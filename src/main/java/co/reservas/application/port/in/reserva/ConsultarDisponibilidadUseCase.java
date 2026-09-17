package co.reservas.application.port.in.reserva;

import java.time.LocalDate;

public interface ConsultarDisponibilidadUseCase {

    DisponibilidadResultado consultar(Integer idServicio, LocalDate desde, LocalDate hasta);
}

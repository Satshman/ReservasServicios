package co.reservas.application.port.in.agenda;

import java.util.List;

public interface ConsultarHorariosUseCase {

    List<HorarioResultado> consultar(Integer idServicio);
}

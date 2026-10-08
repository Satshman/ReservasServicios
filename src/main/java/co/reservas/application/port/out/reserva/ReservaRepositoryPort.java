package co.reservas.application.port.out.reserva;

import co.reservas.domain.recurso.OcupacionRecurso;
import co.reservas.domain.reserva.HistorialReserva;
import co.reservas.domain.reserva.Reserva;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReservaRepositoryPort {

    Reserva guardar(Reserva reserva);

    void registrarHistorial(HistorialReserva historial);

    /**
     * Cambios de estado de las reservas dadas, en orden cronológico.
     */
    List<HistorialReserva> listarHistorial(Collection<Integer> idsReservas);

    Optional<Reserva> buscarPorId(Integer idReserva);

    /**
     * Reservas que cumplen el criterio, de la más reciente a la más antigua por fecha de inicio.
     */
    List<Reserva> buscar(CriterioBusquedaReservas criterio);

    /**
     * Busca la reserva bloqueando su fila ({@code SELECT ... FOR UPDATE}) hasta el fin de la transacción.
     */
    Optional<Reserva> bloquearPorId(Integer idReserva);

    /**
     * Guarda el estado de una reserva existente.
     */
    void actualizarEstado(Reserva reserva);

    long contarConfirmadasEnTurno(Integer idServicio, Instant inicio);

    /**
     * Cuenta reservas confirmadas por inicio de turno con inicio en [desde, hasta).
     */
    Map<Instant, Long> contarConfirmadasPorTurno(Integer idServicio, Instant desde, Instant hasta);

    boolean existeSolapeCliente(Integer idCliente, Instant inicio, Instant fin);

    List<Reserva> listarConfirmadasFuturasPorServicio(Integer idServicio, Instant ahora);

    /**
     * Ocupaciones de reservas confirmadas sobre los recursos dados que se solapan con [desde, hasta).
     */
    List<OcupacionRecurso> listarOcupaciones(Collection<Integer> idsRecursos, Instant desde, Instant hasta);

    List<Reserva> listarPorCliente(Integer idCliente);

    List<Reserva> listarPorServicio(Integer idServicio);
}

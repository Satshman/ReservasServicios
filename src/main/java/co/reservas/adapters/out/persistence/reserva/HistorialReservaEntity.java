package co.reservas.adapters.out.persistence.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_historial_reservas")
public class HistorialReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_reserva", nullable = false)
    private Integer idReserva;

    @Column(name = "id_estado_anterior")
    private Integer idEstadoAnterior;

    @Column(name = "id_estado_nuevo", nullable = false)
    private Integer idEstadoNuevo;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio;

    protected HistorialReservaEntity() {
    }

    public HistorialReservaEntity(Integer idReserva, Integer idEstadoAnterior, Integer idEstadoNuevo,
                                  Integer idUsuario, Instant fechaCambio) {
        this.idReserva = idReserva;
        this.idEstadoAnterior = idEstadoAnterior;
        this.idEstadoNuevo = idEstadoNuevo;
        this.idUsuario = idUsuario;
        this.fechaCambio = fechaCambio;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public Integer getIdEstadoAnterior() {
        return idEstadoAnterior;
    }

    public Integer getIdEstadoNuevo() {
        return idEstadoNuevo;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public Instant getFechaCambio() {
        return fechaCambio;
    }
}

package co.reservas.adapters.out.persistence.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_reservas")
public class ReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_estado", nullable = false)
    private Integer idEstado;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private Instant fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private Instant fechaHoraFin;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ReservaEntity() {
    }

    @SuppressWarnings("java:S107")
    public ReservaEntity(Integer id, Integer idCliente, Integer idServicio, Integer idEstado, Instant fechaHoraInicio,
                         Instant fechaHoraFin, Instant creadoEn) {
        this.id = id;
        this.idCliente = idCliente;
        this.idServicio = idServicio;
        this.idEstado = idEstado;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.creadoEn = creadoEn;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public Integer getIdServicio() {
        return idServicio;
    }

    public Integer getIdEstado() {
        return idEstado;
    }

    public Instant getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public Instant getFechaHoraFin() {
        return fechaHoraFin;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

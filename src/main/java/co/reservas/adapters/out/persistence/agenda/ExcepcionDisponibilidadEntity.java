package co.reservas.adapters.out.persistence.agenda;

import co.reservas.domain.agenda.ExcepcionDisponibilidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "tbl_excepciones_disponibilidad")
public class ExcepcionDisponibilidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "motivo", length = 50)
    private String motivo;

    protected ExcepcionDisponibilidadEntity() {
    }

    public ExcepcionDisponibilidadEntity(Integer id, Integer idServicio, LocalDate fecha, LocalTime horaInicio,
                                         LocalTime horaFin, String motivo) {
        this.id = id;
        this.idServicio = idServicio;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.motivo = motivo;
    }

    public static ExcepcionDisponibilidadEntity deDominio(ExcepcionDisponibilidad d) {
        return new ExcepcionDisponibilidadEntity(
                d.id(), d.idServicio(), d.fecha(), d.horaInicio(), d.horaFin(), d.motivo());
    }

    public ExcepcionDisponibilidad aDominio() {
        return new ExcepcionDisponibilidad(id, idServicio, fecha, horaInicio, horaFin, motivo);
    }
}
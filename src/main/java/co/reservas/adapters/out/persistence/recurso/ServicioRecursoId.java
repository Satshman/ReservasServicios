package co.reservas.adapters.out.persistence.recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ServicioRecursoId implements Serializable {

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_recurso", nullable = false)
    private Integer idRecurso;

    protected ServicioRecursoId() {
    }

    public ServicioRecursoId(Integer idServicio, Integer idRecurso) {
        this.idServicio = idServicio;
        this.idRecurso = idRecurso;
    }

    public Integer getIdServicio() {
        return idServicio;
    }

    public Integer getIdRecurso() {
        return idRecurso;
    }

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof ServicioRecursoId id
                && Objects.equals(idServicio, id.idServicio) && Objects.equals(idRecurso, id.idRecurso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idServicio, idRecurso);
    }
}

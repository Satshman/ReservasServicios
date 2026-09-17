package co.reservas.adapters.out.persistence.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReservaRecursoId implements Serializable {

    @Column(name = "id_reserva", nullable = false)
    private Integer idReserva;

    @Column(name = "id_recurso", nullable = false)
    private Integer idRecurso;

    protected ReservaRecursoId() {
    }

    public ReservaRecursoId(Integer idReserva, Integer idRecurso) {
        this.idReserva = idReserva;
        this.idRecurso = idRecurso;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public Integer getIdRecurso() {
        return idRecurso;
    }

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof ReservaRecursoId id
                && Objects.equals(idReserva, id.idReserva) && Objects.equals(idRecurso, id.idRecurso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva, idRecurso);
    }
}

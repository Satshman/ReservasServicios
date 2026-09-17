package co.reservas.adapters.out.persistence.reserva;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_reservas_recursos")
public class ReservaRecursoEntity {

    @EmbeddedId
    private ReservaRecursoId id;

    protected ReservaRecursoEntity() {
    }

    public ReservaRecursoEntity(Integer idReserva, Integer idRecurso) {
        this.id = new ReservaRecursoId(idReserva, idRecurso);
    }

    public ReservaRecursoId getId() {
        return id;
    }
}

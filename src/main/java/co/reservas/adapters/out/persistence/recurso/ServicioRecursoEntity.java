package co.reservas.adapters.out.persistence.recurso;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_servicios_recursos")
public class ServicioRecursoEntity {

    @EmbeddedId
    private ServicioRecursoId id;

    protected ServicioRecursoEntity() {
    }

    public ServicioRecursoEntity(Integer idServicio, Integer idRecurso) {
        this.id = new ServicioRecursoId(idServicio, idRecurso);
    }

    public ServicioRecursoId getId() {
        return id;
    }
}

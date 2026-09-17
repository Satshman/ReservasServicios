package co.reservas.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_tipos_estado")
public class TipoEstadoEntity {

    @Id
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    protected TipoEstadoEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

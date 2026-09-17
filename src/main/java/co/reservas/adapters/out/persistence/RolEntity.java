package co.reservas.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_roles")
public class RolEntity {

    @Id
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    protected RolEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

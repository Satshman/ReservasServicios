package co.reservas.adapters.out.persistence.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_categorias_servicio")
public class CategoriaServicioEntity {

    @Id
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    protected CategoriaServicioEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

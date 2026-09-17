package co.reservas.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_estados")
public class EstadoEntity {

    @Id
    private Integer id;

    @Column(name = "id_tipo_estado", nullable = false)
    private Integer idTipoEstado;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    protected EstadoEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdTipoEstado() {
        return idTipoEstado;
    }

    public String getNombre() {
        return nombre;
    }
}

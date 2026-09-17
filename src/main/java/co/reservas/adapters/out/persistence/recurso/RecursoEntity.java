package co.reservas.adapters.out.persistence.recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_recursos")
public class RecursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_proveedor", nullable = false)
    private Integer idProveedor;

    @Column(name = "id_tipo_recurso", nullable = false)
    private Integer idTipoRecurso;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected RecursoEntity() {
    }

    public RecursoEntity(Integer id, Integer idProveedor, Integer idTipoRecurso, String nombre, boolean activo) {
        this.id = id;
        this.idProveedor = idProveedor;
        this.idTipoRecurso = idTipoRecurso;
        this.nombre = nombre;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public Integer getIdTipoRecurso() {
        return idTipoRecurso;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }
}

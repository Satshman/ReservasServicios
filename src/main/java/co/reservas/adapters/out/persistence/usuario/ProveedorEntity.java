package co.reservas.adapters.out.persistence.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_proveedores")
public class ProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "nombre_comercial", nullable = false, length = 100)
    private String nombreComercial;

    @Column(name = "zona_horaria", nullable = false, length = 50)
    private String zonaHoraria;

    protected ProveedorEntity() {
    }

    public ProveedorEntity(Integer id, Integer idUsuario, String nombreComercial, String zonaHoraria) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.nombreComercial = nombreComercial;
        this.zonaHoraria = zonaHoraria;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }
}

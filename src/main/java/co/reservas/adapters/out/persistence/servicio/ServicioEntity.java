package co.reservas.adapters.out.persistence.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;

@Entity
@Table(name = "tbl_servicios")
public class ServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_proveedor", nullable = false)
    private Integer idProveedor;

    @Column(name = "id_categoria", nullable = false)
    private Integer idCategoria;

    @Column(name = "id_estado", nullable = false)
    private Integer idEstado;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "duracion", nullable = false, columnDefinition = "interval")
    private Duration duracion;

    @Column(name = "capacidad", nullable = false)
    private Short capacidad;

    protected ServicioEntity() {
    }

    @SuppressWarnings("java:S107")
    public ServicioEntity(Integer id, Integer idProveedor, Integer idCategoria, Integer idEstado, String nombre,
                          String descripcion, Duration duracion, Short capacidad) {
        this.id = id;
        this.idProveedor = idProveedor;
        this.idCategoria = idCategoria;
        this.idEstado = idEstado;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracion = duracion;
        this.capacidad = capacidad;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public Integer getIdEstado() {
        return idEstado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Duration getDuracion() {
        return duracion;
    }

    public Short getCapacidad() {
        return capacidad;
    }
}

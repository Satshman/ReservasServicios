
package co.reservas.adapters.out.persistence.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_historial_servicios")
public class HistorialServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "tipo_cambio", nullable = false, length = 30)
    private String tipoCambio;

    @Column(name = "valor_anterior", columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "text")
    private String valorNuevo;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio;

    protected HistorialServicioEntity() {
    }

    public HistorialServicioEntity(Integer idServicio, String tipoCambio, String valorAnterior, String valorNuevo,
                                   Integer idUsuario, Instant fechaCambio) {
        this.idServicio = idServicio;
        this.tipoCambio = tipoCambio;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.idUsuario = idUsuario;
        this.fechaCambio = fechaCambio;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdServicio() {
        return idServicio;
    }

    public String getTipoCambio() {
        return tipoCambio;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public Instant getFechaCambio() {
        return fechaCambio;
    }
}

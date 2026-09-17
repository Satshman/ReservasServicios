package co.reservas.adapters.out.persistence.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_tokens_verificacion")
public class TokenVerificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected TokenVerificacionEntity() {
    }

    public TokenVerificacionEntity(Integer id, Integer idUsuario, String tokenHash, Instant expiraEn, Instant usadoEn,
                                   Instant creadoEn) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.usadoEn = usadoEn;
        this.creadoEn = creadoEn;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getUsadoEn() {
        return usadoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

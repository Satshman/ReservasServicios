package co.reservas.adapters.out.persistence.usuario;

import co.reservas.domain.usuario.CodigoMfa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_codigos_mfa")
public class CodigoMfaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected CodigoMfaEntity() {
    }

    public CodigoMfaEntity(Integer id, Integer idUsuario, String codigoHash, Instant expiraEn,
                           Instant usadoEn, Instant creadoEn) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.codigoHash = codigoHash;
        this.expiraEn = expiraEn;
        this.usadoEn = usadoEn;
        this.creadoEn = creadoEn;
    }

    public static CodigoMfaEntity deDominio(CodigoMfa d) {
        return new CodigoMfaEntity(
                d.id(), d.idUsuario(), d.codigoHash(), d.expiraEn(), d.usadoEn(), d.creadoEn());
    }

    public CodigoMfa aDominio() {
        return new CodigoMfa(id, idUsuario, codigoHash, expiraEn, usadoEn, creadoEn);
    }
}
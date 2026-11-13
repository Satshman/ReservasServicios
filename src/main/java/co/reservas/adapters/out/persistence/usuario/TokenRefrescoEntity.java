package co.reservas.adapters.out.persistence.usuario;

import co.reservas.domain.usuario.TokenRefresco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tbl_tokens_refresco")
public class TokenRefrescoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected TokenRefrescoEntity() {
    }

    public TokenRefrescoEntity(Integer id, Integer idUsuario, String tokenHash, Instant expiraEn,
                               Instant revocadoEn, Instant creadoEn) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.revocadoEn = revocadoEn;
        this.creadoEn = creadoEn;
    }

    public static TokenRefrescoEntity deDominio(TokenRefresco d) {
        return new TokenRefrescoEntity(
                d.id(), d.idUsuario(), d.tokenHash(), d.expiraEn(), d.revocadoEn(), d.creadoEn());
    }

    public TokenRefresco aDominio() {
        return new TokenRefresco(id, idUsuario, tokenHash, expiraEn, revocadoEn, creadoEn);
    }
}
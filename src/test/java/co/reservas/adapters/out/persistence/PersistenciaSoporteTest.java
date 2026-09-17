package co.reservas.adapters.out.persistence;

import co.reservas.domain.reserva.EstadoReserva;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.EstadoUsuario;
import co.reservas.domain.usuario.Rol;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistenciaSoporteTest {

    private static CatalogoSistemaJpaRepository.FilaEstado fila(int id, String tipo, String nombre) {
        return new CatalogoSistemaJpaRepository.FilaEstado() {
            @Override
            public Integer getId() {
                return id;
            }

            @Override
            public String getTipo() {
                return tipo;
            }

            @Override
            public String getNombre() {
                return nombre;
            }
        };
    }

    private static RolEntity rol(int id, String nombre) {
        RolEntity rol = mock(RolEntity.class);
        when(rol.getId()).thenReturn(id);
        when(rol.getNombre()).thenReturn(nombre);
        return rol;
    }

    @Test
    @DisplayName("Dado las filas semilla, cuando se traducen enums, entonces usa el id del tipo correcto aunque el nombre se repita y carga el catálogo una sola vez")
    void catalogoSistema() {
        // Arrange
        CatalogoSistemaJpaRepository repositorio = mock(CatalogoSistemaJpaRepository.class);
        List<RolEntity> roles = List.of(rol(1, "CLIENTE"), rol(2, "PROVEEDOR"));
        when(repositorio.listarRoles()).thenReturn(roles);
        when(repositorio.listarEstados()).thenReturn(List.of(fila(2, "USUARIO", "ACTIVO"),
                fila(4, "SERVICIO", "ACTIVO"), fila(6, "RESERVA", "CONFIRMADA")));
        CatalogoSistema catalogo = new CatalogoSistema(repositorio);

        // Act - Assert
        assertThat(catalogo.idRol(Rol.PROVEEDOR)).isEqualTo(2);
        assertThat(catalogo.rol(1)).isEqualTo(Rol.CLIENTE);
        assertThat(catalogo.idEstado(EstadoUsuario.ACTIVO)).isEqualTo(2);
        assertThat(catalogo.idEstado(EstadoServicio.ACTIVO)).isEqualTo(4);
        assertThat(catalogo.idEstado(EstadoReserva.CONFIRMADA)).isEqualTo(6);
        assertThat(catalogo.estadoUsuario(2)).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(catalogo.estadoServicio(4)).isEqualTo(EstadoServicio.ACTIVO);
        assertThat(catalogo.estadoReserva(6)).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(catalogo.estadoReserva(null)).isNull();
        assertThat(catalogo.idEstadoReservaONulo(null)).isNull();
        assertThatThrownBy(() -> catalogo.idEstado(EstadoReserva.CANCELADA)).isInstanceOf(IllegalStateException.class);
        verify(repositorio, times(1)).listarEstados();
    }

    @Test
    @DisplayName("Dado una violación de la restricción única esperada, cuando se guarda, entonces se traduce al error de negocio; otras violaciones se propagan")
    void restriccionesUnicas() {
        // Arrange
        DataIntegrityViolationException duplicado = new DataIntegrityViolationException("dup",
                new ConstraintViolationException("dup", new SQLException(), "UK_USUARIOS_EMAIL"));
        DataIntegrityViolationException otra = new DataIntegrityViolationException("fk",
                new ConstraintViolationException("fk", new SQLException(), "fk_algo"));

        // Act - Assert
        assertThat(RestriccionesUnicas.traducir(() -> "ok", "uk_usuarios_email",
                () -> new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO, "x"))).isEqualTo("ok");
        assertThatThrownBy(() -> RestriccionesUnicas.traducir(() -> {
            throw duplicado;
        }, "uk_usuarios_email", () -> new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO, "x")))
                .isInstanceOfSatisfying(ExcepcionNegocio.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.EMAIL_YA_REGISTRADO));
        assertThatThrownBy(() -> RestriccionesUnicas.traducir(() -> {
            throw otra;
        }, "uk_usuarios_email", () -> new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO, "x")))
                .isSameAs(otra);
        assertThatThrownBy(() -> RestriccionesUnicas.traducir(() -> {
            throw new DataIntegrityViolationException("sin causa");
        }, "uk_usuarios_email", () -> new ExcepcionNegocio(CodigoError.EMAIL_YA_REGISTRADO, "x")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

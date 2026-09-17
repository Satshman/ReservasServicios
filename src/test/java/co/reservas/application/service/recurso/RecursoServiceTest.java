package co.reservas.application.service.recurso;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.recurso.CrearRecursoComando;
import co.reservas.application.port.in.recurso.RecursoResultado;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {

    private static final UsuarioAutenticado USUARIO = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Proveedor PROVEEDOR = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final ServicioPropio PROPIO = new ServicioPropio(
            new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null, Duration.ofMinutes(30), 1), PROVEEDOR);

    @Mock
    private RecursoRepositoryPort recursos;
    @Mock
    private CatalogoRepositoryPort catalogos;
    @Mock
    private VerificadorPropiedad verificador;

    private RecursoService servicio;

    @BeforeEach
    void configurar() {
        servicio = new RecursoService(recursos, catalogos, verificador);
    }

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado recursos propios y activos, cuando se asignan a un servicio propio, entonces reemplaza las asignaciones y los devuelve ordenados por id")
    void asignaRecursosPropios() {
        // Arrange
        when(verificador.servicioPropioBloqueado(USUARIO, 7)).thenReturn(PROPIO);
        when(recursos.buscarPorIds(Set.of(4, 2))).thenReturn(List.of(new Recurso(4, 3, 1, "Sala B", true),
                new Recurso(2, 3, 2, "Equipo", true)));

        // Act
        List<RecursoResultado> asignados = servicio.asignarAServicio(USUARIO, 7, Set.of(4, 2));

        // Assert
        assertThat(asignados).extracting(RecursoResultado::id).containsExactly(2, 4);
        verify(recursos).reemplazarAsignaciones(eq(7), eq(Set.of(2, 4)));
    }

    @Test
    @DisplayName("Dado un recurso inexistente o de otro proveedor, cuando se asigna, entonces responde RECURSO_NO_ENCONTRADO o ACCESO_DENEGADO")
    void recursosNoAsignables() {
        // Arrange
        when(verificador.servicioPropioBloqueado(USUARIO, 7)).thenReturn(PROPIO);
        when(recursos.buscarPorIds(Set.of(9))).thenReturn(List.of());
        when(recursos.buscarPorIds(Set.of(5))).thenReturn(List.of(new Recurso(5, 99, 1, "Ajeno", true)));

        // Act - Assert
        assertCodigo(() -> servicio.asignarAServicio(USUARIO, 7, Set.of(9)), CodigoError.RECURSO_NO_ENCONTRADO);
        assertCodigo(() -> servicio.asignarAServicio(USUARIO, 7, Set.of(5)), CodigoError.ACCESO_DENEGADO);
        verify(recursos, never()).reemplazarAsignaciones(any(), any());
    }

    @Test
    @DisplayName("Dado un tipo inexistente o un nombre repetido, cuando crea un recurso, entonces responde 404 o 409 sin guardar")
    void crearConErrores() {
        // Arrange
        when(verificador.proveedorDe(USUARIO)).thenReturn(PROVEEDOR);
        when(catalogos.existeTipoRecurso(9)).thenReturn(false);
        when(catalogos.existeTipoRecurso(1)).thenReturn(true);
        when(recursos.existeNombre(3, "Sala")).thenReturn(true);

        // Act - Assert
        assertCodigo(() -> servicio.crear(USUARIO, new CrearRecursoComando("Sala", 9)),
                CodigoError.TIPO_RECURSO_NO_ENCONTRADO);
        assertCodigo(() -> servicio.crear(USUARIO, new CrearRecursoComando("Sala", 1)),
                CodigoError.RECURSO_YA_REGISTRADO);
        verify(recursos, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado recursos del proveedor, cuando los lista, entonces se devuelven ordenados por id")
    void listar() {
        // Arrange
        when(verificador.proveedorDe(USUARIO)).thenReturn(PROVEEDOR);
        when(recursos.listarPorProveedor(3)).thenReturn(List.of(new Recurso(8, 3, 1, "B", true),
                new Recurso(3, 3, 1, "A", false)));

        // Act
        List<RecursoResultado> lista = servicio.listar(USUARIO);

        // Assert
        assertThat(lista).extracting(RecursoResultado::id).containsExactly(3, 8);
    }
}

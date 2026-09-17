package co.reservas.application.service.servicio;

import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.recurso.CrearRecursoComando;
import co.reservas.application.port.in.recurso.RecursoResultado;
import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.in.servicio.ServicioResultado;
import co.reservas.application.port.out.recurso.RecursoRepositoryPort;
import co.reservas.application.port.out.servicio.CatalogoRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.port.out.usuario.UsuarioRepositoryPort;
import co.reservas.application.service.recurso.RecursoService;
import co.reservas.domain.recurso.Recurso;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.servicio.Servicio;
import co.reservas.domain.shared.CodigoError;
import co.reservas.domain.shared.DetalleCampo;
import co.reservas.domain.shared.ElementoCatalogo;
import co.reservas.domain.shared.ExcepcionNegocio;
import co.reservas.domain.usuario.Proveedor;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioYRecursoServiciosTest {

    private static final UsuarioAutenticado PROVEEDOR = new UsuarioAutenticado(20, Rol.PROVEEDOR);
    private static final Proveedor PERFIL = new Proveedor(3, 20, "Clínica", ZoneId.of("America/Bogota"));
    private static final Servicio SERVICIO = new Servicio(7, 3, 1, EstadoServicio.ACTIVO, "Consulta", null,
            Duration.ofMinutes(30), 1);

    @Mock
    private UsuarioRepositoryPort usuarios;
    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private CatalogoRepositoryPort catalogos;
    @Mock
    private RecursoRepositoryPort recursos;

    private static void assertCodigo(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    @DisplayName("Dado un servicio de otro proveedor, un cliente o un servicio inexistente, cuando se verifica la propiedad, entonces responde 403 o 404")
    void verificadorPropiedad() {
        // Arrange
        VerificadorPropiedad verificador = new VerificadorPropiedad(usuarios, servicios);
        Servicio ajeno = new Servicio(8, 99, 1, EstadoServicio.ACTIVO, "Ajeno", null, Duration.ofMinutes(30), 1);
        when(servicios.bloquearPorId(8)).thenReturn(Optional.of(ajeno));
        when(servicios.buscarPorId(404)).thenReturn(Optional.empty());
        when(usuarios.buscarProveedorPorUsuario(20)).thenReturn(Optional.of(PERFIL));

        // Act - Assert
        assertCodigo(() -> verificador.servicioPropioBloqueado(PROVEEDOR, 8), CodigoError.ACCESO_DENEGADO);
        assertCodigo(() -> verificador.servicioPropio(PROVEEDOR, 404), CodigoError.SERVICIO_NO_ENCONTRADO);
        assertCodigo(() -> verificador.proveedorDe(new UsuarioAutenticado(1, Rol.CLIENTE)), CodigoError.ACCESO_DENEGADO);
    }

    @Test
    @DisplayName("Dado nombres repetidos o una categoría inexistente, cuando se validan servicios nuevos, entonces el error indica el campo con su índice")
    void registroServiciosValida() {
        // Arrange
        RegistroServicios registro = new RegistroServicios(servicios, catalogos);
        when(catalogos.existeCategoria(1)).thenReturn(true);
        when(catalogos.existeCategoria(9)).thenReturn(false);
        List<NuevoServicioComando> repetidos = List.of(new NuevoServicioComando("A", null, 1, 30, 1),
                new NuevoServicioComando("A", null, 1, 30, 1));
        List<NuevoServicioComando> categoria = List.of(new NuevoServicioComando("B", null, 9, 30, 1));
        List<NuevoServicioComando> duracion = List.of(new NuevoServicioComando("C", null, 1, 2, 1));

        // Act - Assert
        assertThatThrownBy(() -> registro.validar(repetidos, i -> "servicios[" + i + "]."))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getDetalles())
                        .containsExactly(new DetalleCampo("servicios[1].nombre", "El nombre del servicio está repetido")));
        assertCodigo(() -> registro.validar(categoria, i -> ""), CodigoError.CATEGORIA_NO_ENCONTRADA);
        assertThatThrownBy(() -> registro.validar(duracion, i -> "servicios[" + i + "]."))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.getDetalles())
                        .extracting(detalle -> ((DetalleCampo) detalle).campo())
                        .containsExactly("servicios[0].duracionMinutos"));
    }

    @Test
    @DisplayName("Dado un nombre ya usado por el proveedor, cuando crea un servicio, entonces responde SERVICIO_YA_REGISTRADO")
    void servicioDuplicado() {
        // Arrange
        VerificadorPropiedad verificador = new VerificadorPropiedad(usuarios, servicios);
        ServicioService servicioService = new ServicioService(servicios, usuarios,
                new RegistroServicios(servicios, catalogos), verificador);
        when(usuarios.buscarProveedorPorUsuario(20)).thenReturn(Optional.of(PERFIL));
        when(catalogos.existeCategoria(1)).thenReturn(true);
        when(servicios.existeNombre(3, "Consulta")).thenReturn(true);

        // Act - Assert
        assertCodigo(() -> servicioService.crear(PROVEEDOR, new NuevoServicioComando(" Consulta ", null, 1, 30, 1)),
                CodigoError.SERVICIO_YA_REGISTRADO);
        verify(servicios, never()).guardar(any());
    }

    @Test
    @DisplayName("Dado servicios activos de un proveedor, cuando se listan, entonces incluyen el nombre comercial y la zona horaria")
    void listarServicios() {
        // Arrange
        ServicioService servicioService = new ServicioService(servicios, usuarios,
                new RegistroServicios(servicios, catalogos), new VerificadorPropiedad(usuarios, servicios));
        when(servicios.listarActivos(null, 1)).thenReturn(List.of(SERVICIO));
        when(usuarios.buscarProveedoresPorIds(List.of(3))).thenReturn(List.of(PERFIL));

        // Act
        List<ServicioResultado> lista = servicioService.listar(null, 1);

        // Assert
        assertThat(lista).singleElement().satisfies(resultado -> {
            assertThat(resultado.nombreProveedor()).isEqualTo("Clínica");
            assertThat(resultado.zonaHoraria()).isEqualTo("America/Bogota");
            assertThat(resultado.duracionMinutos()).isEqualTo(30);
        });
    }

    @Test
    @DisplayName("Dado un recurso inactivo, cuando se asigna a un servicio, entonces responde VALIDACION_FALLIDA sin reemplazar asignaciones")
    void recursoInactivo() {
        // Arrange
        VerificadorPropiedad verificador = new VerificadorPropiedad(usuarios, servicios);
        RecursoService recursoService = new RecursoService(recursos, catalogos, verificador);
        when(servicios.bloquearPorId(7)).thenReturn(Optional.of(SERVICIO));
        when(usuarios.buscarProveedorPorUsuario(20)).thenReturn(Optional.of(PERFIL));
        when(recursos.buscarPorIds(Set.of(5))).thenReturn(List.of(new Recurso(5, 3, 1, "Sala", false)));

        // Act - Assert
        assertCodigo(() -> recursoService.asignarAServicio(PROVEEDOR, 7, Set.of(5)), CodigoError.VALIDACION_FALLIDA);
        verify(recursos, never()).reemplazarAsignaciones(any(), any());
    }

    @Test
    @DisplayName("Dado un tipo de recurso existente y un nombre libre, cuando crea un recurso, entonces se guarda activo")
    void crearRecurso() {
        // Arrange
        RecursoService recursoService = new RecursoService(recursos, catalogos,
                new VerificadorPropiedad(usuarios, servicios));
        when(usuarios.buscarProveedorPorUsuario(20)).thenReturn(Optional.of(PERFIL));
        when(catalogos.existeTipoRecurso(1)).thenReturn(true);
        when(recursos.guardar(any())).thenAnswer(invocacion -> {
            Recurso r = invocacion.getArgument(0);
            return new Recurso(50, r.idProveedor(), r.idTipoRecurso(), r.nombre(), r.activo());
        });

        // Act
        RecursoResultado resultado = recursoService.crear(PROVEEDOR, new CrearRecursoComando(" Sala 1 ", 1));

        // Assert
        assertThat(resultado).isEqualTo(new RecursoResultado(50, 1, "Sala 1", true));
    }

    @Test
    @DisplayName("Dado los catálogos, cuando se consultan, entonces se delegan al repositorio")
    void catalogos() {
        // Arrange
        CatalogoService catalogoService = new CatalogoService(catalogos);
        when(catalogos.listarCategorias()).thenReturn(List.of(new ElementoCatalogo(1, "SALUD")));
        when(catalogos.listarTiposRecurso()).thenReturn(List.of(new ElementoCatalogo(1, "SALA")));
        when(catalogos.listarTiposDocumento()).thenReturn(List.of(new ElementoCatalogo(1, "CC")));

        // Act - Assert
        assertThat(catalogoService.categorias()).extracting(ElementoCatalogo::nombre).containsExactly("SALUD");
        assertThat(catalogoService.tiposRecurso()).extracting(ElementoCatalogo::nombre).containsExactly("SALA");
        assertThat(catalogoService.tiposDocumento()).extracting(ElementoCatalogo::nombre).containsExactly("CC");
    }
}

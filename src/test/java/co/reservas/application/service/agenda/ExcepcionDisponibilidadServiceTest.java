package co.reservas.application.service.agenda;

import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase.CrearExcepcionComando;
import co.reservas.application.port.in.agenda.GestionarExcepcionesDisponibilidadUseCase.ExcepcionResultado;
import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.out.agenda.ExcepcionDisponibilidadRepositoryPort;
import co.reservas.application.port.out.servicio.ServicioRepositoryPort;
import co.reservas.application.service.servicio.ServicioPropio;
import co.reservas.application.service.servicio.VerificadorPropiedad;
import co.reservas.domain.agenda.ExcepcionDisponibilidad;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcepcionDisponibilidadServiceTest {

    @Mock
    private ExcepcionDisponibilidadRepositoryPort excepciones;
    @Mock
    private ServicioRepositoryPort servicios;
    @Mock
    private VerificadorPropiedad verificador;

    private ExcepcionDisponibilidadService service;

    private final UsuarioAutenticado usuarioProveedor = new UsuarioAutenticado(10, Rol.PROVEEDOR);
    private final Proveedor proveedor = new Proveedor(1, 10, "Clínica Central", ZoneId.of("America/Bogota"));
    private final Servicio servicio = new Servicio(
            100, 1, 1, EstadoServicio.ACTIVO, "Consulta", "Desc", Duration.ofMinutes(30), 1);

    @BeforeEach
    void setUp() {
        service = new ExcepcionDisponibilidadService(excepciones, servicios, verificador);
    }

    @Test
    @DisplayName("Dado un proveedor dueño del servicio, cuando registra una excepción de disponibilidad válida, entonces se guarda y bloquea los turnos de esa franja")
    void creaExcepcionYBloqueaTurnosEnFranja() {
        // Arrange
        LocalDate fecha = LocalDate.of(2026, 12, 24);
        ZoneId zona = ZoneId.of("America/Bogota");
        ExcepcionDisponibilidad guardada = new ExcepcionDisponibilidad(
                5, 100, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0), "Mantenimiento");
        when(verificador.servicioPropio(usuarioProveedor, 100))
                .thenReturn(new ServicioPropio(servicio, proveedor));
        when(excepciones.guardar(any())).thenReturn(guardada);

        // Act
        ExcepcionResultado resultado = service.crear(usuarioProveedor, 100,
                new CrearExcepcionComando(fecha, LocalTime.of(10, 0), LocalTime.of(12, 0), "Mantenimiento"));

        // Assert
        assertEquals(5, resultado.id());
        Instant turno1030 = ZonedDateTime.of(fecha, LocalTime.of(10, 30), zona).toInstant();
        Instant turno0800 = ZonedDateTime.of(fecha, LocalTime.of(8, 0), zona).toInstant();
        assertTrue(guardada.bloquea(turno1030, Duration.ofMinutes(30), zona));
        assertFalse(guardada.bloquea(turno0800, Duration.ofMinutes(30), zona));
    }

    @Test
    @DisplayName("Dada una excepción con hora de inicio pero sin hora de fin, cuando se intenta crear, entonces falla con validación")
    void rechazaExcepcionConUnaSolaHora() {
        // Arrange
        when(verificador.servicioPropio(usuarioProveedor, 100))
                .thenReturn(new ServicioPropio(servicio, proveedor));
        CrearExcepcionComando comandoInvalido = new CrearExcepcionComando(
                LocalDate.of(2026, 12, 24), LocalTime.of(10, 0), null, "Festivo");

        // Act & Assert
        assertThrows(ExcepcionNegocio.class,
                () -> service.crear(usuarioProveedor, 100, comandoInvalido));
    }

    @Test
    @DisplayName("Dado un servicio con excepciones registradas, cuando se listan, entonces devuelve una por cada una en el mismo orden del repositorio")
    void listaLasExcepcionesDelServicio() {
        // Arrange
        ExcepcionDisponibilidad navidad = new ExcepcionDisponibilidad(
                5, 100, LocalDate.of(2026, 12, 25), null, null, "Festivo");
        ExcepcionDisponibilidad mantenimiento = new ExcepcionDisponibilidad(
                6, 100, LocalDate.of(2026, 12, 24), LocalTime.of(10, 0), LocalTime.of(12, 0), "Mantenimiento");
        when(servicios.buscarPorId(100)).thenReturn(Optional.of(servicio));
        when(excepciones.listarPorServicio(100)).thenReturn(List.of(navidad, mantenimiento));

        // Act
        List<ExcepcionResultado> resultado = service.listarPorServicio(100);

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(5, resultado.get(0).id());
        assertEquals("Festivo", resultado.get(0).motivo());
        // Una excepción de día completo no lleva horas.
        assertNull(resultado.get(0).horaInicio());
        assertEquals(LocalTime.of(10, 0), resultado.get(1).horaInicio());
    }

    @Test
    @DisplayName("Dado un servicio que no existe, cuando se listan sus excepciones, entonces falla con SERVICIO_NO_ENCONTRADO")
    void listarExigeQueElServicioExista() {
        // Arrange
        when(servicios.buscarPorId(404)).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class, () -> service.listarPorServicio(404));
        assertEquals(CodigoError.SERVICIO_NO_ENCONTRADO, error.getCodigo());
        verify(excepciones, never()).listarPorServicio(any());
    }

    @Test
    @DisplayName("Dado el proveedor dueño del servicio, cuando elimina una excepción existente, entonces verifica la propiedad y la borra")
    void eliminaExcepcionPropia() {
        // Arrange
        ExcepcionDisponibilidad existente = new ExcepcionDisponibilidad(
                5, 100, LocalDate.of(2026, 12, 25), null, null, "Festivo");
        when(excepciones.buscarPorId(5)).thenReturn(Optional.of(existente));
        when(verificador.servicioPropio(usuarioProveedor, 100))
                .thenReturn(new ServicioPropio(servicio, proveedor));

        // Act
        service.eliminar(usuarioProveedor, 5);

        // Assert: la propiedad se verifica contra el servicio de la excepción, no contra uno que llegue por fuera.
        verify(verificador).servicioPropio(usuarioProveedor, 100);
        verify(excepciones).eliminar(5);
    }

    @Test
    @DisplayName("Dada una excepción que no existe, cuando se intenta eliminar, entonces falla con HORARIO_NO_ENCONTRADO y no borra nada")
    void eliminarExcepcionInexistente() {
        // Arrange
        when(excepciones.buscarPorId(404)).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class,
                () -> service.eliminar(usuarioProveedor, 404));
        assertEquals(CodigoError.HORARIO_NO_ENCONTRADO, error.getCodigo());
        verify(excepciones, never()).eliminar(any());
    }
}
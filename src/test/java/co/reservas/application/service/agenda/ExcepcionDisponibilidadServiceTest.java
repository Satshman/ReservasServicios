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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
}
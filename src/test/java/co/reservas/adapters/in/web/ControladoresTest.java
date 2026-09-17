package co.reservas.adapters.in.web;

import co.reservas.adapters.in.web.agenda.AgendaController;
import co.reservas.adapters.in.web.agenda.CrearHorarioRequest;
import co.reservas.adapters.in.web.agenda.EditarHorarioRequest;
import co.reservas.adapters.in.web.auth.AuthController;
import co.reservas.adapters.in.web.auth.LoginRequest;
import co.reservas.adapters.in.web.auth.ReenvioVerificacionRequest;
import co.reservas.adapters.in.web.auth.RegistroRequest;
import co.reservas.adapters.in.web.recurso.AsignarRecursosRequest;
import co.reservas.adapters.in.web.recurso.RecursoController;
import co.reservas.adapters.in.web.recurso.RecursoRequest;
import co.reservas.adapters.in.web.reserva.ReservaController;
import co.reservas.adapters.in.web.reserva.ReservaRequest;
import co.reservas.adapters.in.web.servicio.CatalogoController;
import co.reservas.adapters.in.web.servicio.ServicioController;
import co.reservas.adapters.in.web.servicio.ServicioRequest;
import co.reservas.application.port.in.agenda.CambioHorarioResultado;
import co.reservas.application.port.in.agenda.ConsultarHorariosUseCase;
import co.reservas.application.port.in.agenda.CrearHorarioComando;
import co.reservas.application.port.in.agenda.CrearHorarioUseCase;
import co.reservas.application.port.in.agenda.EditarHorarioComando;
import co.reservas.application.port.in.agenda.EditarHorarioUseCase;
import co.reservas.application.port.in.agenda.EliminarHorarioUseCase;
import co.reservas.application.port.in.agenda.ReservaAfectada;
import co.reservas.application.port.in.auth.IniciarSesionUseCase;
import co.reservas.application.port.in.auth.RegistrarUsuarioComando;
import co.reservas.application.port.in.auth.RegistrarUsuarioUseCase;
import co.reservas.application.port.in.auth.ReenviarVerificacionUseCase;
import co.reservas.application.port.in.auth.UsuarioAutenticado;
import co.reservas.application.port.in.auth.VerificarCuentaUseCase;
import co.reservas.application.port.in.recurso.CrearRecursoComando;
import co.reservas.application.port.in.recurso.GestionarRecursosUseCase;
import co.reservas.application.port.in.reserva.ConsultarDisponibilidadUseCase;
import co.reservas.application.port.in.reserva.ConsultarReservasUseCase;
import co.reservas.application.port.in.reserva.CrearReservaComando;
import co.reservas.application.port.in.reserva.CrearReservaUseCase;
import co.reservas.application.port.in.servicio.ConsultarCatalogosUseCase;
import co.reservas.application.port.in.servicio.GestionarServiciosUseCase;
import co.reservas.application.port.in.servicio.NuevoServicioComando;
import co.reservas.application.port.in.servicio.ServicioResultado;
import co.reservas.domain.servicio.EstadoServicio;
import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los controladores solo traducen DTOs web a comandos y extraen la identidad del JWT.
 */
class ControladoresTest {

    private static Jwt jwt(int idUsuario, Rol rol) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(idUsuario))
                .claim("rol", rol.name()).build();
    }

    @Test
    @DisplayName("Dado un registro con correo con espacios y servicios, cuando llega al controlador, entonces se envía el comando normalizado al caso de uso")
    void authController() {
        // Arrange
        RegistrarUsuarioUseCase registrar = mock(RegistrarUsuarioUseCase.class);
        VerificarCuentaUseCase verificar = mock(VerificarCuentaUseCase.class);
        ReenviarVerificacionUseCase reenviar = mock(ReenviarVerificacionUseCase.class);
        IniciarSesionUseCase iniciar = mock(IniciarSesionUseCase.class);
        AuthController controlador = new AuthController(registrar, verificar, reenviar, iniciar);
        RegistroRequest solicitud = new RegistroRequest("Ana", " ana@prueba.co ", "clave-larga-segura", "PROVEEDOR",
                null, "Ana SAS", "America/Bogota",
                List.of(new ServicioRequest("Consulta", null, 1, 30, null)));

        // Act
        controlador.registrar(solicitud);
        controlador.verificar("abc");
        controlador.reenviar(new ReenvioVerificacionRequest(" ana@prueba.co "));
        controlador.login(new LoginRequest("ana@prueba.co", "clave"));

        // Assert
        verify(registrar).registrar(new RegistrarUsuarioComando("Ana", "ana@prueba.co", "clave-larga-segura",
                Rol.PROVEEDOR, null, "Ana SAS", "America/Bogota",
                List.of(new NuevoServicioComando("Consulta", null, 1, 30, null))));
        verify(verificar).verificar("abc");
        verify(reenviar).reenviar("ana@prueba.co");
        verify(iniciar).iniciarSesion("ana@prueba.co", "clave");
    }

    @Test
    @DisplayName("Dado un proveedor autenticado, cuando crea un servicio, entonces responde con la ubicación del recurso creado")
    void servicioYCatalogoController() {
        // Arrange
        GestionarServiciosUseCase servicios = mock(GestionarServiciosUseCase.class);
        ConsultarCatalogosUseCase catalogos = mock(ConsultarCatalogosUseCase.class);
        ServicioResultado creado = new ServicioResultado(15, 3, "Clínica", "America/Bogota", 1, "Consulta", null, 30, 1,
                EstadoServicio.ACTIVO);
        when(servicios.crear(any(), any())).thenReturn(creado);
        ServicioController controlador = new ServicioController(servicios);
        CatalogoController catalogo = new CatalogoController(catalogos);

        // Act
        ResponseEntity<ServicioResultado> respuesta = controlador.crear(jwt(20, Rol.PROVEEDOR),
                new ServicioRequest("Consulta", null, 1, 30, 1));
        controlador.listar(3, null);
        controlador.consultar(15);
        catalogo.categorias();
        catalogo.tiposRecurso();
        catalogo.tiposDocumento();

        // Assert
        assertThat(respuesta.getHeaders().getLocation()).hasToString("/api/v1/servicios/15");
        verify(servicios).crear(new UsuarioAutenticado(20, Rol.PROVEEDOR),
                new NuevoServicioComando("Consulta", null, 1, 30, 1));
        verify(servicios).listar(3, null);
        verify(catalogos).tiposDocumento();
    }

    @Test
    @DisplayName("Dado un proveedor autenticado, cuando gestiona la agenda y los recursos, entonces cada operación llega al caso de uso con su identidad")
    void agendaYRecursoController() {
        // Arrange
        CrearHorarioUseCase crear = mock(CrearHorarioUseCase.class);
        ConsultarHorariosUseCase consultar = mock(ConsultarHorariosUseCase.class);
        EditarHorarioUseCase editar = mock(EditarHorarioUseCase.class);
        EliminarHorarioUseCase eliminar = mock(EliminarHorarioUseCase.class);
        GestionarRecursosUseCase recursos = mock(GestionarRecursosUseCase.class);
        UsuarioAutenticado proveedor = new UsuarioAutenticado(20, Rol.PROVEEDOR);
        ReservaAfectada afectada = new ReservaAfectada(1, OffsetDateTime.parse("2026-10-12T10:00:00-05:00"),
                OffsetDateTime.parse("2026-10-12T10:30:00-05:00"));
        when(eliminar.eliminar(proveedor, 9, true)).thenReturn(new CambioHorarioResultado(null, List.of(afectada)));
        AgendaController agenda = new AgendaController(crear, consultar, editar, eliminar);
        RecursoController recurso = new RecursoController(recursos);

        // Act
        agenda.crear(jwt(20, Rol.PROVEEDOR), 7, new CrearHorarioRequest(Set.of(1), LocalTime.of(8, 0),
                LocalTime.of(12, 0)));
        agenda.consultar(7);
        agenda.editar(jwt(20, Rol.PROVEEDOR), 9, false, new EditarHorarioRequest(2, LocalTime.of(9, 0),
                LocalTime.of(10, 0)));
        var eliminacion = agenda.eliminar(jwt(20, Rol.PROVEEDOR), 9, true);
        recurso.crear(jwt(20, Rol.PROVEEDOR), new RecursoRequest("Sala", 1));
        recurso.listar(jwt(20, Rol.PROVEEDOR));
        recurso.asignar(jwt(20, Rol.PROVEEDOR), 7, new AsignarRecursosRequest(Set.of(4)));

        // Assert
        verify(crear).crear(proveedor, 7, new CrearHorarioComando(Set.of(1), LocalTime.of(8, 0), LocalTime.of(12, 0)));
        verify(consultar).consultar(7);
        verify(editar).editar(proveedor, 9, new EditarHorarioComando(2, LocalTime.of(9, 0), LocalTime.of(10, 0)), false);
        assertThat(eliminacion.reservasAfectadas()).containsExactly(afectada);
        verify(recursos).crear(proveedor, new CrearRecursoComando("Sala", 1));
        verify(recursos).listar(proveedor);
        verify(recursos).asignarAServicio(proveedor, 7, Set.of(4));
    }

    @Test
    @DisplayName("Dado un cliente autenticado, cuando consulta disponibilidad y reserva, entonces se invocan los casos de uso con sus datos")
    void reservaController() {
        // Arrange
        CrearReservaUseCase crear = mock(CrearReservaUseCase.class);
        ConsultarReservasUseCase consultar = mock(ConsultarReservasUseCase.class);
        ConsultarDisponibilidadUseCase disponibilidad = mock(ConsultarDisponibilidadUseCase.class);
        ReservaController controlador = new ReservaController(crear, consultar, disponibilidad);
        OffsetDateTime inicio = OffsetDateTime.parse("2026-10-12T10:00:00-05:00");
        LocalDate lunes = LocalDate.of(2026, 10, 12);

        // Act
        controlador.disponibilidad(7, lunes, lunes);
        controlador.crear(jwt(30, Rol.CLIENTE), new ReservaRequest(7, inicio));
        controlador.mias(jwt(30, Rol.CLIENTE));
        controlador.porServicio(jwt(20, Rol.PROVEEDOR), 7);

        // Assert
        verify(disponibilidad).consultar(7, lunes, lunes);
        verify(crear).crear(new UsuarioAutenticado(30, Rol.CLIENTE), new CrearReservaComando(7, inicio));
        verify(consultar).consultarMias(new UsuarioAutenticado(30, Rol.CLIENTE));
        verify(consultar).consultarPorServicio(new UsuarioAutenticado(20, Rol.PROVEEDOR), 7);
    }
}

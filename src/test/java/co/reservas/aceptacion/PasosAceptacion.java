package co.reservas.aceptacion;

import co.reservas.application.port.in.auth.AprovisionarAdministradorUseCase;
import co.reservas.soporte.PruebaIntegracion;
import io.cucumber.java.After;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pasos de los escenarios Gherkin de {@code src/test/resources/features}. Reutiliza el contexto de las pruebas de
 * integración (PostgreSQL en contenedor, reloj ajustable y correo capturado) y recorre la API como lo haría un cliente
 * HTTP. Cucumber crea una instancia por escenario, así que el estado de los campos no se comparte entre escenarios.
 */
@CucumberContextConfiguration
public class PasosAceptacion extends PruebaIntegracion {

    private static final Map<String, Integer> DIAS = Map.of("lunes", 1, "martes", 2, "miércoles", 3, "jueves", 4,
            "viernes", 5, "sábado", 6, "domingo", 7);
    private static final String ZONA = "-05:00";

    @Autowired
    private AprovisionarAdministradorUseCase aprovisionador;

    private ProveedorDePrueba proveedor;
    private String emailAdmin;
    private String tokenCliente;
    private String emailCliente;
    private String refreshToken;
    private Integer idReserva;
    private MvcResult respuesta;

    @After
    public void restablecerReloj() {
        reloj.restablecer();
        notificaciones.simularFallo(false);
    }

    private static String instante(String fecha, String hora) {
        return fecha + "T" + hora + ":00" + ZONA;
    }

    private JsonNode cuerpo() throws Exception {
        return leer(respuesta);
    }

    // ---- Contexto ----

    @Dado("un proveedor con el servicio {string} de {int} minutos y capacidad {int}")
    public void unProveedorConServicio(String nombre, int duracion, int capacidad) throws Exception {
        proveedor = proveedorConServicios(List.of(servicio(nombre, duracion, capacidad)));
    }

    @Dado("el servicio atiende los {word} de {string} a {string}")
    public void elServicioAtiende(String dia, String inicio, String fin) throws Exception {
        crearHorario(proveedor.token(), proveedor.idServicio(), List.of(DIAS.get(dia)), inicio, fin);
    }

    @Dado("un cliente verificado con sesión iniciada")
    @Dado("otro cliente verificado con sesión iniciada")
    public void unClienteConSesion() throws Exception {
        emailCliente = emailUnico("cliente");
        registrarCliente(emailCliente).andExpect(status().isCreated());
        verificarCuenta(emailCliente);
        tokenCliente = tokenDe(emailCliente);
    }

    @Dado("un cliente registrado y verificado")
    public void unClienteRegistradoYVerificado() throws Exception {
        emailCliente = emailUnico("cliente");
        registrarCliente(emailCliente).andExpect(status().isCreated());
        verificarCuenta(emailCliente);
    }

    @Dado("el cliente ya reservó el turno del {string} a las {string}")
    public void elClienteYaReservo(String fecha, String hora) throws Exception {
        MvcResult resultado = reservar(tokenCliente, proveedor.idServicio(), instante(fecha, hora))
                .andExpect(status().isCreated()).andReturn();
        idReserva = leer(resultado).get("id").asInt();
    }

    @Dado("han pasado {int} días")
    public void hanPasadoDias(int dias) throws Exception {
        reloj.avanzar(Duration.ofDays(dias));
        // El token de acceso dura 30 minutos: tras días, cliente y proveedor vuelven a iniciar sesión.
        if (tokenCliente != null) {
            tokenCliente = tokenDe(emailCliente);
        }
        if (proveedor != null) {
            proveedor = new ProveedorDePrueba(proveedor.email(), tokenDe(proveedor.email()), proveedor.idProveedor(),
                    proveedor.idsServicios());
        }
    }

    @Dado("han pasado {int} minutos")
    public void hanPasadoMinutos(int minutos) {
        reloj.avanzar(Duration.ofMinutes(minutos));
    }

    @Dado("que falló el inicio de sesión {int} veces")
    public void fallaElInicioDeSesion(int veces) throws Exception {
        for (int intento = 0; intento < veces; intento++) {
            login(emailCliente, "clave-incorrecta-" + intento);
        }
    }

    @Dado("que inició sesión con la contraseña correcta")
    public void queInicioSesion() throws Exception {
        iniciaSesionConLaContrasenaCorrecta();
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(200);
        refreshToken = cuerpo().get("refreshToken").asString();
    }

    // ---- Acciones ----

    @Cuando("el cliente reserva el turno del {string} a las {string}")
    public void elClienteReserva(String fecha, String hora) throws Exception {
        respuesta = reservar(tokenCliente, proveedor.idServicio(), instante(fecha, hora)).andReturn();
        if (respuesta.getResponse().getStatus() == 201) {
            idReserva = cuerpo().get("id").asInt();
        }
    }

    @Cuando("el cliente cancela su reserva")
    public void elClienteCancela() throws Exception {
        respuesta = enviar(post(V1 + "/reservas/" + idReserva + "/cancelacion"), null, tokenCliente).andReturn();
    }

    @Cuando("el proveedor cancela la reserva")
    public void elProveedorCancela() throws Exception {
        respuesta = enviar(post(V1 + "/reservas/" + idReserva + "/cancelacion"), null, proveedor.token())
                .andReturn();
    }

    @Cuando("inicia sesión con la contraseña correcta")
    public void iniciaSesionConLaContrasenaCorrecta() throws Exception {
        respuesta = login(emailCliente, PASSWORD).andReturn();
    }

    @Cuando("cierra la sesión")
    public void cierraLaSesion() throws Exception {
        enviar(post(V1 + "/auth/logout"), Map.of("refreshToken", refreshToken), null).andExpect(status().isOk());
    }

    @Cuando("intenta renovar la sesión con su token de refresco")
    public void intentaRenovarLaSesion() throws Exception {
        respuesta = enviar(post(V1 + "/auth/refresh"), Map.of("refreshToken", refreshToken), null).andReturn();
    }

    // ---- MFA de administradores ----

    @Dado("un administrador aprovisionado desde la configuración")
    public void unAdministradorAprovisionado() {
        emailAdmin = emailUnico("admin");
        aprovisionador.aprovisionar(emailAdmin, "Administradora", PASSWORD);
    }

    @Cuando("el administrador inicia sesión por el login normal")
    public void elAdministradorIniciaSesionNormal() throws Exception {
        respuesta = login(emailAdmin, PASSWORD).andReturn();
    }

    @Cuando("el administrador solicita el código MFA con su contraseña")
    @Dado("que el administrador solicitó el código MFA")
    public void elAdministradorSolicitaElCodigo() throws Exception {
        respuesta = enviar(post(V1 + "/auth/mfa/solicitud"), Map.of("email", emailAdmin, "password", PASSWORD), null)
                .andReturn();
    }

    @Dado("verificó {int} códigos incorrectos")
    public void verificoCodigosIncorrectos(int veces) throws Exception {
        String incorrecto = codigoMfaRecibido().equals("000000") ? "111111" : "000000";
        for (int intento = 0; intento < veces; intento++) {
            verificarCodigoMfa(incorrecto);
        }
    }

    @Cuando("el administrador verifica el código recibido")
    public void elAdministradorVerificaElCodigo() throws Exception {
        verificarCodigoMfa(codigoMfaRecibido());
    }

    @Entonces("el administrador recibe un código de 6 dígitos por correo")
    public void recibeUnCodigo() {
        assertThat(codigoMfaRecibido()).matches("\\d{6}");
    }

    @Entonces("la sesión tiene el rol {string}")
    public void laSesionTieneElRol(String rol) throws Exception {
        assertThat(cuerpo().get("rol").asString()).isEqualTo(rol);
        assertThat(cuerpo().get("accessToken").asString()).isNotBlank();
    }

    private String codigoMfaRecibido() {
        return notificaciones.ultimoCodigoMfaPara(emailAdmin).orElseThrow();
    }

    private void verificarCodigoMfa(String codigo) throws Exception {
        respuesta = enviar(post(V1 + "/auth/mfa/verificacion"), Map.of("email", emailAdmin, "codigo", codigo), null)
                .andReturn();
    }

    // ---- Resultados ----

    @Entonces("la respuesta tiene el estado {int}")
    public void laRespuestaTieneElEstado(int estado) throws Exception {
        assertThat(respuesta.getResponse().getStatus())
                .as("cuerpo: %s", respuesta.getResponse().getContentAsString())
                .isEqualTo(estado);
    }

    @Entonces("el código de error es {string}")
    public void elCodigoDeErrorEs(String codigo) throws Exception {
        assertThat(cuerpo().get("errorCode").asString()).isEqualTo(codigo);
        assertThat(cuerpo().get("traceId").asString()).isNotBlank();
    }

    @Entonces("la reserva queda en estado {string}")
    public void laReservaQuedaEnEstado(String estado) throws Exception {
        assertThat(cuerpo().get("estado").asString()).isEqualTo(estado);
    }

    @Entonces("la respuesta sugiere turnos alternativos")
    public void sugiereTurnosAlternativos() throws Exception {
        assertThat(cuerpo().get("details").size()).isPositive();
    }

    @Entonces("el turno del {string} a las {string} queda con {int} cupos disponibles")
    public void elTurnoQuedaConCupos(String fecha, String hora, int cupos) throws Exception {
        JsonNode disponibilidad = leer(mockMvc.perform(get(V1 + "/servicios/" + proveedor.idServicio()
                        + "/disponibilidad").param("desde", fecha).param("hasta", fecha))
                .andExpect(status().isOk()).andReturn());
        int encontrados = 0;
        for (JsonNode turno : disponibilidad.get("turnos")) {
            if (turno.get("fechaHoraInicio").asString().startsWith(fecha + "T" + hora)) {
                encontrados = turno.get("cuposDisponibles").asInt();
            }
        }
        // Un turno sin cupos no se lista en la disponibilidad.
        assertThat(encontrados).isEqualTo(cupos);
    }

    @Entonces("recibe un token de acceso y un token de refresco")
    public void recibeTokens() throws Exception {
        assertThat(cuerpo().get("accessToken").asString()).isNotBlank();
        assertThat(cuerpo().get("refreshToken").asString()).isNotBlank();
    }
}

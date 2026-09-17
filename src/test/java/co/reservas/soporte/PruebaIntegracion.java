package co.reservas.soporte;

import co.reservas.infrastructure.ReservasServiciosApplication;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración y aceptación: contexto completo, PostgreSQL real en contenedor y MockMvc.
 */
@SpringBootTest(classes = ReservasServiciosApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ConfiguracionPruebas.class)
public abstract class PruebaIntegracion {

    protected static final String V1 = "/api/v1";
    protected static final String PASSWORD = "clave-segura-de-prueba";
    protected static final int CATEGORIA_SALUD = 1;
    protected static final int TIPO_RECURSO_SALA = 1;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected NotificacionesCapturadas notificaciones;

    @Autowired
    protected RelojAjustable reloj;

    @AfterEach
    void restablecerEstadoCompartido() {
        reloj.restablecer();
        notificaciones.simularFallo(false);
    }

    protected static String emailUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID() + "@prueba.co";
    }

    protected String json(Object cuerpo) {
        return jsonMapper.writeValueAsString(cuerpo);
    }

    protected JsonNode leer(MvcResult resultado) throws Exception {
        return jsonMapper.readTree(resultado.getResponse().getContentAsString());
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder solicitud, Object cuerpo, String token)
            throws Exception {
        if (cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(json(cuerpo));
        }
        if (token != null) {
            solicitud.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    protected static Map<String, Object> servicio(String nombre, int duracionMinutos, int capacidad) {
        return Map.of("nombre", nombre, "idCategoria", CATEGORIA_SALUD, "duracionMinutos", duracionMinutos,
                "capacidad", capacidad);
    }

    protected ResultActions registrarCliente(String email) throws Exception {
        return enviar(post(V1 + "/auth/registro"), Map.of(
                "nombreCompleto", "Cliente de Prueba", "email", email, "password", PASSWORD, "rol", "CLIENTE"), null);
    }

    protected ResultActions registrarProveedor(String email, List<Map<String, Object>> servicios) throws Exception {
        return enviar(post(V1 + "/auth/registro"), Map.of(
                "nombreCompleto", "Proveedor de Prueba", "email", email, "password", PASSWORD, "rol", "PROVEEDOR",
                "zonaHoraria", "America/Bogota", "servicios", servicios), null);
    }

    protected void verificarCuenta(String email) throws Exception {
        String token = notificaciones.ultimoPara(email).orElseThrow().token();
        mockMvc.perform(get(V1 + "/auth/verificacion").param("token", token)).andExpect(status().isOk());
    }

    protected ResultActions login(String email, String password) throws Exception {
        return enviar(post(V1 + "/auth/login"), Map.of("email", email, "password", password), null);
    }

    protected String tokenDe(String email) throws Exception {
        return leer(login(email, PASSWORD).andExpect(status().isOk()).andReturn()).get("accessToken").asString();
    }

    /** Registra, verifica e inicia sesión con un cliente nuevo. */
    protected String clienteConSesion() throws Exception {
        String email = emailUnico("cliente");
        registrarCliente(email).andExpect(status().isCreated());
        verificarCuenta(email);
        return tokenDe(email);
    }

    /**
     * Proveedor verificado con sesión. A cada nombre de servicio se le agrega un sufijo único; los ids se
     * devuelven en el mismo orden recibido.
     */
    protected ProveedorDePrueba proveedorConServicios(List<Map<String, Object>> servicios) throws Exception {
        String email = emailUnico("proveedor");
        String sufijo = " " + UUID.randomUUID().toString().substring(0, 8);
        List<Map<String, Object>> conSufijo = servicios.stream()
                .map(servicio -> {
                    Map<String, Object> copia = new HashMap<>(servicio);
                    copia.put("nombre", servicio.get("nombre") + sufijo);
                    return copia;
                })
                .toList();
        registrarProveedor(email, conSufijo).andExpect(status().isCreated());
        verificarCuenta(email);
        String token = tokenDe(email);
        JsonNode lista = leer(mockMvc.perform(get(V1 + "/servicios")).andReturn());
        List<Integer> ids = new ArrayList<>();
        int idProveedor = 0;
        for (Map<String, Object> servicio : conSufijo) {
            for (JsonNode nodo : lista) {
                if (servicio.get("nombre").equals(nodo.get("nombre").asString())) {
                    ids.add(nodo.get("id").asInt());
                    idProveedor = nodo.get("idProveedor").asInt();
                }
            }
        }
        return new ProveedorDePrueba(email, token, idProveedor, ids);
    }

    protected void crearHorario(String token, int idServicio, List<Integer> dias, String inicio, String fin)
            throws Exception {
        enviar(post(V1 + "/servicios/" + idServicio + "/horarios"),
                Map.of("diasSemana", dias, "horaInicio", inicio, "horaFin", fin), token)
                .andExpect(status().isCreated());
    }

    protected int crearRecurso(String token, String nombre) throws Exception {
        MvcResult resultado = enviar(post(V1 + "/recursos"),
                Map.of("nombre", nombre, "idTipoRecurso", TIPO_RECURSO_SALA), token)
                .andExpect(status().isCreated()).andReturn();
        return leer(resultado).get("id").asInt();
    }

    protected void asignarRecursos(String token, int idServicio, List<Integer> idsRecursos) throws Exception {
        enviar(put(V1 + "/servicios/" + idServicio + "/recursos"), Map.of("idsRecursos", idsRecursos), token)
                .andExpect(status().isOk());
    }

    protected ResultActions reservar(String token, int idServicio, String fechaHoraInicio) throws Exception {
        return enviar(post(V1 + "/reservas"), Map.of("idServicio", idServicio, "fechaHoraInicio", fechaHoraInicio),
                token);
    }

    public record ProveedorDePrueba(String email, String token, int idProveedor, List<Integer> idsServicios) {

        public int idServicio() {
            return idsServicios.getFirst();
        }
    }
}

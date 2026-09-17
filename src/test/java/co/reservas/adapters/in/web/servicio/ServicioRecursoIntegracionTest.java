package co.reservas.adapters.in.web.servicio;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Servicios y recursos del proveedor")
class ServicioRecursoIntegracionTest extends PruebaIntegracion {

    @Nested
    @DisplayName("Servicios")
    class Servicios {

        @Test
        @DisplayName("Dado un proveedor autenticado, cuando crea un servicio, entonces responde 201 con Location y se puede consultar y filtrar")
        void crearYConsultar() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            Map<String, Object> nuevo = Map.of("nombre", "Clase grupal", "descripcion", "Yoga", "idCategoria", 3,
                    "duracionMinutos", 60, "capacidad", 10);

            // Act
            String ubicacion = enviar(post(V1 + "/servicios"), nuevo, proveedor.token())
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(V1 + "/servicios/")))
                    .andExpect(jsonPath("$.capacidad").value(10))
                    .andReturn().getResponse().getHeader("Location");

            // Assert
            mockMvc.perform(get(ubicacion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Clase grupal"))
                    .andExpect(jsonPath("$.idProveedor").value(proveedor.idProveedor()));
            mockMvc.perform(get(V1 + "/servicios").param("idProveedor", String.valueOf(proveedor.idProveedor()))
                            .param("idCategoria", "3"))
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Dado un nombre repetido, una categoría inexistente o un servicio inexistente, cuando opera, entonces recibe 409, 404 y 404")
        void errores() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            Map<String, Object> repetido = Map.of("nombre", "Masaje", "idCategoria", 2, "duracionMinutos", 30);
            enviar(post(V1 + "/servicios"), repetido, proveedor.token()).andExpect(status().isCreated());

            // Act - Assert
            enviar(post(V1 + "/servicios"), repetido, proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_YA_REGISTRADO"));
            enviar(post(V1 + "/servicios"), Map.of("nombre", "X", "idCategoria", 999, "duracionMinutos", 30),
                    proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("CATEGORIA_NO_ENCONTRADA"));
            mockMvc.perform(get(V1 + "/servicios/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("SERVICIO_NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("Dado una ruta inexistente o un método no soportado, cuando se invoca, entonces recibe 404 RUTA_NO_ENCONTRADA o 405 METODO_NO_PERMITIDO")
        void rutasYMetodos() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));

            // Act - Assert
            enviar(get(V1 + "/no-existe"), null, proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("RUTA_NO_ENCONTRADA"));
            enviar(patch(V1 + "/servicios/" + proveedor.idServicio()), null, proveedor.token())
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.errorCode").value("METODO_NO_PERMITIDO"));
        }
    }

    @Nested
    @DisplayName("HU-09 - Gestión de recursos")
    class Recursos {

        @Test
        @DisplayName("Dado un proveedor, cuando crea recursos y los asigna a su servicio, entonces se listan y la asignación responde los recursos")
        void crearListarYAsignar() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            int sala = crearRecurso(proveedor.token(), "Sala 1");
            int equipo = crearRecurso(proveedor.token(), "Equipo rayos X");

            // Act - Assert
            enviar(get(V1 + "/recursos"), null, proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].activo").value(true));
            enviar(put(V1 + "/servicios/" + proveedor.idServicio() + "/recursos"),
                    Map.of("idsRecursos", List.of(equipo, sala)), proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(sala))
                    .andExpect(jsonPath("$[1].id").value(equipo));
            enviar(put(V1 + "/servicios/" + proveedor.idServicio() + "/recursos"), Map.of("idsRecursos", List.of()),
                    proveedor.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Dado un nombre repetido o un tipo inexistente, cuando crea un recurso, entonces recibe 409 RECURSO_YA_REGISTRADO o 404 TIPO_RECURSO_NO_ENCONTRADO")
        void erroresAlCrear() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            crearRecurso(proveedor.token(), "Sala 1");

            // Act - Assert
            enviar(post(V1 + "/recursos"), Map.of("nombre", "Sala 1", "idTipoRecurso", 1), proveedor.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("RECURSO_YA_REGISTRADO"));
            enviar(post(V1 + "/recursos"), Map.of("nombre", "Sala 2", "idTipoRecurso", 99), proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TIPO_RECURSO_NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("Dado un recurso ajeno o inexistente, cuando lo asigna a su servicio, entonces recibe 403 ACCESO_DENEGADO o 404 RECURSO_NO_ENCONTRADO")
        void soloRecursosPropios() throws Exception {
            // Arrange
            ProveedorDePrueba proveedor = proveedorConServicios(List.of(servicio("Consulta", 30, 1)));
            ProveedorDePrueba otro = proveedorConServicios(List.of(servicio("Otro", 30, 1)));
            int recursoAjeno = crearRecurso(otro.token(), "Sala ajena");
            String ruta = V1 + "/servicios/" + proveedor.idServicio() + "/recursos";

            // Act - Assert
            enviar(put(ruta), Map.of("idsRecursos", List.of(recursoAjeno)), proveedor.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
            enviar(put(ruta), Map.of("idsRecursos", List.of(999999)), proveedor.token())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("RECURSO_NO_ENCONTRADO"));
            enviar(put(V1 + "/servicios/" + otro.idServicio() + "/recursos"),
                    Map.of("idsRecursos", List.of(recursoAjeno)), proveedor.token())
                    .andExpect(status().isForbidden());
        }
    }
}

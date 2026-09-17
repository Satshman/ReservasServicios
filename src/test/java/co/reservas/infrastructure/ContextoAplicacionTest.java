package co.reservas.infrastructure;

import co.reservas.soporte.PruebaIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContextoAplicacionTest extends PruebaIntegracion {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Dado el esquema migrado, cuando arranca la aplicación, entonces Hibernate lo valida y existen los datos semilla")
    void arrancaConEsquemaMigradoYDatosSemilla() {
        // Arrange - Act
        Integer estados = jdbc.queryForObject("select count(*) from tbl_estados", Integer.class);
        Integer categorias = jdbc.queryForObject("select count(*) from tbl_categorias_servicio", Integer.class);

        // Assert
        assertThat(estados).isEqualTo(8);
        assertThat(categorias).isEqualTo(6);
    }

    @Test
    @DisplayName("Dado un cliente sin token, cuando consulta un endpoint protegido, entonces recibe 401 con el cuerpo uniforme y traceId")
    void endpointProtegidoSinTokenResponde401Uniforme() throws Exception {
        // Arrange - Act - Assert
        mockMvc.perform(get(V1 + "/reservas/mias").header("X-Trace-Id", "prueba-traza-0001"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "prueba-traza-0001"))
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.traceId").value("prueba-traza-0001"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.timestamp").value("2026-10-05T13:00:00Z"));
    }

    @Test
    @DisplayName("Dado el contrato OpenAPI, cuando se consulta la documentación, entonces está disponible sin autenticación")
    void documentacionOpenApiDisponible() throws Exception {
        // Arrange - Act - Assert
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dado los catálogos semilla, cuando se consultan, entonces se listan públicamente")
    void catalogosPublicos() throws Exception {
        // Arrange - Act - Assert
        mockMvc.perform(get(V1 + "/catalogos/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("SALUD"));
        mockMvc.perform(get(V1 + "/catalogos/tipos-recurso")).andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get(V1 + "/catalogos/tipos-documento")).andExpect(jsonPath("$.length()").value(5));
    }
}

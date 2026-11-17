package co.reservas.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginacionTest {

    @Test
    @DisplayName("Dada una lista de elementos, cuando se aplica paginación con página y tamaño válidos, entonces devuelve la sublista correspondiente")
    void recortaSublistaSegunPaginaYTamano() {
        // Arrange
        List<Integer> datos = List.of(1, 2, 3, 4, 5);

        // Act
        List<Integer> pagina0 = Paginacion.aplicar(datos, 0, 2);
        List<Integer> pagina2 = Paginacion.aplicar(datos, 2, 2);

        // Assert
        assertEquals(List.of(1, 2), pagina0);
        assertEquals(List.of(5), pagina2);
    }

    @Test
    @DisplayName("Dada una solicitud de paginación con página negativa o tamaño fuera de rango, cuando se aplica, entonces rechaza con excepción de validación")
    void rechazaParametrosInvalidos() {
        List<Integer> datos = List.of(1, 2, 3);
        assertThrows(ExcepcionNegocio.class, () -> Paginacion.aplicar(datos, -1, 10));
        assertThrows(ExcepcionNegocio.class, () -> Paginacion.aplicar(datos, 0, 0));
        assertThrows(ExcepcionNegocio.class, () -> Paginacion.aplicar(datos, 0, 101));
    }
}
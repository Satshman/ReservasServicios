package co.reservas.domain.reserva;

import co.reservas.domain.usuario.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaCancelacionTest {

    @Test
    @DisplayName("Dado una anticipación mínima de 5 días, cuando se consulta por rol, entonces solo el cliente tiene ventana")
    void ventanaSoloParaCliente() {
        // Arrange
        PoliticaCancelacion politica = new PoliticaCancelacion(Duration.ofDays(5));

        // Act - Assert
        assertThat(politica.anticipacionPara(Rol.CLIENTE)).isEqualTo(Duration.ofDays(5));
        assertThat(politica.anticipacionPara(Rol.PROVEEDOR)).isEqualTo(Duration.ZERO);
        assertThat(politica.anticipacionPara(Rol.ADMIN)).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("Dado una anticipación nula o negativa, cuando se construye la política, entonces se rechaza; cero se permite")
    void anticipacionInvalida() {
        // Arrange - Act - Assert
        assertThatThrownBy(() -> new PoliticaCancelacion(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PoliticaCancelacion(Duration.ofDays(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new PoliticaCancelacion(Duration.ZERO).anticipacionPara(Rol.CLIENTE)).isEqualTo(Duration.ZERO);
    }
}

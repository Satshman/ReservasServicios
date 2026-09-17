package co.reservas.domain.usuario;

/**
 * Roles del sistema. Los nombres coinciden con las filas semilla de {@code tbl_roles}.
 */
public enum Rol {
    CLIENTE("/panel/cliente"),
    PROVEEDOR("/panel/proveedor"),
    ADMIN("/panel/admin");

    private final String rutaPanel;

    Rol(String rutaPanel) {
        this.rutaPanel = rutaPanel;
    }

    public String rutaPanel() {
        return rutaPanel;
    }
}

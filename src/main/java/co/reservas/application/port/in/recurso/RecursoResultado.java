package co.reservas.application.port.in.recurso;

import co.reservas.domain.recurso.Recurso;

public record RecursoResultado(Integer id, Integer idTipoRecurso, String nombre, boolean activo) {

    public static RecursoResultado de(Recurso recurso) {
        return new RecursoResultado(recurso.id(), recurso.idTipoRecurso(), recurso.nombre(), recurso.activo());
    }
}

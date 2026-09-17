package co.reservas.application.port.in.recurso;

import co.reservas.application.port.in.auth.UsuarioAutenticado;

import java.util.List;
import java.util.Set;

public interface GestionarRecursosUseCase {

    RecursoResultado crear(UsuarioAutenticado usuario, CrearRecursoComando comando);

    List<RecursoResultado> listar(UsuarioAutenticado usuario);

    List<RecursoResultado> asignarAServicio(UsuarioAutenticado usuario, Integer idServicio, Set<Integer> idsRecursos);
}

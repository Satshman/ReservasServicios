package co.reservas.application.port.out.auth;

import co.reservas.domain.usuario.Usuario;

public interface TokenEmisorPort {

    TokenAcceso emitir(Usuario usuario);

    record TokenAcceso(String valor, long expiraEnSegundos) {
    }
}

package co.reservas.adapters.in.web.error;

import co.reservas.domain.shared.CodigoError;
import org.springframework.http.HttpStatus;

/**
 * Estado HTTP de cada código de error del contrato.
 */
public final class CodigosHttp {

    private CodigosHttp() {
    }

    public static HttpStatus estado(CodigoError codigo) {
        return switch (codigo) {
            case VALIDACION_FALLIDA, SOLICITUD_INVALIDA, SERVICIO_REQUERIDO, TOKEN_VERIFICACION_INVALIDO ->
                    HttpStatus.BAD_REQUEST;
            case NO_AUTENTICADO, CREDENCIALES_INVALIDAS -> HttpStatus.UNAUTHORIZED;
            case ACCESO_DENEGADO, CUENTA_NO_VERIFICADA, CUENTA_INACTIVA -> HttpStatus.FORBIDDEN;
            case SERVICIO_NO_ENCONTRADO, HORARIO_NO_ENCONTRADO, RECURSO_NO_ENCONTRADO, CATEGORIA_NO_ENCONTRADA,
                 TIPO_RECURSO_NO_ENCONTRADO, RUTA_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case METODO_NO_PERMITIDO -> HttpStatus.METHOD_NOT_ALLOWED;
            case EMAIL_YA_REGISTRADO, SERVICIO_YA_REGISTRADO, RECURSO_YA_REGISTRADO, HORARIO_SOLAPADO,
                 HORARIO_CON_RESERVAS, TURNO_SIN_CUPO, RECURSO_NO_DISPONIBLE, RESERVA_SOLAPADA -> HttpStatus.CONFLICT;
            case RESERVA_EN_EL_PASADO, HORARIO_NO_DISPONIBLE, SERVICIO_NO_DISPONIBLE, BLOQUE_MENOR_A_DURACION ->
                    HttpStatus.UNPROCESSABLE_CONTENT;
            case CUENTA_BLOQUEADA -> HttpStatus.LOCKED;
            case ERROR_INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

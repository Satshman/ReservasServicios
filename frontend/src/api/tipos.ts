/**
 * Tipos del contrato de la API de ReservaYa.
 *
 * Las formas están tomadas de respuestas reales del backend desplegado
 * (https://reservaya-vvke.onrender.com/api/v1) y de los DTO de
 * `src/main/java/co/reservas/adapters/in/web/**`, no de suposiciones.
 */

export type Rol = 'CLIENTE' | 'PROVEEDOR' | 'ADMIN';

export type EstadoUsuario = 'PENDIENTE_VERIFICACION' | 'ACTIVO' | 'INACTIVO';

export type EstadoServicio = 'ACTIVO' | 'INACTIVO';

export type EstadoReserva = 'CONFIRMADA' | 'CANCELADA' | 'COMPLETADA';

/** Códigos de error del contrato uniforme (docs/modelo-datos-y-paquetes.md §3). */
export type CodigoError =
  | 'VALIDACION_FALLIDA'
  | 'SOLICITUD_INVALIDA'
  | 'SERVICIO_REQUERIDO'
  | 'TOKEN_VERIFICACION_INVALIDO'
  | 'NO_AUTENTICADO'
  | 'CREDENCIALES_INVALIDAS'
  | 'ACCESO_DENEGADO'
  | 'CUENTA_NO_VERIFICADA'
  | 'CUENTA_INACTIVA'
  | 'SERVICIO_NO_ENCONTRADO'
  | 'HORARIO_NO_ENCONTRADO'
  | 'RECURSO_NO_ENCONTRADO'
  | 'CATEGORIA_NO_ENCONTRADA'
  | 'TIPO_RECURSO_NO_ENCONTRADO'
  | 'RUTA_NO_ENCONTRADA'
  | 'METODO_NO_PERMITIDO'
  | 'EMAIL_YA_REGISTRADO'
  | 'SERVICIO_YA_REGISTRADO'
  | 'RECURSO_YA_REGISTRADO'
  | 'HORARIO_SOLAPADO'
  | 'HORARIO_CON_RESERVAS'
  | 'TURNO_SIN_CUPO'
  | 'RECURSO_NO_DISPONIBLE'
  | 'RESERVA_SOLAPADA'
  | 'RESERVA_EN_EL_PASADO'
  | 'HORARIO_NO_DISPONIBLE'
  | 'SERVICIO_NO_DISPONIBLE'
  | 'BLOQUE_MENOR_A_DURACION'
  | 'CUENTA_BLOQUEADA'
  | 'ERROR_INTERNO';

/**
 * `details` es heterogéneo: el backend lo declara como `List<Object>` y según el
 * código de error trae errores de campo, turnos sugeridos, recursos en conflicto
 * o reservas afectadas.
 */
export interface DetalleCampo {
  campo: string;
  mensaje: string;
}

export interface SugerenciaTurno {
  tipo: 'SUGERENCIA';
  fechaHoraInicio: string;
  fechaHoraFin: string;
}

export interface RecursoEnConflicto {
  tipo: 'RECURSO';
  idRecurso: number;
  nombre: string;
}

export interface ReservaAfectada {
  idReserva: number;
  fechaHoraInicio: string;
  fechaHoraFin: string;
}

export type DetalleError =
  | DetalleCampo
  | SugerenciaTurno
  | RecursoEnConflicto
  | ReservaAfectada
  | Record<string, unknown>;

/** Cuerpo uniforme de error: `{errorCode, message, details, traceId, timestamp}`. */
export interface RespuestaError {
  errorCode: CodigoError;
  message: string;
  details: DetalleError[];
  traceId: string | null;
  timestamp: string;
}

export interface ElementoCatalogo {
  id: number;
  nombre: string;
}

export interface Servicio {
  id: number;
  idProveedor: number;
  nombreProveedor: string;
  zonaHoraria: string;
  idCategoria: number;
  nombre: string;
  descripcion: string | null;
  duracionMinutos: number;
  capacidad: number;
  estado: EstadoServicio;
}

export interface TurnoDisponible {
  fechaHoraInicio: string;
  fechaHoraFin: string;
  cuposDisponibles: number;
}

export interface Disponibilidad {
  idServicio: number;
  zonaHoraria: string;
  turnos: TurnoDisponible[];
}

/** `horaInicio` y `horaFin` llegan como `HH:mm:ss`. */
export interface Horario {
  id: number;
  idServicio: number;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
}

export interface CambioHorario {
  horario: Horario | null;
  reservasAfectadas: ReservaAfectada[];
}

export interface EliminacionHorario {
  reservasAfectadas: ReservaAfectada[];
}

export interface Recurso {
  id: number;
  idTipoRecurso: number;
  nombre: string;
  activo: boolean;
}

export interface Reserva {
  id: number;
  idServicio: number;
  nombreServicio: string;
  idCliente: number;
  estado: EstadoReserva;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  recursos: Recurso[];
  creadoEn: string;
}

export interface Sesion {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  rol: Rol;
  redirectTo: string;
}

export interface RegistroResultado {
  id: number;
  email: string;
  rol: Rol;
  estado: EstadoUsuario;
  mensaje: string;
}

export interface Mensaje {
  mensaje: string;
}

export interface NuevoServicio {
  nombre: string;
  descripcion?: string;
  idCategoria: number;
  duracionMinutos: number;
  capacidad: number;
}

export interface DatosRegistro {
  nombreCompleto: string;
  email: string;
  password: string;
  rol: 'CLIENTE' | 'PROVEEDOR';
  telefono?: string;
  nombreComercial?: string;
  zonaHoraria?: string;
  servicios?: NuevoServicio[];
}

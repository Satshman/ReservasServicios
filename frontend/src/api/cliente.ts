import type {
  CodigoError,
  DetalleCampo,
  DetalleError,
  RecursoEnConflicto,
  ReservaAfectada,
  RespuestaError,
  SugerenciaTurno,
} from './tipos';

const URL_API_POR_DEFECTO = 'https://reservaya-vvke.onrender.com/api/v1';

// `import.meta.env` solo existe cuando el código lo sirve o empaqueta Vite; la
// comprobación permite además ejecutar estos módulos en Node para verificarlos.
const urlConfigurada =
  typeof import.meta.env === 'undefined' ? undefined : import.meta.env.VITE_API_URL;

export const URL_API: string = urlConfigurada ?? URL_API_POR_DEFECTO;

const MENSAJE_RED =
  'No se pudo contactar el servidor. Revisa tu conexión e inténtalo de nuevo.';

/** Error de negocio o de transporte, siempre con un mensaje presentable al usuario. */
export class ErrorApi extends Error {
  readonly status: number;
  readonly errorCode: CodigoError | 'SIN_CONEXION';
  readonly details: DetalleError[];
  readonly traceId: string | null;

  constructor(
    status: number,
    errorCode: CodigoError | 'SIN_CONEXION',
    message: string,
    details: DetalleError[] = [],
    traceId: string | null = null,
  ) {
    super(message);
    this.name = 'ErrorApi';
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
    this.traceId = traceId;
  }

  /** `true` cuando la sesión falta, expiró o fue rechazada. */
  get esSesionInvalida(): boolean {
    return this.status === 401 && this.errorCode !== 'CREDENCIALES_INVALIDAS';
  }
}

export function esErrorApi(error: unknown): error is ErrorApi {
  return error instanceof ErrorApi;
}

/** Mensaje presentable para cualquier excepción capturada en la interfaz. */
export function mensajeDeError(error: unknown): string {
  if (esErrorApi(error)) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return 'Ocurrió un error inesperado.';
}

export function traceIdDeError(error: unknown): string | null {
  return esErrorApi(error) ? error.traceId : null;
}

export function detallesDeCampo(error: unknown): DetalleCampo[] {
  if (!esErrorApi(error)) return [];
  return error.details.filter(
    (d): d is DetalleCampo =>
      typeof (d as DetalleCampo).campo === 'string' &&
      typeof (d as DetalleCampo).mensaje === 'string',
  );
}

export function sugerenciasDeError(error: unknown): SugerenciaTurno[] {
  if (!esErrorApi(error)) return [];
  return error.details.filter(
    (d): d is SugerenciaTurno => (d as SugerenciaTurno).tipo === 'SUGERENCIA',
  );
}

export function recursosEnConflicto(error: unknown): RecursoEnConflicto[] {
  if (!esErrorApi(error)) return [];
  return error.details.filter(
    (d): d is RecursoEnConflicto => (d as RecursoEnConflicto).tipo === 'RECURSO',
  );
}

export function reservasAfectadasDeError(error: unknown): ReservaAfectada[] {
  if (!esErrorApi(error)) return [];
  return error.details.filter(
    (d): d is ReservaAfectada => typeof (d as ReservaAfectada).idReserva === 'number',
  );
}

export interface OpcionesPeticion {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  cuerpo?: unknown;
  token?: string | null;
  /** Aborta la petición cuando el componente se desmonta. */
  signal?: AbortSignal;
}

function esRespuestaError(valor: unknown): valor is RespuestaError {
  return (
    typeof valor === 'object' &&
    valor !== null &&
    typeof (valor as RespuestaError).errorCode === 'string' &&
    typeof (valor as RespuestaError).message === 'string'
  );
}

/**
 * Punto único de acceso a la API. Normaliza todo fallo —de red o de negocio—
 * a un `ErrorApi` con `message`, `details` y `traceId`.
 */
export async function peticion<T>(ruta: string, opciones: OpcionesPeticion = {}): Promise<T> {
  const { metodo = 'GET', cuerpo, token, signal } = opciones;
  const cabeceras: Record<string, string> = { Accept: 'application/json' };
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (token) cabeceras.Authorization = `Bearer ${token}`;

  let respuesta: Response;
  try {
    respuesta = await fetch(`${URL_API}${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      signal: signal ?? null,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ErrorApi(0, 'SIN_CONEXION', MENSAJE_RED);
  }

  if (respuesta.status === 204) return undefined as T;

  const texto = await respuesta.text();
  let datos: unknown = null;
  if (texto) {
    try {
      datos = JSON.parse(texto) as unknown;
    } catch {
      datos = null;
    }
  }

  if (!respuesta.ok) {
    if (esRespuestaError(datos)) {
      throw new ErrorApi(
        respuesta.status,
        datos.errorCode,
        datos.message,
        Array.isArray(datos.details) ? datos.details : [],
        datos.traceId ?? null,
      );
    }
    throw new ErrorApi(
      respuesta.status,
      'ERROR_INTERNO',
      `El servidor respondió con un error (HTTP ${respuesta.status}).`,
    );
  }

  return datos as T;
}

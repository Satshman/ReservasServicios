import type { Rol, Sesion } from '../api/tipos';

const CLAVE = 'reservaya.sesion';

/** Sesión persistida. `expiraEn` es un epoch en milisegundos. */
export interface SesionGuardada {
  accessToken: string;
  rol: Rol;
  email: string;
  idUsuario: number | null;
  expiraEn: number;
  /** Id de proveedor descubierto; la API no expone `GET /proveedores/me`. */
  idProveedor?: number;
}

/** Lee el `sub` del JWT sin validar la firma: sirve solo para mostrar contexto. */
function idDesdeToken(token: string): number | null {
  try {
    const carga = token.split('.')[1];
    if (!carga) return null;
    const normalizado = carga.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(
      decodeURIComponent(
        atob(normalizado)
          .split('')
          .map((c) => `%${`00${c.charCodeAt(0).toString(16)}`.slice(-2)}`)
          .join(''),
      ),
    ) as { sub?: string };
    const id = Number(json.sub);
    return Number.isFinite(id) ? id : null;
  } catch {
    return null;
  }
}

export function construirSesion(email: string, respuesta: Sesion): SesionGuardada {
  return {
    accessToken: respuesta.accessToken,
    rol: respuesta.rol,
    email,
    idUsuario: idDesdeToken(respuesta.accessToken),
    expiraEn: Date.now() + respuesta.expiresIn * 1000,
  };
}

export function sesionVigente(sesion: SesionGuardada | null): boolean {
  return sesion !== null && sesion.expiraEn > Date.now();
}

export function leerSesion(): SesionGuardada | null {
  try {
    const crudo = window.localStorage.getItem(CLAVE);
    if (!crudo) return null;
    const sesion = JSON.parse(crudo) as SesionGuardada;
    if (typeof sesion?.accessToken !== 'string' || typeof sesion?.expiraEn !== 'number') {
      return null;
    }
    return sesionVigente(sesion) ? sesion : null;
  } catch {
    // Modo privado, almacenamiento bloqueado o contenido corrupto.
    return null;
  }
}

export function guardarSesion(sesion: SesionGuardada): void {
  try {
    window.localStorage.setItem(CLAVE, JSON.stringify(sesion));
  } catch {
    // La sesión sigue viva en memoria aunque no se pueda persistir.
  }
}

export function borrarSesion(): void {
  try {
    window.localStorage.removeItem(CLAVE);
  } catch {
    // Sin acción: no hay nada que limpiar si el almacenamiento no está disponible.
  }
}

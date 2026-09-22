import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { Sesion } from '../api/tipos';
import {
  borrarSesion,
  construirSesion,
  guardarSesion,
  leerSesion,
  sesionVigente,
  type SesionGuardada,
} from './almacenamiento';

export interface ContextoSesion {
  sesion: SesionGuardada | null;
  /** Token vigente, o `null` si la sesión falta o expiró. */
  token: string | null;
  abrirSesion: (email: string, respuesta: Sesion) => SesionGuardada;
  cerrarSesion: () => void;
  recordarProveedor: (idProveedor: number) => void;
}

export const SesionContext = createContext<ContextoSesion | null>(null);

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<SesionGuardada | null>(() => leerSesion());

  // Cierra la sesión sola cuando el token caduca mientras la pestaña está abierta.
  useEffect(() => {
    if (!sesion) return;
    const restante = sesion.expiraEn - Date.now();
    if (restante <= 0) {
      setSesion(null);
      borrarSesion();
      return;
    }
    const temporizador = window.setTimeout(() => {
      setSesion(null);
      borrarSesion();
    }, restante);
    return () => window.clearTimeout(temporizador);
  }, [sesion]);

  const abrirSesion = useCallback((email: string, respuesta: Sesion) => {
    const nueva = construirSesion(email, respuesta);
    guardarSesion(nueva);
    setSesion(nueva);
    return nueva;
  }, []);

  const cerrarSesion = useCallback(() => {
    borrarSesion();
    setSesion(null);
  }, []);

  const recordarProveedor = useCallback((idProveedor: number) => {
    setSesion((actual) => {
      if (!actual || actual.idProveedor === idProveedor) return actual;
      const actualizada = { ...actual, idProveedor };
      guardarSesion(actualizada);
      return actualizada;
    });
  }, []);

  const valor = useMemo<ContextoSesion>(
    () => ({
      sesion,
      token: sesionVigente(sesion) ? (sesion as SesionGuardada).accessToken : null,
      abrirSesion,
      cerrarSesion,
      recordarProveedor,
    }),
    [sesion, abrirSesion, cerrarSesion, recordarProveedor],
  );

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

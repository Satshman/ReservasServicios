import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { esErrorApi } from '../api/cliente';
import { useSesion } from './useSesion';

/**
 * Devuelve una función que intercepta los 401 de las llamadas protegidas:
 * cierra la sesión y envía a `/ingresar` con un mensaje claro y el destino
 * al que volver. Responde `true` cuando ya gestionó el error.
 */
export function useSesionExpirada(): (error: unknown) => boolean {
  const { cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();

  return useCallback(
    (error: unknown) => {
      if (!esErrorApi(error) || !error.esSesionInvalida) return false;
      cerrarSesion();
      navegar('/ingresar', {
        replace: true,
        state: {
          destino: `${ubicacion.pathname}${ubicacion.search}`,
          aviso: 'Tu sesión expiró. Ingresa de nuevo para continuar.',
        },
      });
      return true;
    },
    [cerrarSesion, navegar, ubicacion.pathname, ubicacion.search],
  );
}

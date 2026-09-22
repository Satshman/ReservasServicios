import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import type { Rol } from '../api/tipos';
import { useSesion } from '../sesion/useSesion';

interface Props {
  rol: Rol;
  children: ReactNode;
}

/**
 * Exige sesión vigente y el rol correcto. Sin sesión envía a `/ingresar`
 * conservando el destino, para volver tras autenticarse.
 */
export function RutaProtegida({ rol, children }: Props) {
  const { sesion, token } = useSesion();
  const ubicacion = useLocation();

  if (!sesion || !token) {
    return (
      <Navigate
        to="/ingresar"
        replace
        state={{
          destino: `${ubicacion.pathname}${ubicacion.search}`,
          aviso: 'Tu sesión no está activa o expiró. Ingresa de nuevo para continuar.',
        }}
      />
    );
  }

  if (sesion.rol !== rol) {
    const propio = sesion.rol === 'PROVEEDOR' ? '/panel/proveedor' : '/panel/cliente';
    return <Navigate to={propio} replace />;
  }

  return <>{children}</>;
}

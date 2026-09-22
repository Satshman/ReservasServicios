import { useContext } from 'react';
import { SesionContext, type ContextoSesion } from './SesionContext';

export function useSesion(): ContextoSesion {
  const contexto = useContext(SesionContext);
  if (!contexto) {
    throw new Error('useSesion debe usarse dentro de <ProveedorSesion>.');
  }
  return contexto;
}

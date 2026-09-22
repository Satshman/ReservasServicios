import { listarReservasDeServicio } from './api';
import { esErrorApi } from './cliente';
import type { Servicio } from './tipos';

/**
 * Descubre el `idProveedor` del usuario autenticado.
 *
 * La API todavía no expone `GET /proveedores/me` ni incluye el id de proveedor
 * en el token (solo lleva `sub` y `rol`), así que se prueba un servicio por
 * cada proveedor distinto del catálogo contra un endpoint de solo lectura
 * restringido al dueño: `GET /servicios/{id}/reservas` responde 200 al dueño y
 * 403 ACCESO_DENEGADO a cualquier otro. El resultado se memoriza en la sesión.
 */
export async function descubrirIdProveedor(
  token: string,
  servicios: Servicio[],
  signal?: AbortSignal,
): Promise<number | null> {
  const vistos = new Set<number>();
  for (const servicio of servicios) {
    if (vistos.has(servicio.idProveedor)) continue;
    vistos.add(servicio.idProveedor);
    try {
      await listarReservasDeServicio(token, servicio.id, { signal });
      return servicio.idProveedor;
    } catch (error: unknown) {
      if (esErrorApi(error) && error.status === 403) continue;
      throw error;
    }
  }
  return null;
}

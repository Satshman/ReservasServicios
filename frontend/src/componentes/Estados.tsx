import type { ReactNode } from 'react';
import { mensajeDeError, traceIdDeError } from '../api/cliente';

/** Estado de carga: se anuncia a lectores de pantalla con `role="status"`. */
export function Cargando({ mensaje = 'Cargando…' }: { mensaje?: string }) {
  return (
    <p className="estado estado--cargando" role="status" aria-live="polite">
      <span className="estado__indicador" aria-hidden="true" />
      {mensaje}
    </p>
  );
}

/** Estado vacío: explica por qué no hay datos y ofrece la acción que sí existe. */
export function Vacio({ titulo, detalle, accion }: { titulo: string; detalle?: string; accion?: ReactNode }) {
  return (
    <div className="estado estado--vacio">
      <p className="estado__titulo">{titulo}</p>
      {detalle ? <p className="estado__detalle">{detalle}</p> : null}
      {accion ? <div className="estado__accion">{accion}</div> : null}
    </div>
  );
}

interface PropsErrorCaja {
  error: unknown;
  /** Título opcional; por defecto el mensaje del error es el contenido principal. */
  titulo?: string;
  onReintentar?: () => void;
  children?: ReactNode;
}

/**
 * Estado de error. El mensaje es el del contrato (`message`) y el `traceId`
 * queda visible de forma discreta para poder rastrear el fallo en los logs.
 */
export function CajaError({ error, titulo, onReintentar, children }: PropsErrorCaja) {
  if (!error) return null;
  const traceId = traceIdDeError(error);
  return (
    <div className="estado estado--error" role="alert">
      {titulo ? <p className="estado__titulo">{titulo}</p> : null}
      <p className="estado__detalle">{mensajeDeError(error)}</p>
      {children}
      {onReintentar ? (
        <div className="estado__accion">
          <button type="button" className="boton boton--secundario" onClick={onReintentar}>
            Reintentar
          </button>
        </div>
      ) : null}
      {traceId ? <TraceId valor={traceId} /> : null}
    </div>
  );
}

/** Identificador de traza; se muestra siempre, en tamaño reducido. */
export function TraceId({ valor }: { valor: string }) {
  return (
    <p className="trace-id">
      <span className="trace-id__etiqueta">Código de seguimiento</span>
      <code>{valor}</code>
    </p>
  );
}

/**
 * Región de anuncios asíncronos. Se monta siempre para que los lectores de
 * pantalla registren los cambios de contenido.
 */
export function Anuncio({ mensaje, tono = 'info' }: { mensaje: string | null; tono?: 'info' | 'exito' }) {
  return (
    <div className={`anuncio anuncio--${tono}`} aria-live="polite" role="status">
      {mensaje ? <p>{mensaje}</p> : null}
    </div>
  );
}

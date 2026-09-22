import { useEffect, useId, useRef } from 'react';
import type { ReactNode } from 'react';

interface PropsDialogo {
  titulo: string;
  descripcion?: string;
  textoConfirmar: string;
  textoCancelar?: string;
  ocupado?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
  children?: ReactNode;
}

/**
 * Diálogo modal de confirmación. Recibe el foco al abrirse, se cierra con
 * Escape y lo devuelve al elemento que lo disparó.
 */
export function Dialogo({
  titulo,
  descripcion,
  textoConfirmar,
  textoCancelar = 'Cancelar',
  ocupado = false,
  onConfirmar,
  onCancelar,
  children,
}: PropsDialogo) {
  const idTitulo = useId();
  const idDescripcion = useId();
  const panel = useRef<HTMLDivElement>(null);
  const confirmar = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const anterior = document.activeElement as HTMLElement | null;
    confirmar.current?.focus();

    function alPulsar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        evento.stopPropagation();
        onCancelar();
        return;
      }
      if (evento.key !== 'Tab' || !panel.current) return;
      const focusables = panel.current.querySelectorAll<HTMLElement>(
        'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (focusables.length === 0) return;
      const primero = focusables[0];
      const ultimo = focusables[focusables.length - 1];
      if (evento.shiftKey && document.activeElement === primero) {
        evento.preventDefault();
        ultimo.focus();
      } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault();
        primero.focus();
      }
    }

    document.addEventListener('keydown', alPulsar, true);
    return () => {
      document.removeEventListener('keydown', alPulsar, true);
      anterior?.focus?.();
    };
  }, [onCancelar]);

  return (
    <div className="dialogo__fondo" onClick={onCancelar}>
      <div
        ref={panel}
        className="dialogo"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        aria-describedby={descripcion ? idDescripcion : undefined}
        onClick={(evento) => evento.stopPropagation()}
      >
        <h2 className="dialogo__titulo" id={idTitulo}>
          {titulo}
        </h2>
        {descripcion ? (
          <p className="dialogo__descripcion" id={idDescripcion}>
            {descripcion}
          </p>
        ) : null}
        {children}
        <div className="dialogo__acciones">
          <button
            type="button"
            className="boton boton--secundario"
            onClick={onCancelar}
            disabled={ocupado}
          >
            {textoCancelar}
          </button>
          <button
            ref={confirmar}
            type="button"
            className="boton boton--peligro"
            onClick={onConfirmar}
            disabled={ocupado}
          >
            {ocupado ? 'Procesando…' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}

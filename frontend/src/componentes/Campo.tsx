import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';

interface Comun {
  etiqueta: string;
  ayuda?: string;
  error?: string;
}

type PropsCampo = Comun & Omit<InputHTMLAttributes<HTMLInputElement>, 'id'>;

/** Campo de texto con `label` asociado por `htmlFor` y errores enlazados por `aria-describedby`. */
export function Campo({ etiqueta, ayuda, error, ...resto }: PropsCampo) {
  const id = useId();
  const idAyuda = `${id}-ayuda`;
  const idError = `${id}-error`;
  const descrito = [ayuda ? idAyuda : null, error ? idError : null].filter(Boolean).join(' ');
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      <input
        id={id}
        className={`campo__control${error ? ' campo__control--error' : ''}`}
        aria-invalid={error ? true : undefined}
        aria-describedby={descrito || undefined}
        {...resto}
      />
      {ayuda ? (
        <p className="campo__ayuda" id={idAyuda}>
          {ayuda}
        </p>
      ) : null}
      {error ? (
        <p className="campo__error" id={idError}>
          {error}
        </p>
      ) : null}
    </div>
  );
}

type PropsSelector = Comun & Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> & {
  children: ReactNode;
};

export function Selector({ etiqueta, ayuda, error, children, ...resto }: PropsSelector) {
  const id = useId();
  const idAyuda = `${id}-ayuda`;
  const idError = `${id}-error`;
  const descrito = [ayuda ? idAyuda : null, error ? idError : null].filter(Boolean).join(' ');
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      <select
        id={id}
        className={`campo__control${error ? ' campo__control--error' : ''}`}
        aria-invalid={error ? true : undefined}
        aria-describedby={descrito || undefined}
        {...resto}
      >
        {children}
      </select>
      {ayuda ? (
        <p className="campo__ayuda" id={idAyuda}>
          {ayuda}
        </p>
      ) : null}
      {error ? (
        <p className="campo__error" id={idError}>
          {error}
        </p>
      ) : null}
    </div>
  );
}

import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { reenviarVerificacion, verificarCuenta } from '../api/api';
import { mensajeDeError } from '../api/cliente';
import { Campo } from '../componentes/Campo';
import { CajaError, Cargando } from '../componentes/Estados';

type Fase = 'sin-token' | 'verificando' | 'exito' | 'fallo';

export function Verificacion() {
  const [parametros] = useSearchParams();
  const token = parametros.get('token');

  const [fase, setFase] = useState<Fase>(token ? 'verificando' : 'sin-token');
  const [mensaje, setMensaje] = useState<string>('');
  const [error, setError] = useState<unknown>(null);

  const [email, setEmail] = useState('');
  const [reenviando, setReenviando] = useState(false);
  const [avisoReenvio, setAvisoReenvio] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Verificación de cuenta — ReservaYa';
  }, []);

  useEffect(() => {
    if (!token) {
      setFase('sin-token');
      return;
    }
    const control = new AbortController();
    setFase('verificando');
    setError(null);
    verificarCuenta(token, { signal: control.signal })
      .then((respuesta) => {
        setMensaje(respuesta.mensaje);
        setFase('exito');
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setError(fallo);
        setFase('fallo');
      });
    return () => control.abort();
  }, [token]);

  async function solicitarReenvio(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setReenviando(true);
    setAvisoReenvio(null);
    try {
      const respuesta = await reenviarVerificacion(email.trim());
      setAvisoReenvio(respuesta.mensaje);
    } catch (fallo: unknown) {
      setAvisoReenvio(mensajeDeError(fallo));
    } finally {
      setReenviando(false);
    }
  }

  return (
    <div className="pagina pagina--estrecha">
      <h1>Verificación de cuenta</h1>

      <div aria-live="polite" className="region-anuncios">
        {fase === 'verificando' ? <Cargando mensaje="Validando el enlace de verificación…" /> : null}

        {fase === 'exito' ? (
          <div className="estado estado--exito" role="status">
            <p className="estado__titulo">Cuenta verificada</p>
            <p className="estado__detalle">{mensaje}</p>
            <div className="estado__accion">
              <Link className="boton boton--principal" to="/ingresar">
                Ir a iniciar sesión
              </Link>
            </div>
          </div>
        ) : null}

        {fase === 'fallo' ? (
          <CajaError error={error} titulo="No pudimos verificar la cuenta">
            <p className="estado__ayuda">
              El enlace pudo caducar (dura 24 horas) o ya fue usado. Solicita uno nuevo más abajo.
            </p>
          </CajaError>
        ) : null}

        {fase === 'sin-token' ? (
          <p className="estado estado--aviso" role="status">
            Esta página necesita el enlace que enviamos por correo. Si lo perdiste, pide un nuevo
            envío con el formulario siguiente.
          </p>
        ) : null}
      </div>

      <section aria-labelledby="titulo-reenvio">
        <h2 id="titulo-reenvio">Reenviar verificación</h2>
        <p className="texto-apoyo">
          Escribe el correo con el que te registraste. Por seguridad la respuesta es la misma exista
          o no la cuenta.
        </p>
        <form className="formulario" onSubmit={solicitarReenvio} noValidate>
          <Campo
            etiqueta="Correo electrónico"
            type="email"
            name="email"
            autoComplete="email"
            required
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
          />
          <div aria-live="polite" className="region-anuncios">
            {avisoReenvio ? (
              <p className="estado estado--aviso" role="status">
                {avisoReenvio}
              </p>
            ) : null}
          </div>
          <button
            type="submit"
            className="boton boton--principal"
            disabled={reenviando || email.trim() === ''}
          >
            {reenviando ? 'Enviando…' : 'Reenviar verificación'}
          </button>
        </form>
      </section>

      <p className="texto-apoyo">
        <Link to="/ingresar">Volver a iniciar sesión</Link>
      </p>
    </div>
  );
}

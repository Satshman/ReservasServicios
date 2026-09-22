import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { iniciarSesion, reenviarVerificacion } from '../api/api';
import { esErrorApi, mensajeDeError } from '../api/cliente';
import { Campo } from '../componentes/Campo';
import { TraceId } from '../componentes/Estados';
import { useSesion } from '../sesion/useSesion';

interface EstadoNavegacion {
  destino?: string;
  turnoPendiente?: string;
  aviso?: string;
}

/** Texto y acción de recuperación propios de cada desenlace documentado. */
interface Desenlace {
  mensaje: string;
  ayuda?: string;
  ofrecerReenvio?: boolean;
  traceId?: string | null;
}

function interpretar(error: unknown): Desenlace {
  if (!esErrorApi(error)) {
    return { mensaje: mensajeDeError(error) };
  }
  const traceId = error.traceId;
  switch (error.errorCode) {
    case 'CREDENCIALES_INVALIDAS':
      return {
        mensaje: error.message,
        ayuda:
          'Revisa el correo y la contraseña. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.',
        traceId,
      };
    case 'CUENTA_NO_VERIFICADA':
      return {
        mensaje: error.message,
        ayuda: 'Abre el enlace que enviamos a tu correo o solicita un nuevo envío.',
        ofrecerReenvio: true,
        traceId,
      };
    case 'CUENTA_INACTIVA':
      return {
        mensaje: error.message,
        ayuda: 'La cuenta fue desactivada. Contacta al administrador de la plataforma.',
        traceId,
      };
    case 'CUENTA_BLOQUEADA':
      return {
        mensaje: error.message,
        ayuda: 'El bloqueo por intentos fallidos dura 15 minutos. Vuelve a intentarlo después.',
        traceId,
      };
    default:
      return { mensaje: error.message, traceId };
  }
}

export function Ingresar() {
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const { abrirSesion, sesion } = useSesion();
  const estado = (ubicacion.state as EstadoNavegacion | null) ?? null;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [desenlace, setDesenlace] = useState<Desenlace | null>(null);
  const [avisoReenvio, setAvisoReenvio] = useState<string | null>(null);
  const [reenviando, setReenviando] = useState(false);
  /** Marca el envío manual para que el redirector automático no lo pise. */
  const destinoTomado = useRef(false);

  useEffect(() => {
    document.title = 'Ingresar — ReservaYa';
  }, []);

  // Con sesión vigente ya no tiene sentido mostrar el formulario, salvo que el
  // propio envío esté llevando al usuario a su destino.
  useEffect(() => {
    if (!sesion || destinoTomado.current) return;
    navegar(sesion.rol === 'PROVEEDOR' ? '/panel/proveedor' : '/panel/cliente', { replace: true });
  }, [sesion, navegar]);

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setEnviando(true);
    setDesenlace(null);
    setAvisoReenvio(null);
    try {
      const respuesta = await iniciarSesion(email.trim(), password);
      destinoTomado.current = true;
      abrirSesion(email.trim(), respuesta);
      // El destino lo decide la API con `redirectTo`, salvo que hubiera un
      // turno pendiente al que volver.
      if (estado?.destino) {
        navegar(estado.destino, {
          replace: true,
          state: estado.turnoPendiente ? { turnoPendiente: estado.turnoPendiente } : null,
        });
      } else {
        navegar(respuesta.redirectTo, { replace: true });
      }
    } catch (error: unknown) {
      setDesenlace(interpretar(error));
    } finally {
      setEnviando(false);
    }
  }

  async function solicitarReenvio() {
    setReenviando(true);
    setAvisoReenvio(null);
    try {
      const respuesta = await reenviarVerificacion(email.trim());
      setAvisoReenvio(respuesta.mensaje);
    } catch (error: unknown) {
      setAvisoReenvio(mensajeDeError(error));
    } finally {
      setReenviando(false);
    }
  }

  return (
    <div className="pagina pagina--estrecha">
      <h1>Ingresar</h1>
      <p className="texto-apoyo">
        Accede con la cuenta que registraste. Te llevaremos al panel que corresponde a tu rol.
      </p>

      {estado?.aviso ? (
        <p className="estado estado--aviso" role="status">
          {estado.aviso}
        </p>
      ) : null}

      <form className="formulario" onSubmit={enviar} noValidate>
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          name="email"
          autoComplete="email"
          required
          value={email}
          onChange={(evento) => setEmail(evento.target.value)}
        />
        <Campo
          etiqueta="Contraseña"
          type="password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(evento) => setPassword(evento.target.value)}
        />

        <div aria-live="polite" className="region-anuncios">
          {desenlace ? (
            <div className="estado estado--error" role="alert">
              <p className="estado__detalle">{desenlace.mensaje}</p>
              {desenlace.ayuda ? <p className="estado__ayuda">{desenlace.ayuda}</p> : null}
              {desenlace.ofrecerReenvio ? (
                <div className="estado__accion">
                  <button
                    type="button"
                    className="boton boton--secundario"
                    onClick={() => void solicitarReenvio()}
                    disabled={reenviando || email.trim() === ''}
                  >
                    {reenviando ? 'Enviando…' : 'Reenviar correo de verificación'}
                  </button>
                </div>
              ) : null}
              {desenlace.traceId ? <TraceId valor={desenlace.traceId} /> : null}
            </div>
          ) : null}
          {avisoReenvio ? (
            <p className="estado estado--aviso" role="status">
              {avisoReenvio}
            </p>
          ) : null}
        </div>

        <button type="submit" className="boton boton--principal" disabled={enviando}>
          {enviando ? 'Verificando…' : 'Ingresar'}
        </button>
      </form>

      <p className="texto-apoyo">
        ¿Aún no tienes cuenta? <Link to="/registro">Crear una cuenta</Link>.
      </p>
      <p className="texto-apoyo">
        ¿Tu cuenta sigue sin verificar? <Link to="/verificacion">Ir a verificación</Link>.
      </p>
    </div>
  );
}

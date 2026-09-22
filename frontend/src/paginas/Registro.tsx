import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarCategorias, registrar } from '../api/api';
import { detallesDeCampo, esErrorApi, mensajeDeError, traceIdDeError } from '../api/cliente';
import type { ElementoCatalogo, NuevoServicio, RegistroResultado } from '../api/tipos';
import { Campo, Selector } from '../componentes/Campo';
import { TraceId } from '../componentes/Estados';

type RolElegible = 'CLIENTE' | 'PROVEEDOR';

interface FilaServicio {
  clave: number;
  nombre: string;
  idCategoria: string;
  duracionMinutos: string;
  capacidad: string;
}

const ZONAS = [
  'America/Bogota',
  'America/Mexico_City',
  'America/Lima',
  'America/Santiago',
  'America/Argentina/Buenos_Aires',
  'Europe/Madrid',
] as const;

let contador = 0;
function filaVacia(): FilaServicio {
  contador += 1;
  return {
    clave: contador,
    nombre: '',
    idCategoria: '',
    duracionMinutos: '30',
    capacidad: '1',
  };
}

export function Registro() {
  const [rol, setRol] = useState<RolElegible>('CLIENTE');
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [telefono, setTelefono] = useState('');
  const [nombreComercial, setNombreComercial] = useState('');
  const [zonaHoraria, setZonaHoraria] = useState<string>('America/Bogota');
  const [servicios, setServicios] = useState<FilaServicio[]>([filaVacia()]);

  const [categorias, setCategorias] = useState<ElementoCatalogo[]>([]);
  const [errorCategorias, setErrorCategorias] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [resultado, setResultado] = useState<RegistroResultado | null>(null);

  useEffect(() => {
    document.title = 'Crear cuenta — ReservaYa';
  }, []);

  useEffect(() => {
    const control = new AbortController();
    listarCategorias({ signal: control.signal })
      .then((datos) => {
        setCategorias(datos);
        setErrorCategorias(null);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setErrorCategorias(fallo);
      });
    return () => control.abort();
  }, []);

  function actualizarFila(clave: number, cambios: Partial<FilaServicio>) {
    setServicios((filas) =>
      filas.map((fila) => (fila.clave === clave ? { ...fila, ...cambios } : fila)),
    );
  }

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setEnviando(true);
    setError(null);
    setResultado(null);

    const listaServicios: NuevoServicio[] = servicios
      .filter((fila) => fila.nombre.trim() !== '' || fila.idCategoria !== '')
      .map((fila) => ({
        nombre: fila.nombre.trim(),
        idCategoria: Number(fila.idCategoria),
        duracionMinutos: Number(fila.duracionMinutos),
        capacidad: Number(fila.capacidad),
      }));

    try {
      const respuesta = await registrar({
        nombreCompleto: nombreCompleto.trim(),
        email: email.trim(),
        password,
        rol,
        ...(telefono.trim() ? { telefono: telefono.trim() } : {}),
        ...(rol === 'PROVEEDOR'
          ? {
              ...(nombreComercial.trim() ? { nombreComercial: nombreComercial.trim() } : {}),
              zonaHoraria,
              servicios: listaServicios,
            }
          : {}),
      });
      setResultado(respuesta);
    } catch (fallo: unknown) {
      setError(fallo);
    } finally {
      setEnviando(false);
    }
  }

  const errores = detallesDeCampo(error);
  const errorDe = (campo: string) =>
    errores.find((detalle) => detalle.campo === campo)?.mensaje;
  const traceId = traceIdDeError(error);
  const codigo = esErrorApi(error) ? error.errorCode : null;

  if (resultado) {
    return (
      <div className="pagina pagina--estrecha">
        <h1>Cuenta creada</h1>
        <div className="estado estado--exito" role="status">
          <p className="estado__detalle">{resultado.mensaje}</p>
          <p className="estado__ayuda">
            Cuenta <strong>{resultado.email}</strong> · rol {resultado.rol} · estado{' '}
            {resultado.estado}.
          </p>
        </div>
        <p className="texto-apoyo">
          Revisa tu correo y abre el enlace de verificación. Cuando esté verificada podrás{' '}
          <Link to="/ingresar">ingresar</Link>.
        </p>
        <p className="texto-apoyo">
          ¿No te llegó? <Link to="/verificacion">Solicita un nuevo envío</Link>.
        </p>
      </div>
    );
  }

  return (
    <div className="pagina pagina--estrecha">
      <h1>Crear cuenta</h1>
      <p className="texto-apoyo">
        Elige el tipo de cuenta. Un proveedor publica servicios y gestiona su agenda; un cliente
        reserva turnos.
      </p>

      <form className="formulario" onSubmit={enviar} noValidate>
        <fieldset className="grupo">
          <legend className="grupo__titulo">Tipo de cuenta</legend>
          <div className="opciones">
            <label className="opcion">
              <input
                type="radio"
                name="rol"
                value="CLIENTE"
                checked={rol === 'CLIENTE'}
                onChange={() => setRol('CLIENTE')}
              />
              <span>
                <strong>Cliente</strong>
                <span className="opcion__detalle">Quiero reservar turnos.</span>
              </span>
            </label>
            <label className="opcion">
              <input
                type="radio"
                name="rol"
                value="PROVEEDOR"
                checked={rol === 'PROVEEDOR'}
                onChange={() => setRol('PROVEEDOR')}
              />
              <span>
                <strong>Proveedor</strong>
                <span className="opcion__detalle">Ofrezco servicios y gestiono mi agenda.</span>
              </span>
            </label>
          </div>
        </fieldset>

        <Campo
          etiqueta="Nombre completo"
          name="nombreCompleto"
          autoComplete="name"
          required
          minLength={2}
          maxLength={100}
          value={nombreCompleto}
          onChange={(evento) => setNombreCompleto(evento.target.value)}
          error={errorDe('nombreCompleto')}
        />
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          name="email"
          autoComplete="email"
          required
          value={email}
          onChange={(evento) => setEmail(evento.target.value)}
          error={errorDe('email')}
        />
        <Campo
          etiqueta="Contraseña"
          type="password"
          name="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={64}
          ayuda="Entre 12 y 64 caracteres."
          value={password}
          onChange={(evento) => setPassword(evento.target.value)}
          error={errorDe('password')}
        />
        <Campo
          etiqueta="Teléfono (opcional)"
          type="tel"
          name="telefono"
          autoComplete="tel"
          maxLength={20}
          value={telefono}
          onChange={(evento) => setTelefono(evento.target.value)}
          error={errorDe('telefono')}
        />

        {rol === 'PROVEEDOR' ? (
          <>
            <fieldset className="grupo">
              <legend className="grupo__titulo">Datos del negocio</legend>
              <Campo
                etiqueta="Nombre comercial (opcional)"
                name="nombreComercial"
                maxLength={100}
                ayuda="Si lo dejas vacío usaremos tu nombre completo."
                value={nombreComercial}
                onChange={(evento) => setNombreComercial(evento.target.value)}
                error={errorDe('nombreComercial')}
              />
              <Selector
                etiqueta="Zona horaria"
                name="zonaHoraria"
                value={zonaHoraria}
                onChange={(evento) => setZonaHoraria(evento.target.value)}
                ayuda="Define en qué horario se generan los turnos de tu agenda."
                error={errorDe('zonaHoraria')}
              >
                {ZONAS.map((zona) => (
                  <option key={zona} value={zona}>
                    {zona}
                  </option>
                ))}
              </Selector>
            </fieldset>

            <fieldset className="grupo">
              <legend className="grupo__titulo">Servicios que ofreces</legend>
              <p className="campo__ayuda">
                Debes registrar al menos un servicio. Podrás añadir más desde tu panel.
              </p>
              {errorCategorias ? (
                <p className="estado estado--error" role="alert">
                  No se pudieron cargar las categorías: {mensajeDeError(errorCategorias)}
                </p>
              ) : null}

              {servicios.map((fila, indice) => (
                <div className="fila-servicio" key={fila.clave}>
                  <p className="fila-servicio__titulo">Servicio {indice + 1}</p>
                  <Campo
                    etiqueta="Nombre del servicio"
                    name={`servicios[${indice}].nombre`}
                    maxLength={100}
                    value={fila.nombre}
                    onChange={(evento) => actualizarFila(fila.clave, { nombre: evento.target.value })}
                    error={errorDe(`servicios[${indice}].nombre`)}
                  />
                  <Selector
                    etiqueta="Categoría"
                    name={`servicios[${indice}].idCategoria`}
                    value={fila.idCategoria}
                    onChange={(evento) =>
                      actualizarFila(fila.clave, { idCategoria: evento.target.value })
                    }
                    error={errorDe(`servicios[${indice}].idCategoria`)}
                  >
                    <option value="">Selecciona una categoría</option>
                    {categorias.map((categoria) => (
                      <option key={categoria.id} value={categoria.id}>
                        {categoria.nombre}
                      </option>
                    ))}
                  </Selector>
                  <div className="fila-servicio__numeros">
                    <Campo
                      etiqueta="Duración (minutos)"
                      type="number"
                      name={`servicios[${indice}].duracionMinutos`}
                      min={5}
                      max={480}
                      step={5}
                      value={fila.duracionMinutos}
                      onChange={(evento) =>
                        actualizarFila(fila.clave, { duracionMinutos: evento.target.value })
                      }
                      error={errorDe(`servicios[${indice}].duracionMinutos`)}
                    />
                    <Campo
                      etiqueta="Cupos por turno"
                      type="number"
                      name={`servicios[${indice}].capacidad`}
                      min={1}
                      value={fila.capacidad}
                      onChange={(evento) =>
                        actualizarFila(fila.clave, { capacidad: evento.target.value })
                      }
                      error={errorDe(`servicios[${indice}].capacidad`)}
                    />
                  </div>
                  {servicios.length > 1 ? (
                    <button
                      type="button"
                      className="boton boton--secundario"
                      onClick={() =>
                        setServicios((filas) => filas.filter((otra) => otra.clave !== fila.clave))
                      }
                    >
                      Quitar el servicio {indice + 1}
                    </button>
                  ) : null}
                </div>
              ))}

              <button
                type="button"
                className="boton boton--secundario"
                onClick={() => setServicios((filas) => [...filas, filaVacia()])}
              >
                Añadir otro servicio
              </button>
            </fieldset>
          </>
        ) : null}

        <div aria-live="polite" className="region-anuncios">
          {error ? (
            <div className="estado estado--error" role="alert">
              <p className="estado__detalle">{mensajeDeError(error)}</p>
              {codigo === 'EMAIL_YA_REGISTRADO' ? (
                <p className="estado__ayuda">
                  Ese correo ya tiene cuenta. <Link to="/ingresar">Ingresa</Link> o usa otro correo.
                </p>
              ) : null}
              {codigo === 'SERVICIO_REQUERIDO' ? (
                <p className="estado__ayuda">
                  Completa al menos un servicio con nombre y categoría antes de continuar.
                </p>
              ) : null}
              {errores.length > 0 ? (
                <ul className="lista-errores">
                  {errores.map((detalle) => (
                    <li key={`${detalle.campo}-${detalle.mensaje}`}>
                      <strong>{detalle.campo}</strong>: {detalle.mensaje}
                    </li>
                  ))}
                </ul>
              ) : null}
              {traceId ? <TraceId valor={traceId} /> : null}
            </div>
          ) : null}
        </div>

        <button type="submit" className="boton boton--principal" disabled={enviando}>
          {enviando ? 'Creando la cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p className="texto-apoyo">
        ¿Ya tienes cuenta? <Link to="/ingresar">Ingresar</Link>.
      </p>
    </div>
  );
}

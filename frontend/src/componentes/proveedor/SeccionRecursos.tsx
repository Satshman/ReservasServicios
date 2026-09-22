import { useCallback, useEffect, useState } from 'react';
import { asignarRecursos, crearRecurso, listarRecursos, listarTiposRecurso } from '../../api/api';
import type { ElementoCatalogo, Recurso, Servicio } from '../../api/tipos';
import { Anuncio, CajaError, Cargando, Vacio } from '../../componentes/Estados';
import { useSesionExpirada } from '../../sesion/useSesionExpirada';
import { Campo, Selector } from '../Campo';

interface Props {
  token: string;
  servicios: Servicio[];
}

export function SeccionRecursos({ token, servicios }: Props) {
  const manejarSesionExpirada = useSesionExpirada();

  const [recursos, setRecursos] = useState<Recurso[]>([]);
  const [tipos, setTipos] = useState<ElementoCatalogo[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  const [nombre, setNombre] = useState('');
  const [idTipoRecurso, setIdTipoRecurso] = useState('');
  const [creando, setCreando] = useState(false);
  const [errorAccion, setErrorAccion] = useState<unknown>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const [idServicio, setIdServicio] = useState<string>(
    servicios.length > 0 ? String(servicios[0].id) : '',
  );
  const [seleccionados, setSeleccionados] = useState<number[]>([]);
  const [asignando, setAsignando] = useState(false);

  const recargar = useCallback(() => setIntento((n) => n + 1), []);

  useEffect(() => {
    const control = new AbortController();
    listarTiposRecurso({ signal: control.signal })
      .then(setTipos)
      .catch(() => setTipos([]));
    return () => control.abort();
  }, []);

  useEffect(() => {
    const control = new AbortController();
    setCargando(true);
    setError(null);
    listarRecursos(token, { signal: control.signal })
      .then((datos) => {
        setRecursos(datos);
        setCargando(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        if (!manejarSesionExpirada(fallo)) setError(fallo);
        setCargando(false);
      });
    return () => control.abort();
  }, [token, intento, manejarSesionExpirada]);

  useEffect(() => {
    if (servicios.length > 0 && idServicio === '') setIdServicio(String(servicios[0].id));
  }, [servicios, idServicio]);

  async function crear(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setCreando(true);
    setErrorAccion(null);
    setAviso(null);
    try {
      const recurso = await crearRecurso(token, {
        nombre: nombre.trim(),
        idTipoRecurso: Number(idTipoRecurso),
      });
      setAviso(`Recurso "${recurso.nombre}" creado.`);
      setNombre('');
      setIdTipoRecurso('');
      recargar();
    } catch (fallo: unknown) {
      if (!manejarSesionExpirada(fallo)) setErrorAccion(fallo);
    } finally {
      setCreando(false);
    }
  }

  async function asignar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!idServicio) return;
    setAsignando(true);
    setErrorAccion(null);
    setAviso(null);
    try {
      const asignados = await asignarRecursos(token, Number(idServicio), seleccionados);
      setAviso(
        asignados.length === 0
          ? 'Se quitaron todos los recursos del servicio.'
          : `El servicio quedó con ${asignados.length} recurso(s) asignado(s).`,
      );
    } catch (fallo: unknown) {
      if (!manejarSesionExpirada(fallo)) setErrorAccion(fallo);
    } finally {
      setAsignando(false);
    }
  }

  const nombreTipo = (id: number) => tipos.find((tipo) => tipo.id === id)?.nombre ?? `Tipo ${id}`;

  return (
    <>
      <Anuncio mensaje={aviso} tono="exito" />
      {errorAccion ? <CajaError error={errorAccion} titulo="No se pudo completar la acción" /> : null}

      <form className="formulario" onSubmit={crear} noValidate>
        <fieldset className="grupo">
          <legend className="grupo__titulo">Crear recurso</legend>
          <Campo
            etiqueta="Nombre del recurso"
            required
            maxLength={100}
            value={nombre}
            ayuda="Por ejemplo: Consultorio 1, Silla 2, Estilista Ana."
            onChange={(evento) => setNombre(evento.target.value)}
          />
          <Selector
            etiqueta="Tipo de recurso"
            required
            value={idTipoRecurso}
            onChange={(evento) => setIdTipoRecurso(evento.target.value)}
          >
            <option value="">Selecciona un tipo</option>
            {tipos.map((tipo) => (
              <option key={tipo.id} value={tipo.id}>
                {tipo.nombre}
              </option>
            ))}
          </Selector>
          <button
            type="submit"
            className="boton boton--principal"
            disabled={creando || nombre.trim() === '' || idTipoRecurso === ''}
          >
            {creando ? 'Creando…' : 'Crear recurso'}
          </button>
        </fieldset>
      </form>

      <h3 className="subtitulo">Mis recursos</h3>

      {cargando ? <Cargando mensaje="Cargando recursos…" /> : null}

      {!cargando && error ? (
        <CajaError error={error} titulo="No se pudieron cargar los recursos" onReintentar={recargar} />
      ) : null}

      {!cargando && !error && recursos.length === 0 ? (
        <Vacio
          titulo="Aún no registras recursos"
          detalle="Los recursos son salas, equipos o personal que una reserva ocupa por completo durante el turno."
        />
      ) : null}

      {!cargando && !error && recursos.length > 0 ? (
        <>
          <ul className="lista-recursos">
            {recursos.map((recurso) => (
              <li key={recurso.id}>
                <span className="lista-recursos__nombre">{recurso.nombre}</span>
                <span className="lista-recursos__tipo">{nombreTipo(recurso.idTipoRecurso)}</span>
                <span className={`insignia insignia--${recurso.activo ? 'activo' : 'inactivo'}`}>
                  {recurso.activo ? 'Activo' : 'Inactivo'}
                </span>
              </li>
            ))}
          </ul>

          {servicios.length > 0 ? (
            <form className="formulario" onSubmit={asignar} noValidate>
              <fieldset className="grupo">
                <legend className="grupo__titulo">Asignar recursos a un servicio</legend>
                <p className="campo__ayuda">
                  La asignación reemplaza la anterior: cada reserva del servicio ocupará todos los
                  recursos marcados.
                </p>
                <Selector
                  etiqueta="Servicio"
                  value={idServicio}
                  onChange={(evento) => setIdServicio(evento.target.value)}
                >
                  {servicios.map((servicio) => (
                    <option key={servicio.id} value={servicio.id}>
                      {servicio.nombre}
                    </option>
                  ))}
                </Selector>
                <fieldset className="grupo grupo--plano">
                  <legend className="campo__etiqueta">Recursos</legend>
                  <div className="dias">
                    {recursos
                      .filter((recurso) => recurso.activo)
                      .map((recurso) => (
                        <label className="dia-opcion" key={recurso.id}>
                          <input
                            type="checkbox"
                            checked={seleccionados.includes(recurso.id)}
                            onChange={() =>
                              setSeleccionados((actuales) =>
                                actuales.includes(recurso.id)
                                  ? actuales.filter((id) => id !== recurso.id)
                                  : [...actuales, recurso.id],
                              )
                            }
                          />
                          <span>{recurso.nombre}</span>
                        </label>
                      ))}
                  </div>
                </fieldset>
                <button type="submit" className="boton boton--principal" disabled={asignando}>
                  {asignando ? 'Guardando…' : 'Guardar asignación'}
                </button>
              </fieldset>
            </form>
          ) : null}
        </>
      ) : null}
    </>
  );
}

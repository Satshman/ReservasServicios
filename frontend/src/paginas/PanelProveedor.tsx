import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { crearServicio, listarCategorias, listarServicios } from '../api/api';
import { descubrirIdProveedor } from '../api/proveedor';
import type { ElementoCatalogo, Servicio } from '../api/tipos';
import { Campo, Selector } from '../componentes/Campo';
import { Anuncio, CajaError, Cargando, Vacio } from '../componentes/Estados';
import { SeccionAgenda } from '../componentes/proveedor/SeccionAgenda';
import { SeccionRecursos } from '../componentes/proveedor/SeccionRecursos';
import { useSesion } from '../sesion/useSesion';
import { useSesionExpirada } from '../sesion/useSesionExpirada';
import { duracionLegible } from '../utiles/fechas';

type Pestana = 'servicios' | 'agenda' | 'recursos';

const PESTANAS: ReadonlyArray<{ id: Pestana; texto: string }> = [
  { id: 'servicios', texto: 'Mis servicios' },
  { id: 'agenda', texto: 'Agenda' },
  { id: 'recursos', texto: 'Recursos' },
];

export function PanelProveedor() {
  const { token, sesion, recordarProveedor } = useSesion();
  const manejarSesionExpirada = useSesionExpirada();

  const [pestana, setPestana] = useState<Pestana>('servicios');
  const [servicios, setServicios] = useState<Servicio[]>([]);
  const [categorias, setCategorias] = useState<ElementoCatalogo[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [idCategoria, setIdCategoria] = useState('');
  const [duracionMinutos, setDuracionMinutos] = useState('30');
  const [capacidad, setCapacidad] = useState('1');
  const [creando, setCreando] = useState(false);
  const [errorCreacion, setErrorCreacion] = useState<unknown>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Mi negocio — ReservaYa';
  }, []);

  useEffect(() => {
    const control = new AbortController();
    listarCategorias({ signal: control.signal })
      .then(setCategorias)
      .catch(() => setCategorias([]));
    return () => control.abort();
  }, []);

  const recargar = useCallback(() => setIntento((n) => n + 1), []);

  // El id de proveedor no viaja en el token, así que se descubre una vez y se
  // memoriza en la sesión (ver src/api/proveedor.ts).
  useEffect(() => {
    if (!token) return;
    const control = new AbortController();
    let cancelado = false;
    setCargando(true);
    setError(null);

    (async () => {
      try {
        const todos = await listarServicios({}, { signal: control.signal });
        let idProveedor = sesion?.idProveedor ?? null;
        if (idProveedor === null) {
          idProveedor = await descubrirIdProveedor(token, todos, control.signal);
          if (idProveedor !== null) recordarProveedor(idProveedor);
        }
        if (cancelado) return;
        setServicios(
          idProveedor === null
            ? []
            : todos.filter((servicio) => servicio.idProveedor === idProveedor),
        );
        setCargando(false);
      } catch (fallo: unknown) {
        if (cancelado || control.signal.aborted) return;
        if (!manejarSesionExpirada(fallo)) setError(fallo);
        setCargando(false);
      }
    })();

    return () => {
      cancelado = true;
      control.abort();
    };
  }, [token, intento, sesion?.idProveedor, recordarProveedor, manejarSesionExpirada]);

  async function crear(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!token) return;
    setCreando(true);
    setErrorCreacion(null);
    setAviso(null);
    try {
      const servicio = await crearServicio(token, {
        nombre: nombre.trim(),
        ...(descripcion.trim() ? { descripcion: descripcion.trim() } : {}),
        idCategoria: Number(idCategoria),
        duracionMinutos: Number(duracionMinutos),
        capacidad: Number(capacidad),
      });
      recordarProveedor(servicio.idProveedor);
      setAviso(`Servicio "${servicio.nombre}" creado.`);
      setNombre('');
      setDescripcion('');
      setIdCategoria('');
      recargar();
    } catch (fallo: unknown) {
      if (!manejarSesionExpirada(fallo)) setErrorCreacion(fallo);
    } finally {
      setCreando(false);
    }
  }

  const nombreCategoria = (id: number) =>
    categorias.find((categoria) => categoria.id === id)?.nombre ?? 'Sin categoría';

  return (
    <div className="pagina">
      <header className="encabezado-panel">
        <div>
          <h1>Mi negocio</h1>
          <p className="texto-apoyo">Sesión de {sesion?.email}.</p>
        </div>
        <Link className="boton boton--secundario" to="/">
          Ver el catálogo público
        </Link>
      </header>

      <nav className="pestanas" aria-label="Secciones del panel">
        <ul className="pestanas__lista">
          {PESTANAS.map((opcion) => (
            <li key={opcion.id}>
              <button
                type="button"
                className={`pestana${pestana === opcion.id ? ' pestana--activa' : ''}`}
                aria-current={pestana === opcion.id ? 'page' : undefined}
                onClick={() => setPestana(opcion.id)}
              >
                {opcion.texto}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      {cargando ? <Cargando mensaje="Cargando tu negocio…" /> : null}

      {!cargando && error ? (
        <CajaError error={error} titulo="No se pudo cargar tu información" onReintentar={recargar} />
      ) : null}

      {!cargando && !error && pestana === 'servicios' ? (
        <section aria-labelledby="titulo-servicios">
          <h2 id="titulo-servicios">Mis servicios</h2>

          <Anuncio mensaje={aviso} tono="exito" />
          {errorCreacion ? (
            <CajaError error={errorCreacion} titulo="No se pudo crear el servicio" />
          ) : null}

          <form className="formulario" onSubmit={crear} noValidate>
            <fieldset className="grupo">
              <legend className="grupo__titulo">Publicar un servicio</legend>
              <Campo
                etiqueta="Nombre del servicio"
                required
                maxLength={100}
                value={nombre}
                onChange={(evento) => setNombre(evento.target.value)}
              />
              <Campo
                etiqueta="Descripción (opcional)"
                maxLength={1000}
                value={descripcion}
                onChange={(evento) => setDescripcion(evento.target.value)}
              />
              <Selector
                etiqueta="Categoría"
                required
                value={idCategoria}
                onChange={(evento) => setIdCategoria(evento.target.value)}
              >
                <option value="">Selecciona una categoría</option>
                {categorias.map((categoria) => (
                  <option key={categoria.id} value={categoria.id}>
                    {categoria.nombre}
                  </option>
                ))}
              </Selector>
              <div className="fila-horas">
                <Campo
                  etiqueta="Duración (minutos)"
                  type="number"
                  min={5}
                  max={480}
                  step={5}
                  required
                  value={duracionMinutos}
                  onChange={(evento) => setDuracionMinutos(evento.target.value)}
                />
                <Campo
                  etiqueta="Cupos por turno"
                  type="number"
                  min={1}
                  required
                  value={capacidad}
                  onChange={(evento) => setCapacidad(evento.target.value)}
                />
              </div>
              <button
                type="submit"
                className="boton boton--principal"
                disabled={creando || nombre.trim() === '' || idCategoria === ''}
              >
                {creando ? 'Creando…' : 'Crear servicio'}
              </button>
            </fieldset>
          </form>

          <h3 className="subtitulo">Servicios publicados</h3>

          {servicios.length === 0 ? (
            <Vacio
              titulo="Aún no tienes servicios publicados"
              detalle="Crea tu primer servicio con el formulario de arriba para poder definir su agenda."
            />
          ) : (
            <ul className="rejilla">
              {servicios.map((servicio) => (
                <li key={servicio.id}>
                  <article className="tarjeta">
                    <h4 className="tarjeta__titulo">{servicio.nombre}</h4>
                    {servicio.descripcion ? (
                      <p className="tarjeta__descripcion">{servicio.descripcion}</p>
                    ) : null}
                    <ul className="atributos">
                      <li>
                        <span className="atributos__clave">Categoría</span>
                        <span className="atributos__valor">
                          {nombreCategoria(servicio.idCategoria)}
                        </span>
                      </li>
                      <li>
                        <span className="atributos__clave">Duración</span>
                        <span className="atributos__valor">
                          {duracionLegible(servicio.duracionMinutos)}
                        </span>
                      </li>
                      <li>
                        <span className="atributos__clave">Cupos por turno</span>
                        <span className="atributos__valor">{servicio.capacidad}</span>
                      </li>
                      <li>
                        <span className="atributos__clave">Estado</span>
                        <span className="atributos__valor">
                          <span className={`insignia insignia--${servicio.estado.toLowerCase()}`}>
                            {servicio.estado}
                          </span>
                        </span>
                      </li>
                    </ul>
                    <Link className="enlace-discreto" to={`/servicios/${servicio.id}`}>
                      Ver la ficha pública de {servicio.nombre}
                    </Link>
                  </article>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}

      {!cargando && !error && pestana === 'agenda' && token ? (
        <section aria-labelledby="titulo-agenda">
          <h2 id="titulo-agenda">Agenda</h2>
          <SeccionAgenda token={token} servicios={servicios} />
        </section>
      ) : null}

      {!cargando && !error && pestana === 'recursos' && token ? (
        <section aria-labelledby="titulo-recursos">
          <h2 id="titulo-recursos">Recursos</h2>
          <SeccionRecursos token={token} servicios={servicios} />
        </section>
      ) : null}
    </div>
  );
}

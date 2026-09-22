import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarCategorias, listarServicios } from '../api/api';
import type { ElementoCatalogo, Servicio } from '../api/tipos';
import { CajaError, Cargando, Vacio } from '../componentes/Estados';
import { duracionLegible } from '../utiles/fechas';

export function Catalogo() {
  const [servicios, setServicios] = useState<Servicio[]>([]);
  const [categorias, setCategorias] = useState<ElementoCatalogo[]>([]);
  const [idCategoria, setIdCategoria] = useState<string>('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    document.title = 'Catálogo de servicios — ReservaYa';
  }, []);

  // Las categorías alimentan el filtro: GET /catalogos/categorias.
  useEffect(() => {
    const control = new AbortController();
    listarCategorias({ signal: control.signal })
      .then(setCategorias)
      .catch(() => setCategorias([]));
    return () => control.abort();
  }, []);

  // El catálogo se recarga al cambiar el filtro: GET /servicios?idCategoria=
  useEffect(() => {
    const control = new AbortController();
    setCargando(true);
    setError(null);
    listarServicios(
      idCategoria ? { idCategoria: Number(idCategoria) } : {},
      { signal: control.signal },
    )
      .then((datos) => {
        setServicios(datos);
        setCargando(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setError(fallo);
        setCargando(false);
      });
    return () => control.abort();
  }, [idCategoria, intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);

  const nombreCategoria = (id: number) =>
    categorias.find((categoria) => categoria.id === id)?.nombre ?? 'Sin categoría';

  return (
    <div className="pagina">
      <section className="portada">
        <h1>Reserva tu turno en minutos</h1>
        <p className="portada__texto">
          Explora los servicios publicados por nuestros proveedores, consulta la disponibilidad real
          de cada agenda y confirma tu reserva.
        </p>
      </section>

      <section aria-labelledby="titulo-catalogo">
        <div className="barra-filtros">
          <h2 id="titulo-catalogo">Servicios disponibles</h2>
          <div className="campo campo--compacto">
            <label className="campo__etiqueta" htmlFor="filtro-categoria">
              Filtrar por categoría
            </label>
            <select
              id="filtro-categoria"
              className="campo__control"
              value={idCategoria}
              onChange={(evento) => setIdCategoria(evento.target.value)}
            >
              <option value="">Todas las categorías</option>
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>
                  {categoria.nombre}
                </option>
              ))}
            </select>
          </div>
        </div>

        {cargando ? <Cargando mensaje="Cargando servicios…" /> : null}

        {!cargando && error ? (
          <CajaError
            error={error}
            titulo="No se pudo cargar el catálogo"
            onReintentar={reintentar}
          />
        ) : null}

        {!cargando && !error && servicios.length === 0 ? (
          <Vacio
            titulo="Todavía no hay servicios para mostrar"
            detalle={
              idCategoria
                ? 'Ningún servicio activo pertenece a esa categoría. Prueba con otra o quita el filtro.'
                : 'Cuando un proveedor publique su primer servicio aparecerá aquí.'
            }
            accion={
              idCategoria ? (
                <button
                  type="button"
                  className="boton boton--secundario"
                  onClick={() => setIdCategoria('')}
                >
                  Ver todas las categorías
                </button>
              ) : (
                <Link className="boton boton--secundario" to="/registro">
                  Registrar mi negocio
                </Link>
              )
            }
          />
        ) : null}

        {!cargando && !error && servicios.length > 0 ? (
          <ul className="rejilla" aria-label="Listado de servicios">
            {servicios.map((servicio) => (
              <li key={servicio.id}>
                <article className="tarjeta">
                  <p className="tarjeta__proveedor">{servicio.nombreProveedor}</p>
                  <h3 className="tarjeta__titulo">
                    <Link to={`/servicios/${servicio.id}`}>{servicio.nombre}</Link>
                  </h3>
                  {servicio.descripcion ? (
                    <p className="tarjeta__descripcion">{servicio.descripcion}</p>
                  ) : null}
                  <ul className="atributos">
                    <li>
                      <span className="atributos__clave">Categoría</span>
                      <span className="atributos__valor">{nombreCategoria(servicio.idCategoria)}</span>
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
                  </ul>
                  <Link className="boton boton--principal" to={`/servicios/${servicio.id}`}>
                    Ver disponibilidad de {servicio.nombre}
                  </Link>
                </article>
              </li>
            ))}
          </ul>
        ) : null}
      </section>
    </div>
  );
}

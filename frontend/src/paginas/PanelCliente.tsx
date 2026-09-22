import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarMisReservas } from '../api/api';
import type { Reserva } from '../api/tipos';
import { CajaError, Cargando, Vacio } from '../componentes/Estados';
import { useSesion } from '../sesion/useSesion';
import { useSesionExpirada } from '../sesion/useSesionExpirada';
import { esFuturo, fechaLarga, soloHora } from '../utiles/fechas';

function TablaReservas({ reservas, titulo, id }: { reservas: Reserva[]; titulo: string; id: string }) {
  return (
    <section aria-labelledby={id}>
      <h2 id={id}>{titulo}</h2>
      <ul className="lista-reservas">
        {reservas.map((reserva) => (
          <li key={reserva.id}>
            <article className="tarjeta tarjeta--reserva">
              <h3 className="tarjeta__titulo">{reserva.nombreServicio}</h3>
              <p className="tarjeta__descripcion">
                {fechaLarga(reserva.fechaHoraInicio)} · {soloHora(reserva.fechaHoraInicio)} a{' '}
                {soloHora(reserva.fechaHoraFin)}
              </p>
              <ul className="atributos">
                <li>
                  <span className="atributos__clave">Estado</span>
                  <span className="atributos__valor">
                    <span className={`insignia insignia--${reserva.estado.toLowerCase()}`}>
                      {reserva.estado}
                    </span>
                  </span>
                </li>
                <li>
                  <span className="atributos__clave">Reserva</span>
                  <span className="atributos__valor">#{reserva.id}</span>
                </li>
                {reserva.recursos.length > 0 ? (
                  <li>
                    <span className="atributos__clave">Recursos</span>
                    <span className="atributos__valor">
                      {reserva.recursos.map((recurso) => recurso.nombre).join(', ')}
                    </span>
                  </li>
                ) : null}
              </ul>
              <Link className="enlace-discreto" to={`/servicios/${reserva.idServicio}`}>
                Ver el servicio {reserva.nombreServicio}
              </Link>
            </article>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function PanelCliente() {
  const { token, sesion } = useSesion();
  const manejarSesionExpirada = useSesionExpirada();
  const [reservas, setReservas] = useState<Reserva[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    document.title = 'Mis reservas — ReservaYa';
  }, []);

  useEffect(() => {
    if (!token) return;
    const control = new AbortController();
    setCargando(true);
    setError(null);
    listarMisReservas(token, { signal: control.signal })
      .then((datos) => {
        setReservas(datos);
        setCargando(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        if (!manejarSesionExpirada(fallo)) setError(fallo);
        setCargando(false);
      });
    return () => control.abort();
  }, [token, intento, manejarSesionExpirada]);

  const { proximas, pasadas } = useMemo(() => {
    const ordenadas = [...reservas].sort((a, b) =>
      a.fechaHoraInicio.localeCompare(b.fechaHoraInicio),
    );
    return {
      proximas: ordenadas.filter((reserva) => esFuturo(reserva.fechaHoraInicio)),
      pasadas: ordenadas
        .filter((reserva) => !esFuturo(reserva.fechaHoraInicio))
        .reverse(),
    };
  }, [reservas]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);

  return (
    <div className="pagina">
      <header className="encabezado-panel">
        <div>
          <h1>Mis reservas</h1>
          <p className="texto-apoyo">Sesión de {sesion?.email}.</p>
        </div>
        <Link className="boton boton--principal" to="/">
          Explorar el catálogo
        </Link>
      </header>

      {cargando ? <Cargando mensaje="Cargando tus reservas…" /> : null}

      {!cargando && error ? (
        <CajaError error={error} titulo="No se pudieron cargar tus reservas" onReintentar={reintentar} />
      ) : null}

      {!cargando && !error && reservas.length === 0 ? (
        <Vacio
          titulo="Todavía no tienes reservas"
          detalle="Busca un servicio en el catálogo, elige un turno libre y confírmalo."
          accion={
            <Link className="boton boton--principal" to="/">
              Ver servicios disponibles
            </Link>
          }
        />
      ) : null}

      {!cargando && !error && proximas.length > 0 ? (
        <TablaReservas reservas={proximas} titulo="Próximas" id="titulo-proximas" />
      ) : null}

      {!cargando && !error && reservas.length > 0 && proximas.length === 0 ? (
        <Vacio
          titulo="No tienes reservas próximas"
          detalle="Tus reservas anteriores siguen disponibles en el historial."
          accion={
            <Link className="boton boton--secundario" to="/">
              Reservar otro turno
            </Link>
          }
        />
      ) : null}

      {!cargando && !error && pasadas.length > 0 ? (
        <TablaReservas reservas={pasadas} titulo="Historial" id="titulo-pasadas" />
      ) : null}
    </div>
  );
}

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { consultarDisponibilidad, consultarServicio, crearReserva } from '../api/api';
import { recursosEnConflicto, sugerenciasDeError } from '../api/cliente';
import type { Disponibilidad, Servicio, SugerenciaTurno, TurnoDisponible } from '../api/tipos';
import { Anuncio, CajaError, Cargando, Vacio } from '../componentes/Estados';
import { useSesion } from '../sesion/useSesion';
import { useSesionExpirada } from '../sesion/useSesionExpirada';
import {
  diaDeTurno,
  duracionLegible,
  fechaLarga,
  hoyISO,
  rangoProximosDias,
  soloHora,
} from '../utiles/fechas';

interface EstadoNavegacion {
  turnoPendiente?: string;
}

export function DetalleServicio() {
  const { id } = useParams<{ id: string }>();
  const idServicio = Number(id);
  const { token, sesion } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const manejarSesionExpirada = useSesionExpirada();

  const rangoInicial = useMemo(() => rangoProximosDias(7), []);
  const [desde, setDesde] = useState(rangoInicial.desde);
  const [hasta, setHasta] = useState(rangoInicial.hasta);

  const [servicio, setServicio] = useState<Servicio | null>(null);
  const [errorServicio, setErrorServicio] = useState<unknown>(null);
  const [cargandoServicio, setCargandoServicio] = useState(true);

  const [disponibilidad, setDisponibilidad] = useState<Disponibilidad | null>(null);
  const [errorDisponibilidad, setErrorDisponibilidad] = useState<unknown>(null);
  const [cargandoTurnos, setCargandoTurnos] = useState(true);
  const [intento, setIntento] = useState(0);

  const [reservando, setReservando] = useState<string | null>(null);
  const [errorReserva, setErrorReserva] = useState<unknown>(null);
  const [exito, setExito] = useState<string | null>(null);

  const turnoPendiente = (ubicacion.state as EstadoNavegacion | null)?.turnoPendiente ?? null;
  const pendienteProcesado = useRef(false);

  /* ------------------------------------------------------------ carga de datos */

  useEffect(() => {
    if (!Number.isFinite(idServicio)) return;
    const control = new AbortController();
    setCargandoServicio(true);
    setErrorServicio(null);
    consultarServicio(idServicio, { signal: control.signal })
      .then((datos) => {
        setServicio(datos);
        document.title = `${datos.nombre} — ReservaYa`;
        setCargandoServicio(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setErrorServicio(fallo);
        setCargandoServicio(false);
      });
    return () => control.abort();
  }, [idServicio]);

  const recargarTurnos = useCallback(() => setIntento((n) => n + 1), []);

  useEffect(() => {
    if (!Number.isFinite(idServicio) || !desde || !hasta || desde > hasta) return;
    const control = new AbortController();
    setCargandoTurnos(true);
    setErrorDisponibilidad(null);
    consultarDisponibilidad(idServicio, desde, hasta, { signal: control.signal })
      .then((datos) => {
        setDisponibilidad(datos);
        setCargandoTurnos(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setErrorDisponibilidad(fallo);
        setDisponibilidad(null);
        setCargandoTurnos(false);
      });
    return () => control.abort();
  }, [idServicio, desde, hasta, intento]);

  /* ---------------------------------------------------------------- reservar */

  const reservar = useCallback(
    async (fechaHoraInicio: string) => {
      if (!token || !sesion) {
        // Sin sesión: se envía a ingresar y se vuelve exactamente a este turno.
        navegar('/ingresar', {
          state: {
            destino: `/servicios/${idServicio}`,
            turnoPendiente: fechaHoraInicio,
            aviso: 'Ingresa para confirmar la reserva del turno seleccionado.',
          },
        });
        return;
      }
      if (sesion.rol !== 'CLIENTE') {
        setErrorReserva(
          new Error('Solo las cuentas de cliente pueden reservar turnos.'),
        );
        return;
      }
      setReservando(fechaHoraInicio);
      setErrorReserva(null);
      setExito(null);
      try {
        const reserva = await crearReserva(token, idServicio, fechaHoraInicio);
        setExito(
          `Reserva confirmada para el ${fechaLarga(reserva.fechaHoraInicio, disponibilidad?.zonaHoraria)} a las ${soloHora(reserva.fechaHoraInicio, disponibilidad?.zonaHoraria)}.`,
        );
        recargarTurnos();
      } catch (fallo: unknown) {
        if (!manejarSesionExpirada(fallo)) setErrorReserva(fallo);
      } finally {
        setReservando(null);
      }
    },
    [token, sesion, navegar, idServicio, disponibilidad?.zonaHoraria, recargarTurnos, manejarSesionExpirada],
  );

  // Turno guardado antes de ingresar: se confirma al volver con sesión activa.
  useEffect(() => {
    if (!turnoPendiente || pendienteProcesado.current || !token) return;
    pendienteProcesado.current = true;
    navegar(ubicacion.pathname, { replace: true, state: null });
    void reservar(turnoPendiente);
  }, [turnoPendiente, token, reservar, navegar, ubicacion.pathname]);

  /* ---------------------------------------------------------------- agrupado */

  const porDia = useMemo(() => {
    if (!disponibilidad) return [];
    const mapa = new Map<string, TurnoDisponible[]>();
    for (const turno of disponibilidad.turnos) {
      const clave = diaDeTurno(turno.fechaHoraInicio, disponibilidad.zonaHoraria);
      const lista = mapa.get(clave);
      if (lista) lista.push(turno);
      else mapa.set(clave, [turno]);
    }
    return [...mapa.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [disponibilidad]);

  const sugerencias: SugerenciaTurno[] = sugerenciasDeError(errorReserva);
  const conflictos = recursosEnConflicto(errorReserva);
  const rangoInvalido = Boolean(desde && hasta && desde > hasta);

  if (!Number.isFinite(idServicio)) {
    return (
      <div className="pagina">
        <Vacio
          titulo="Servicio no válido"
          detalle="La dirección no corresponde a un servicio del catálogo."
          accion={
            <Link className="boton boton--principal" to="/">
              Volver al catálogo
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="pagina">
      <p className="migas">
        <Link to="/">Catálogo</Link> <span aria-hidden="true">›</span>{' '}
        {servicio ? servicio.nombre : 'Servicio'}
      </p>

      {cargandoServicio ? <Cargando mensaje="Cargando el servicio…" /> : null}

      {!cargandoServicio && errorServicio ? (
        <CajaError error={errorServicio} titulo="No se pudo cargar el servicio" />
      ) : null}

      {servicio ? (
        <>
          <section className="ficha" aria-labelledby="titulo-servicio">
            <p className="tarjeta__proveedor">{servicio.nombreProveedor}</p>
            <h1 id="titulo-servicio">{servicio.nombre}</h1>
            {servicio.descripcion ? <p className="ficha__descripcion">{servicio.descripcion}</p> : null}
            <ul className="atributos atributos--fila">
              <li>
                <span className="atributos__clave">Duración</span>
                <span className="atributos__valor">{duracionLegible(servicio.duracionMinutos)}</span>
              </li>
              <li>
                <span className="atributos__clave">Cupos por turno</span>
                <span className="atributos__valor">{servicio.capacidad}</span>
              </li>
              <li>
                <span className="atributos__clave">Zona horaria</span>
                <span className="atributos__valor">{servicio.zonaHoraria}</span>
              </li>
              <li>
                <span className="atributos__clave">Estado</span>
                <span className="atributos__valor">{servicio.estado}</span>
              </li>
            </ul>
          </section>

          <section aria-labelledby="titulo-disponibilidad">
            <h2 id="titulo-disponibilidad">Disponibilidad</h2>
            <p className="texto-apoyo">
              Los horarios se muestran en la zona del proveedor ({servicio.zonaHoraria}). El rango
              máximo de consulta es de 31 días.
            </p>

            <form
              className="rango"
              onSubmit={(evento) => {
                evento.preventDefault();
                recargarTurnos();
              }}
            >
              <div className="campo campo--compacto">
                <label className="campo__etiqueta" htmlFor="rango-desde">
                  Desde
                </label>
                <input
                  id="rango-desde"
                  className="campo__control"
                  type="date"
                  value={desde}
                  min={hoyISO()}
                  onChange={(evento) => setDesde(evento.target.value)}
                />
              </div>
              <div className="campo campo--compacto">
                <label className="campo__etiqueta" htmlFor="rango-hasta">
                  Hasta
                </label>
                <input
                  id="rango-hasta"
                  className="campo__control"
                  type="date"
                  value={hasta}
                  min={desde || hoyISO()}
                  onChange={(evento) => setHasta(evento.target.value)}
                />
              </div>
              <button type="submit" className="boton boton--principal">
                Actualizar disponibilidad
              </button>
            </form>

            {rangoInvalido ? (
              <p className="estado estado--error" role="alert">
                La fecha inicial no puede ser posterior a la final.
              </p>
            ) : null}

            <Anuncio mensaje={exito} tono="exito" />
            {exito ? (
              <p className="texto-apoyo">
                <Link to="/panel/cliente">Ver mis reservas</Link>
              </p>
            ) : null}

            {errorReserva ? (
              <CajaError error={errorReserva} titulo="No se pudo crear la reserva">
                {conflictos.length > 0 ? (
                  <p className="estado__detalle">
                    Recursos ocupados: {conflictos.map((recurso) => recurso.nombre).join(', ')}.
                  </p>
                ) : null}
                {sugerencias.length > 0 ? (
                  <div className="sugerencias">
                    <p className="sugerencias__titulo" id="titulo-sugerencias">
                      Turnos alternativos sugeridos
                    </p>
                    <ul className="sugerencias__lista" aria-labelledby="titulo-sugerencias">
                      {sugerencias.map((sugerencia) => (
                        <li key={sugerencia.fechaHoraInicio}>
                          <button
                            type="button"
                            className="boton boton--sugerencia"
                            disabled={reservando !== null}
                            onClick={() => void reservar(sugerencia.fechaHoraInicio)}
                          >
                            Reservar el{' '}
                            {fechaLarga(sugerencia.fechaHoraInicio, disponibilidad?.zonaHoraria)} a las{' '}
                            {soloHora(sugerencia.fechaHoraInicio, disponibilidad?.zonaHoraria)}
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </CajaError>
            ) : null}

            {cargandoTurnos ? <Cargando mensaje="Consultando turnos…" /> : null}

            {!cargandoTurnos && errorDisponibilidad ? (
              <CajaError
                error={errorDisponibilidad}
                titulo="No se pudo consultar la disponibilidad"
                onReintentar={recargarTurnos}
              />
            ) : null}

            {!cargandoTurnos && !errorDisponibilidad && porDia.length === 0 ? (
              <Vacio
                titulo="No hay turnos disponibles en este rango"
                detalle="Prueba con otras fechas: la agenda del proveedor puede no cubrir los días elegidos o los cupos ya están tomados."
              />
            ) : null}

            {!cargandoTurnos && !errorDisponibilidad && porDia.length > 0
              ? porDia.map(([dia, turnos]) => (
                  <section className="dia" key={dia} aria-labelledby={`dia-${dia}`}>
                    <h3 className="dia__titulo" id={`dia-${dia}`}>
                      {fechaLarga(turnos[0].fechaHoraInicio, disponibilidad?.zonaHoraria)}
                    </h3>
                    <ul className="turnos">
                      {turnos.map((turno) => (
                        <li key={turno.fechaHoraInicio}>
                          <button
                            type="button"
                            className="turno"
                            disabled={reservando !== null}
                            aria-busy={reservando === turno.fechaHoraInicio}
                            onClick={() => void reservar(turno.fechaHoraInicio)}
                          >
                            <span className="turno__hora">
                              {soloHora(turno.fechaHoraInicio, disponibilidad?.zonaHoraria)}
                            </span>
                            <span className="turno__cupos">
                              {turno.cuposDisponibles === 1
                                ? '1 cupo libre'
                                : `${turno.cuposDisponibles} cupos libres`}
                            </span>
                            <span className="turno__accion">
                              {reservando === turno.fechaHoraInicio ? 'Reservando…' : 'Reservar'}
                            </span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  </section>
                ))
              : null}
          </section>
        </>
      ) : null}
    </div>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { crearHorarios, editarHorario, eliminarHorario, listarHorarios } from '../../api/api';
import { reservasAfectadasDeError } from '../../api/cliente';
import type { Horario, ReservaAfectada, Servicio } from '../../api/tipos';
import { Anuncio, CajaError, Cargando, Vacio } from '../../componentes/Estados';
import { useSesionExpirada } from '../../sesion/useSesionExpirada';
import { DIAS_SEMANA, fechaYHora, horaCorta, nombreDia } from '../../utiles/fechas';
import { Campo, Selector } from '../Campo';
import { Dialogo } from '../Dialogo';

interface Props {
  token: string;
  servicios: Servicio[];
}

/** Operación en curso que quedó pendiente de confirmación por `HORARIO_CON_RESERVAS`. */
type Pendiente =
  | { tipo: 'editar'; idHorario: number; diaSemana: number; horaInicio: string; horaFin: string }
  | { tipo: 'eliminar'; idHorario: number };

export function SeccionAgenda({ token, servicios }: Props) {
  const manejarSesionExpirada = useSesionExpirada();

  const [idServicio, setIdServicio] = useState<string>(
    servicios.length > 0 ? String(servicios[0].id) : '',
  );
  const [horarios, setHorarios] = useState<Horario[]>([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  const [dias, setDias] = useState<number[]>([]);
  const [horaInicio, setHoraInicio] = useState('08:00');
  const [horaFin, setHoraFin] = useState('12:00');
  const [guardando, setGuardando] = useState(false);
  const [errorAccion, setErrorAccion] = useState<unknown>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const [editando, setEditando] = useState<number | null>(null);
  const [edicion, setEdicion] = useState<{ diaSemana: string; horaInicio: string; horaFin: string }>(
    { diaSemana: '1', horaInicio: '08:00', horaFin: '12:00' },
  );

  const [pendiente, setPendiente] = useState<Pendiente | null>(null);
  const [afectadas, setAfectadas] = useState<ReservaAfectada[]>([]);

  useEffect(() => {
    if (servicios.length > 0 && idServicio === '') setIdServicio(String(servicios[0].id));
  }, [servicios, idServicio]);

  const recargar = useCallback(() => setIntento((n) => n + 1), []);

  useEffect(() => {
    if (!idServicio) {
      setHorarios([]);
      return;
    }
    const control = new AbortController();
    setCargando(true);
    setError(null);
    listarHorarios(Number(idServicio), { signal: control.signal })
      .then((datos) => {
        setHorarios([...datos].sort((a, b) => a.diaSemana - b.diaSemana || a.horaInicio.localeCompare(b.horaInicio)));
        setCargando(false);
      })
      .catch((fallo: unknown) => {
        if (control.signal.aborted) return;
        setError(fallo);
        setCargando(false);
      });
    return () => control.abort();
  }, [idServicio, intento]);

  function alternarDia(dia: number) {
    setDias((actuales) =>
      actuales.includes(dia) ? actuales.filter((d) => d !== dia) : [...actuales, dia].sort(),
    );
  }

  async function crear(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!idServicio || dias.length === 0) {
      setErrorAccion(new Error('Selecciona al menos un día de la semana.'));
      return;
    }
    setGuardando(true);
    setErrorAccion(null);
    setAviso(null);
    try {
      const creados = await crearHorarios(token, Number(idServicio), {
        diasSemana: dias,
        horaInicio,
        horaFin,
      });
      setAviso(
        creados.length === 1
          ? 'Se creó 1 bloque de agenda.'
          : `Se crearon ${creados.length} bloques de agenda.`,
      );
      setDias([]);
      recargar();
    } catch (fallo: unknown) {
      if (!manejarSesionExpirada(fallo)) setErrorAccion(fallo);
    } finally {
      setGuardando(false);
    }
  }

  /**
   * Aplica la edición o el borrado. Si el backend responde
   * `409 HORARIO_CON_RESERVAS`, guarda la operación y abre el diálogo con la
   * lista de reservas afectadas; el usuario confirma y se repite con
   * `?confirmar=true`.
   */
  const aplicar = useCallback(
    async (operacion: Pendiente, confirmar: boolean) => {
      setGuardando(true);
      setErrorAccion(null);
      setAviso(null);
      try {
        if (operacion.tipo === 'editar') {
          await editarHorario(
            token,
            operacion.idHorario,
            {
              diaSemana: operacion.diaSemana,
              horaInicio: operacion.horaInicio,
              horaFin: operacion.horaFin,
            },
            confirmar,
          );
          setAviso('Bloque actualizado.');
          setEditando(null);
        } else {
          await eliminarHorario(token, operacion.idHorario, confirmar);
          setAviso('Bloque eliminado.');
        }
        setPendiente(null);
        setAfectadas([]);
        recargar();
      } catch (fallo: unknown) {
        if (manejarSesionExpirada(fallo)) return;
        const reservas = reservasAfectadasDeError(fallo);
        if (reservas.length > 0) {
          setPendiente(operacion);
          setAfectadas(reservas);
        } else {
          setPendiente(null);
          setAfectadas([]);
          setErrorAccion(fallo);
        }
      } finally {
        setGuardando(false);
      }
    },
    [token, recargar, manejarSesionExpirada],
  );

  const servicioActual = servicios.find((servicio) => String(servicio.id) === idServicio);

  if (servicios.length === 0) {
    return (
      <Vacio
        titulo="Todavía no hay servicios que agendar"
        detalle="Crea primero un servicio y después define sus bloques de atención."
      />
    );
  }

  return (
    <>
      <Selector
        etiqueta="Servicio"
        value={idServicio}
        onChange={(evento) => {
          setIdServicio(evento.target.value);
          setEditando(null);
          setAviso(null);
          setErrorAccion(null);
        }}
        ayuda="La agenda se define por servicio: cada bloque es un rango semanal recurrente."
      >
        {servicios.map((servicio) => (
          <option key={servicio.id} value={servicio.id}>
            {servicio.nombre}
          </option>
        ))}
      </Selector>

      <Anuncio mensaje={aviso} tono="exito" />
      {errorAccion ? <CajaError error={errorAccion} titulo="No se pudo aplicar el cambio" /> : null}

      <form className="formulario formulario--bloque" onSubmit={crear} noValidate>
        <fieldset className="grupo">
          <legend className="grupo__titulo">Crear bloques</legend>
          <fieldset className="grupo grupo--plano">
            <legend className="campo__etiqueta">Días de la semana</legend>
            <div className="dias">
              {DIAS_SEMANA.map((dia) => (
                <label className="dia-opcion" key={dia.valor}>
                  <input
                    type="checkbox"
                    checked={dias.includes(dia.valor)}
                    onChange={() => alternarDia(dia.valor)}
                  />
                  <span>{dia.nombre}</span>
                </label>
              ))}
            </div>
            <p className="campo__ayuda">
              Puedes seleccionar varios días: se crea un bloque por cada uno, todo o nada.
            </p>
          </fieldset>
          <div className="fila-horas">
            <Campo
              etiqueta="Hora de inicio"
              type="time"
              value={horaInicio}
              required
              onChange={(evento) => setHoraInicio(evento.target.value)}
            />
            <Campo
              etiqueta="Hora de fin"
              type="time"
              value={horaFin}
              required
              onChange={(evento) => setHoraFin(evento.target.value)}
            />
          </div>
          {servicioActual ? (
            <p className="campo__ayuda">
              El bloque debe durar al menos {servicioActual.duracionMinutos} minutos, la duración de{' '}
              {servicioActual.nombre}.
            </p>
          ) : null}
          <button type="submit" className="boton boton--principal" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Crear bloques'}
          </button>
        </fieldset>
      </form>

      <h3 className="subtitulo">Bloques definidos</h3>

      {cargando ? <Cargando mensaje="Cargando la agenda…" /> : null}

      {!cargando && error ? (
        <CajaError error={error} titulo="No se pudo cargar la agenda" onReintentar={recargar} />
      ) : null}

      {!cargando && !error && horarios.length === 0 ? (
        <Vacio
          titulo="Este servicio aún no tiene agenda"
          detalle="Crea al menos un bloque para que los clientes puedan ver turnos disponibles."
        />
      ) : null}

      {!cargando && !error && horarios.length > 0 ? (
        <ul className="lista-bloques">
          {horarios.map((horario) => (
            <li key={horario.id}>
              {editando === horario.id ? (
                <form
                  className="bloque bloque--edicion"
                  onSubmit={(evento) => {
                    evento.preventDefault();
                    void aplicar(
                      {
                        tipo: 'editar',
                        idHorario: horario.id,
                        diaSemana: Number(edicion.diaSemana),
                        horaInicio: edicion.horaInicio,
                        horaFin: edicion.horaFin,
                      },
                      false,
                    );
                  }}
                >
                  <Selector
                    etiqueta="Día"
                    value={edicion.diaSemana}
                    onChange={(evento) =>
                      setEdicion((actual) => ({ ...actual, diaSemana: evento.target.value }))
                    }
                  >
                    {DIAS_SEMANA.map((dia) => (
                      <option key={dia.valor} value={dia.valor}>
                        {dia.nombre}
                      </option>
                    ))}
                  </Selector>
                  <div className="fila-horas">
                    <Campo
                      etiqueta="Hora de inicio"
                      type="time"
                      value={edicion.horaInicio}
                      required
                      onChange={(evento) =>
                        setEdicion((actual) => ({ ...actual, horaInicio: evento.target.value }))
                      }
                    />
                    <Campo
                      etiqueta="Hora de fin"
                      type="time"
                      value={edicion.horaFin}
                      required
                      onChange={(evento) =>
                        setEdicion((actual) => ({ ...actual, horaFin: evento.target.value }))
                      }
                    />
                  </div>
                  <div className="bloque__acciones">
                    <button type="submit" className="boton boton--principal" disabled={guardando}>
                      Guardar cambios
                    </button>
                    <button
                      type="button"
                      className="boton boton--secundario"
                      onClick={() => setEditando(null)}
                    >
                      Cancelar la edición
                    </button>
                  </div>
                </form>
              ) : (
                <div className="bloque">
                  <p className="bloque__texto">
                    <strong>{nombreDia(horario.diaSemana)}</strong> de{' '}
                    {horaCorta(horario.horaInicio)} a {horaCorta(horario.horaFin)}
                  </p>
                  <div className="bloque__acciones">
                    <button
                      type="button"
                      className="boton boton--secundario"
                      onClick={() => {
                        setEditando(horario.id);
                        setEdicion({
                          diaSemana: String(horario.diaSemana),
                          horaInicio: horaCorta(horario.horaInicio),
                          horaFin: horaCorta(horario.horaFin),
                        });
                      }}
                    >
                      Editar el bloque del {nombreDia(horario.diaSemana).toLowerCase()}
                    </button>
                    <button
                      type="button"
                      className="boton boton--peligro"
                      disabled={guardando}
                      onClick={() => void aplicar({ tipo: 'eliminar', idHorario: horario.id }, false)}
                    >
                      Eliminar el bloque del {nombreDia(horario.diaSemana).toLowerCase()}
                    </button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      ) : null}

      {pendiente && afectadas.length > 0 ? (
        <Dialogo
          titulo="Este cambio afecta reservas confirmadas"
          descripcion={
            pendiente.tipo === 'eliminar'
              ? 'Si eliminas el bloque, las reservas siguientes se mantienen confirmadas fuera de la agenda publicada.'
              : 'Si aplicas la edición, las reservas siguientes se mantienen confirmadas aunque queden fuera del nuevo horario.'
          }
          textoConfirmar={
            pendiente.tipo === 'eliminar' ? 'Eliminar de todos modos' : 'Aplicar de todos modos'
          }
          ocupado={guardando}
          onCancelar={() => {
            setPendiente(null);
            setAfectadas([]);
          }}
          onConfirmar={() => void aplicar(pendiente, true)}
        >
          <ul className="lista-afectadas">
            {afectadas.map((reserva) => (
              <li key={reserva.idReserva}>
                Reserva #{reserva.idReserva}: {fechaYHora(reserva.fechaHoraInicio)} —{' '}
                {fechaYHora(reserva.fechaHoraFin)}
              </li>
            ))}
          </ul>
        </Dialogo>
      ) : null}
    </>
  );
}

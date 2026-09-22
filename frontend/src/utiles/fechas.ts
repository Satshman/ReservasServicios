const DIAS_ISO = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
] as const;

/** Día ISO del backend: 1 = lunes … 7 = domingo. */
export function nombreDia(diaSemana: number): string {
  return DIAS_ISO[diaSemana - 1] ?? `Día ${diaSemana}`;
}

export const DIAS_SEMANA: ReadonlyArray<{ valor: number; nombre: string }> = DIAS_ISO.map(
  (nombre, indice) => ({ valor: indice + 1, nombre }),
);

/** Fecha `YYYY-MM-DD` en la zona local del navegador (sin desplazamiento por UTC). */
export function fechaLocalISO(fecha: Date): string {
  const y = fecha.getFullYear();
  const m = `${fecha.getMonth() + 1}`.padStart(2, '0');
  const d = `${fecha.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function sumarDias(fecha: Date, dias: number): Date {
  const copia = new Date(fecha);
  copia.setDate(copia.getDate() + dias);
  return copia;
}

export function hoyISO(): string {
  return fechaLocalISO(new Date());
}

/** Rango por defecto de la disponibilidad: hoy y los seis días siguientes. */
export function rangoProximosDias(dias = 7): { desde: string; hasta: string } {
  const hoy = new Date();
  return { desde: fechaLocalISO(hoy), hasta: fechaLocalISO(sumarDias(hoy, dias - 1)) };
}

const REGIONES = ['es-CO', 'es'] as const;

function formatear(iso: string, opciones: Intl.DateTimeFormatOptions, zona?: string): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return iso;
  return new Intl.DateTimeFormat([...REGIONES], {
    ...opciones,
    ...(zona ? { timeZone: zona } : {}),
  }).format(fecha);
}

/**
 * Clave `YYYY-MM-DD` de un instante ISO, calculada en la zona del proveedor.
 * Se arma pieza a pieza con `formatToParts` para que el orden del resultado no
 * dependa del formato de ninguna configuración regional.
 */
export function diaDeTurno(iso: string, zona?: string): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return iso.slice(0, 10);
  const partes = new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(zona ? { timeZone: zona } : {}),
  }).formatToParts(fecha);
  const buscar = (tipo: Intl.DateTimeFormatPartTypes) =>
    partes.find((parte) => parte.type === tipo)?.value ?? '';
  const anio = buscar('year');
  const mes = buscar('month');
  const dia = buscar('day');
  return anio && mes && dia ? `${anio}-${mes}-${dia}` : iso.slice(0, 10);
}

export function fechaLarga(iso: string, zona?: string): string {
  return formatear(iso, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }, zona);
}

export function soloHora(iso: string, zona?: string): string {
  return formatear(iso, { hour: '2-digit', minute: '2-digit', hour12: false }, zona);
}

export function fechaYHora(iso: string, zona?: string): string {
  return formatear(
    iso,
    { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false },
    zona,
  );
}

/** `HH:mm:ss` → `HH:mm` para pintar bloques de agenda y alimentar `<input type="time">`. */
export function horaCorta(hora: string): string {
  return hora.length >= 5 ? hora.slice(0, 5) : hora;
}

export function esFuturo(iso: string): boolean {
  const fecha = new Date(iso);
  return !Number.isNaN(fecha.getTime()) && fecha.getTime() > Date.now();
}

export function duracionLegible(minutos: number): string {
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`;
}

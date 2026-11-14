/**
 * Utilidades para manejo determinista de fechas operativas.
 */

/**
 * Retorna la fecha operativa en formato estricto ISO YYYY-MM-DD
 * calculada en la zona horaria de negocio 'America/Lima' (UTC-5),
 * garantizando total independencia de la zona horaria del cliente/navegador.
 */
export function getOperatingDateLima(date: Date = new Date()): string {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });

  const parts = formatter.formatToParts(date);
  const year = parts.find((p) => p.type === 'year')?.value;
  const month = parts.find((p) => p.type === 'month')?.value;
  const day = parts.find((p) => p.type === 'day')?.value;

  if (!year || !month || !day) {
    return date.toISOString().substring(0, 10);
  }

  return `${year}-${month}-${day}`;
}

/**
 * Retorna los componentes de tiempo calculados en la zona horaria 'America/Lima' (UTC-5).
 */
export function getLimaTimeParts(date: Date = new Date()): {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
} {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Lima',
    hourCycle: 'h23',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
  });

  const parts = formatter.formatToParts(date);
  let year = date.getUTCFullYear();
  let month = date.getUTCMonth() + 1;
  let day = date.getUTCDate();
  let hour = 0;
  let minute = 0;
  let second = 0;

  for (const part of parts) {
    if (part.type === 'year') year = parseInt(part.value, 10);
    else if (part.type === 'month') month = parseInt(part.value, 10);
    else if (part.type === 'day') day = parseInt(part.value, 10);
    else if (part.type === 'hour') hour = parseInt(part.value, 10) % 24;
    else if (part.type === 'minute') minute = parseInt(part.value, 10);
    else if (part.type === 'second') second = parseInt(part.value, 10);
  }

  return { year, month, day, hour, minute, second };
}

/**
 * Calcula los milisegundos restantes hasta la próxima medianoche (00:00:00) en 'America/Lima'.
 */
export function getMsUntilNextMidnightLima(now: Date = new Date()): number {
  const { hour, minute, second } = getLimaTimeParts(now);
  const secondsToday = hour * 3600 + minute * 60 + second;
  const secondsUntilMidnight = 86400 - secondsToday;
  const ms = secondsUntilMidnight * 1000 - now.getMilliseconds();
  return Math.max(100, ms);
}

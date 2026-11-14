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

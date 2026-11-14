import { describe, expect, it } from 'vitest';
import { getOperatingDateLima } from './date.utils';

describe('date.utils - getOperatingDateLima', () => {
  it('debe calcular la fecha correcta en zona horaria America/Lima (UTC-5)', () => {
    // 2026-11-15 02:00:00 UTC corresponde a 2026-11-14 21:00:00 en Lima (UTC-5)
    const lateUtc = new Date('2026-11-15T02:00:00Z');
    expect(getOperatingDateLima(lateUtc)).toBe('2026-11-14');
  });

  it('debe cambiar de día al cruzar la medianoche en Lima', () => {
    // 2026-11-15 05:00:00 UTC corresponde exactamente a 2026-11-15 00:00:00 en Lima
    const midnightLima = new Date('2026-11-15T05:00:00Z');
    expect(getOperatingDateLima(midnightLima)).toBe('2026-11-15');
  });

  it('debe calcular correctamente fechas de mediodía en Lima', () => {
    const midday = new Date('2026-11-14T17:00:00Z'); // 12:00 en Lima
    expect(getOperatingDateLima(midday)).toBe('2026-11-14');
  });

  it('debe retornar formato YYYY-MM-DD con padding de dos dígitos', () => {
    const date = new Date('2026-03-05T15:00:00Z'); // 10:00 en Lima
    expect(getOperatingDateLima(date)).toBe('2026-03-05');
  });
});

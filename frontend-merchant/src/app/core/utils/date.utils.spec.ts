import { describe, expect, it } from 'vitest';
import { getLimaTimeParts, getMsUntilNextMidnightLima, getOperatingDateLima } from './date.utils';

describe('date.utils', () => {
  describe('getOperatingDateLima', () => {
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

  describe('getLimaTimeParts', () => {
    it('debe extraer partes de hora en America/Lima', () => {
      // 2026-11-15 03:30:15 UTC = 2026-11-14 22:30:15 Lima
      const date = new Date('2026-11-15T03:30:15Z');
      const parts = getLimaTimeParts(date);
      expect(parts.year).toBe(2026);
      expect(parts.month).toBe(11);
      expect(parts.day).toBe(14);
      expect(parts.hour).toBe(22);
      expect(parts.minute).toBe(30);
      expect(parts.second).toBe(15);
    });
  });

  describe('getMsUntilNextMidnightLima', () => {
    it('debe calcular los milisegundos exactos hasta la medianoche en Lima', () => {
      // 2026-11-15 04:59:50.000 UTC = 2026-11-14 23:59:50 Lima (a 10 segundos de medianoche)
      const date = new Date('2026-11-15T04:59:50.000Z');
      const ms = getMsUntilNextMidnightLima(date);
      expect(ms).toBe(10000);

      const target = new Date(date.getTime() + ms);
      expect(getOperatingDateLima(target)).toBe('2026-11-15');
    });
  });
});

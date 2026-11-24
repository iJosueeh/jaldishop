import { describe, expect, it } from 'vitest';
import { estimateServiceFee } from './pricing';

describe('estimateServiceFee', () => {
  it('returns 0 when subtotal or rate is 0 or missing', () => {
    expect(estimateServiceFee(0, 5)).toBe(0);
    expect(estimateServiceFee(100, 0)).toBe(0);
    expect(estimateServiceFee(100, null)).toBe(0);
  });

  it('calculates percentage fee rounded to 2 decimals without cap', () => {
    expect(estimateServiceFee(100, 2.5)).toBe(2.5);
    expect(estimateServiceFee(33.33, 5)).toBe(1.67);
  });

  it('applies serviceFeeCap when raw fee exceeds the cap', () => {
    expect(estimateServiceFee(500, 5, 10)).toBe(10);
    expect(estimateServiceFee(100, 5, 10)).toBe(5);
  });
});

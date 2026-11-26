import { describe, it, expect } from 'vitest';
import {
  calculateTaxBreakdown,
  estimateServiceFee,
  formatServiceFeeLabel,
} from './pricing';

describe('estimateServiceFee', () => {
  it('returns 0 when subtotal or rate is 0 or null', () => {
    expect(estimateServiceFee(0, 5)).toBe(0);
    expect(estimateServiceFee(100, 0)).toBe(0);
    expect(estimateServiceFee(100, null)).toBe(0);
  });

  it('computes percentage fee rounded to 2 decimals', () => {
    expect(estimateServiceFee(100, 2.5)).toBe(2.5);
    expect(estimateServiceFee(33.33, 5)).toBe(1.67);
  });

  it('applies serviceFeeCap when raw fee exceeds the cap', () => {
    expect(estimateServiceFee(500, 5, 10)).toBe(10);
    expect(estimateServiceFee(100, 5, 10)).toBe(5);
  });
});

describe('calculateTaxBreakdown', () => {
  it('computes 18% included IGV breakdown from gross amount', () => {
    const result = calculateTaxBreakdown(118, 0.18);
    expect(result).toEqual({
      baseAmount: 100,
      includedTaxAmount: 18,
      effectiveRate: 0.18,
    });
  });

  it('handles zero-rated/exempt stores cleanly', () => {
    const result = calculateTaxBreakdown(50, 0);
    expect(result).toEqual({
      baseAmount: 50,
      includedTaxAmount: 0,
      effectiveRate: 0,
    });
  });

  it('returns zeros when grossAmount is 0 or negative', () => {
    expect(calculateTaxBreakdown(0, 0.18)).toEqual({
      baseAmount: 0,
      includedTaxAmount: 0,
      effectiveRate: 0,
    });
  });
});

describe('formatServiceFeeLabel', () => {
  it('formats plain label when rate is 0 or missing', () => {
    expect(formatServiceFeeLabel(0, null)).toBe('Tarifa de servicio');
  });

  it('formats rate and cap when configured', () => {
    expect(formatServiceFeeLabel(3.5, 10, 'PEN')).toContain('3.5%');
    expect(formatServiceFeeLabel(3.5, 10, 'PEN')).toContain('máx.');
  });
});

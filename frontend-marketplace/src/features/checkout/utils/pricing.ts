import { formatCurrency } from '@/shared/utils/formatters';

/**
 * Estimates the customer service fee from a product subtotal, percentage rate (0..100),
 * and optional maximum cap in currency units.
 * Used for pre-reservation UI previews in /carrito and /checkout.
 * Once a CheckoutResponse exists, UI always uses server-computed pricing.serviceFeeAmount.
 */
export function estimateServiceFee(
  subtotal: number,
  serviceFeeRate?: number | null,
  serviceFeeCap?: number | null
): number {
  if (!Number.isFinite(subtotal) || subtotal <= 0) return 0;
  if (!Number.isFinite(serviceFeeRate) || !serviceFeeRate || serviceFeeRate <= 0) return 0;
  const rawFee = Math.round(subtotal * serviceFeeRate) / 100;
  if (serviceFeeCap !== null && serviceFeeCap !== undefined && Number.isFinite(serviceFeeCap) && serviceFeeCap >= 0) {
    return Math.min(rawFee, Math.round(serviceFeeCap * 100) / 100);
  }
  return rawFee;
}

export interface TaxBreakdown {
  baseAmount: number;
  includedTaxAmount: number;
  effectiveRate: number;
}

/**
 * Computes the included IGV breakdown from a tax-inclusive gross amount.
 * Prices in Peru/JaldiShop already include IGV, so includedTaxAmount is informational
 * and must NOT be added on top of the gross total.
 */
export function calculateTaxBreakdown(
  grossAmount: number,
  taxRate?: number | null
): TaxBreakdown {
  if (!Number.isFinite(grossAmount) || grossAmount <= 0) {
    return { baseAmount: 0, includedTaxAmount: 0, effectiveRate: 0 };
  }

  const rawRate = taxRate === undefined || taxRate === null ? 0.18 : taxRate;
  if (!Number.isFinite(rawRate) || rawRate <= 0) {
    const rounded = Math.round(grossAmount * 100) / 100;
    return { baseAmount: rounded, includedTaxAmount: 0, effectiveRate: 0 };
  }

  const effectiveRate = rawRate > 1 ? rawRate / 100 : rawRate;
  const baseAmount = Math.round((grossAmount / (1 + effectiveRate)) * 100) / 100;
  const includedTaxAmount = Math.round((grossAmount - baseAmount) * 100) / 100;

  return { baseAmount, includedTaxAmount, effectiveRate };
}

export function formatServiceFeeLabel(
  rate?: number | null,
  cap?: number | null,
  currency = 'PEN'
): string {
  if (!rate || !Number.isFinite(rate) || rate <= 0) {
    return 'Tarifa de servicio';
  }
  if (cap !== null && cap !== undefined && Number.isFinite(cap) && cap > 0) {
    return `Tarifa de servicio (${rate}%, máx. ${formatCurrency(cap, currency)})`;
  }
  return `Tarifa de servicio (${rate}%)`;
}

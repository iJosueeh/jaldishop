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

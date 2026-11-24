'use client';

import React, { useEffect, useState } from 'react';
import { Tag, Clock, ShoppingBag, AlertTriangle } from 'lucide-react';
import { CartItem, PublicStoreSettings } from '@/features/storefront/types/storefront.types';
import { CheckoutFulfillmentType, CheckoutResponse } from '../types/checkout.types';
import { estimateServiceFee } from '../utils/pricing';
import { formatCurrency } from '@/shared/utils/formatters';

export interface CheckoutOrderSummaryProps {
  storeName: string;
  items: CartItem[];
  currency: string;
  subtotal: number;
  deliveryFee: number;
  serviceFeeRate?: number;
  serviceFeeCap?: number | null;
  fulfillmentType: CheckoutFulfillmentType;
  storeSettings?: PublicStoreSettings | null;
  checkoutResponse: CheckoutResponse | null;
  discountCode: string;
  onDiscountCodeChange: (code: string) => void;
  onApplyDiscount?: () => void;
  onReservationExpired?: () => void;
}

function getRemainingSeconds(expiresAt?: string): number {
  if (!expiresAt) return 0;
  const diff = Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
  return Math.max(0, diff);
}

export function CheckoutOrderSummary({
  storeName,
  items,
  currency,
  subtotal,
  deliveryFee,
  serviceFeeRate = 0,
  serviceFeeCap = null,
  fulfillmentType,
  storeSettings,
  checkoutResponse,
  discountCode,
  onDiscountCodeChange,
  onApplyDiscount,
  onReservationExpired,
}: CheckoutOrderSummaryProps) {
  const [secondsLeft, setSecondsLeft] = useState<number>(() =>
    getRemainingSeconds(checkoutResponse?.reservationExpiresAt)
  );

  useEffect(() => {
    if (!checkoutResponse?.reservationExpiresAt) {
      setSecondsLeft(0);
      return;
    }

    const updateTimer = () => {
      const remaining = getRemainingSeconds(checkoutResponse.reservationExpiresAt);
      setSecondsLeft(remaining);
      if (remaining <= 0 && onReservationExpired) {
        onReservationExpired();
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [checkoutResponse?.reservationExpiresAt, onReservationExpired]);

  const pricing = checkoutResponse?.pricing;
  const effectiveSubtotal = pricing ? Number(pricing.productsSubtotalAmount) : subtotal;
  const effectiveDiscount = pricing ? Number(pricing.discountAmount) : 0;
  const effectiveDeliveryFee = pricing
    ? Number(pricing.deliveryFeeAmount)
    : fulfillmentType === 'DELIVERY'
    ? deliveryFee
    : 0;
  const effectiveServiceFee = pricing
    ? Number(pricing.serviceFeeAmount ?? 0)
    : estimateServiceFee(effectiveSubtotal, serviceFeeRate, serviceFeeCap);
  const effectiveTax = pricing ? Number(pricing.includedTaxAmount) : 0;
  const effectiveTotal = pricing
    ? Number(pricing.totalAmount)
    : Math.max(0, effectiveSubtotal - effectiveDiscount + effectiveDeliveryFee + effectiveServiceFee);
  const effectiveCurrency = pricing?.currency || currency || 'PEN';

  const minDeliveryAmount = storeSettings?.minDeliveryAmount ?? null;
  const isBelowMinDelivery =
    fulfillmentType === 'DELIVERY' &&
    minDeliveryAmount !== null &&
    minDeliveryAmount > 0 &&
    subtotal < minDeliveryAmount;

  const mins = Math.floor(secondsLeft / 60);
  const secs = secondsLeft % 60;
  const formattedTimer = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;

  return (
    <aside
      aria-label="Resumen del pedido"
      className="rounded-[12px] border border-[#1e2c31] bg-[#061a1c] p-5 sm:p-6 text-white space-y-5 lg:sticky lg:top-24"
    >
      <div className="flex items-center justify-between border-b border-[#1e2c31] pb-4">
        <div>
          <span className="text-[11px] font-semibold uppercase tracking-[0.06em] text-[#36f4a4] block">
            Resumen del pedido
          </span>
          <h2 className="font-display text-lg font-medium text-white mt-0.5">{storeName}</h2>
        </div>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-[#072720] border border-[#133b32] px-3 py-1 text-xs font-medium text-[#36f4a4]">
          <ShoppingBag className="w-3.5 h-3.5" aria-hidden="true" />
          <span>{items.reduce((sum, i) => sum + i.quantity, 0)} ítems</span>
        </span>
      </div>

      {/* Live Reservation Countdown */}
      {checkoutResponse?.reservationExpiresAt && (
        <div
          role="status"
          className={`rounded-[12px] border p-3.5 flex items-center justify-between gap-3 text-xs ${
            secondsLeft <= 60
              ? 'bg-rose-950/40 border-rose-500/40 text-rose-200'
              : 'bg-[#072720] border-[#133b32] text-[#36f4a4]'
          }`}
        >
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 shrink-0" aria-hidden="true" />
            <span className="font-medium">
              {secondsLeft > 0 ? 'Reserva de horario activa' : 'La reserva ha expirado'}
            </span>
          </div>
          <span className="font-mono font-bold text-sm tabular-nums">{formattedTimer}</span>
        </div>
      )}

      {/* Minimum delivery warning */}
      {isBelowMinDelivery && minDeliveryAmount !== null && (
        <div
          role="alert"
          className="rounded-[12px] border border-amber-500/40 bg-amber-950/40 p-3.5 text-xs text-amber-200 flex items-start gap-2.5"
        >
          <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" aria-hidden="true" />
          <span>
            El monto mínimo para delivery en {storeName} es{' '}
            <strong className="font-mono">{formatCurrency(minDeliveryAmount, effectiveCurrency)}</strong>. Te faltan{' '}
            <strong className="font-mono">{formatCurrency(minDeliveryAmount - subtotal, effectiveCurrency)}</strong>.
          </span>
        </div>
      )}

      {/* Itemized list */}
      <ul className="divide-y divide-[#1e2c31] max-h-60 overflow-y-auto pr-1">
        {items.map(({ product, quantity }) => (
          <li key={product.variantId || product.id} className="py-2.5 first:pt-0 last:pb-0 flex items-center justify-between gap-3 text-xs">
            <div className="min-w-0">
              <p className="font-medium text-white truncate">
                <span className="font-mono text-[#36f4a4] mr-1.5">{quantity}×</span>
                {product.name}
              </p>
              <p className="text-[11px] text-[#99b3ad]">
                {formatCurrency(product.price, product.priceCurrency || effectiveCurrency)} c/u
              </p>
            </div>
            <span className="font-mono font-semibold text-white shrink-0">
              {formatCurrency(product.price * quantity, product.priceCurrency || effectiveCurrency)}
            </span>
          </li>
        ))}
      </ul>

      {/* Discount code */}
      <div className="pt-3 border-t border-[#1e2c31] space-y-2">
        <label htmlFor="checkout-discount-code" className="text-xs font-medium text-[#99b3ad] flex items-center gap-1.5">
          <Tag className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
          <span>Código de descuento (opcional)</span>
        </label>
        <div className="flex items-center gap-2">
          <input
            id="checkout-discount-code"
            type="text"
            maxLength={80}
            value={discountCode}
            onChange={(e) => onDiscountCodeChange(e.target.value.toUpperCase())}
            placeholder="EJ. BIENVENIDO10"
            className="flex-1 px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none font-mono uppercase"
          />
          {onApplyDiscount && (
            <button
              type="button"
              onClick={onApplyDiscount}
              disabled={!discountCode.trim()}
              className="rounded-full border border-white/30 px-4 py-2 text-xs font-semibold text-white hover:bg-white/10 disabled:opacity-40 cursor-pointer shrink-0"
            >
              Aplicar
            </button>
          )}
        </div>
      </div>

      {/* Pricing breakdown */}
      <div className="pt-3 border-t border-[#1e2c31] space-y-2 text-xs">
        <div className="flex items-center justify-between text-[#99b3ad]">
          <span>Subtotal productos</span>
          <span className="font-mono text-white">
            {formatCurrency(effectiveSubtotal, effectiveCurrency)}
          </span>
        </div>

        {effectiveDiscount > 0 && (
          <div className="flex items-center justify-between text-[#36f4a4]">
            <span>Descuento {pricing?.discountCode ? `(${pricing.discountCode})` : ''}</span>
            <span className="font-mono">-{formatCurrency(effectiveDiscount, effectiveCurrency)}</span>
          </div>
        )}

        <div className="flex items-center justify-between text-[#99b3ad]">
          <span>Costo de envío</span>
          <span className="font-mono text-white">
            {fulfillmentType === 'DELIVERY'
              ? formatCurrency(effectiveDeliveryFee, effectiveCurrency)
              : 'Gratis'}
          </span>
        </div>

        <div className="flex items-center justify-between text-[#99b3ad]">
          <span>Tarifa de servicio</span>
          <span className="font-mono text-white">
            {formatCurrency(effectiveServiceFee, effectiveCurrency)}
          </span>
        </div>

        {effectiveTax > 0 && (
          <div className="flex items-center justify-between text-[#9dabad]">
            <span>IGV incluido</span>
            <span className="font-mono">{formatCurrency(effectiveTax, effectiveCurrency)}</span>
          </div>
        )}

        <div className="pt-3 border-t border-[#1e2c31] flex items-center justify-between text-base font-semibold">
          <span className="text-white">Total a pagar</span>
          <span className="font-display text-xl text-[#36f4a4] tabular-nums">
            {formatCurrency(effectiveTotal, effectiveCurrency)}
          </span>
        </div>
      </div>
    </aside>
  );
}

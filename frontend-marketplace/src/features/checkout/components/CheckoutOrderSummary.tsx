'use client';

import React, { useEffect, useRef, useState } from 'react';
import {
  Tag,
  Clock,
  ShoppingBag,
  AlertTriangle,
  Package,
  Receipt,
  Truck,
  ShieldCheck,
  Info,
  ChevronDown,
} from 'lucide-react';
import { CartItem, PublicStoreSettings } from '@/features/storefront/types/storefront.types';
import { CheckoutFulfillmentType, CheckoutResponse } from '../types/checkout.types';
import {
  calculateTaxBreakdown,
  estimateServiceFee,
  formatServiceFeeLabel,
} from '../utils/pricing';
import { formatCurrency } from '@/shared/utils/formatters';

export interface CheckoutOrderSummaryProps {
  storeName: string;
  items: CartItem[];
  currency: string;
  subtotal: number;
  deliveryFee: number;
  serviceFeeRate?: number;
  serviceFeeCap?: number | null;
  taxRate?: number | null;
  fulfillmentType: CheckoutFulfillmentType;
  storeSettings?: PublicStoreSettings | null;
  checkoutResponse: CheckoutResponse | null;
  discountCode: string;
  onDiscountCodeChange: (code: string) => void;
  onApplyDiscount?: () => void;
  onClearDiscount?: () => void;
  appliedPromo?: {
    code: string;
    discountAmount: number;
  } | null;
  promoError?: string | null;
  isValidatingPromo?: boolean;
  onReservationExpired?: () => void;
  embedded?: boolean;
}

function getRemainingSeconds(expiresAt?: string): number {
  if (!expiresAt) return 0;
  const diff = Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
  return Math.max(0, diff);
}

function OrderItemThumbnail({ imageUrl, name }: { imageUrl?: string; name: string }) {
  const [failed, setFailed] = useState(false);

  if (!imageUrl || failed) {
    return (
      <div className="h-10 w-10 shrink-0 rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] flex items-center justify-center text-[#78716c]">
        <Package className="w-4 h-4" aria-hidden="true" />
      </div>
    );
  }

  return (
    /* eslint-disable-next-line @next/next/no-img-element */
    <img
      src={imageUrl}
      alt={name}
      loading="lazy"
      onError={() => setFailed(true)}
      className="h-10 w-10 shrink-0 rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] object-cover"
    />
  );
}

export function CheckoutOrderSummary({
  storeName,
  items,
  currency,
  subtotal,
  deliveryFee,
  serviceFeeRate = 0,
  serviceFeeCap = null,
  taxRate = 0.18,
  fulfillmentType,
  storeSettings,
  checkoutResponse,
  discountCode,
  onDiscountCodeChange,
  onApplyDiscount,
  onClearDiscount,
  appliedPromo = null,
  promoError = null,
  isValidatingPromo = false,
  onReservationExpired,
  embedded = false,
}: CheckoutOrderSummaryProps) {
  const [secondsLeft, setSecondsLeft] = useState<number>(() =>
    getRemainingSeconds(checkoutResponse?.reservationExpiresAt)
  );
  const [isDiscountOpen, setIsDiscountOpen] = useState<boolean>(() =>
    Boolean(discountCode || checkoutResponse?.pricing?.discountCode)
  );
  const discountInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (discountCode || checkoutResponse?.pricing?.discountCode) {
      setIsDiscountOpen(true);
    }
  }, [discountCode, checkoutResponse?.pricing?.discountCode]);

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
  const effectiveDiscount = pricing
    ? Number(pricing.discountAmount)
    : appliedPromo
    ? Math.min(effectiveSubtotal, Number(appliedPromo.discountAmount || 0))
    : 0;
  const effectiveDiscountCode = pricing?.discountCode || appliedPromo?.code || null;
  const effectiveDeliveryFee = pricing
    ? Number(pricing.deliveryFeeAmount)
    : fulfillmentType === 'DELIVERY'
    ? deliveryFee
    : 0;
  const taxableSubtotal = Math.max(0, effectiveSubtotal - effectiveDiscount);
  const effectiveServiceFee = pricing
    ? Number(pricing.serviceFeeAmount ?? 0)
    : estimateServiceFee(taxableSubtotal, serviceFeeRate, serviceFeeCap);
  const preReservationTax = calculateTaxBreakdown(taxableSubtotal, taxRate);
  const effectiveTax = pricing
    ? Number(pricing.includedTaxAmount)
    : preReservationTax.includedTaxAmount;
  const rawTaxRate = pricing?.taxRate ?? preReservationTax.effectiveRate;
  const effectiveTaxRate = rawTaxRate > 1 ? rawTaxRate / 100 : rawTaxRate;
  const effectiveBaseAmount = Math.max(
    0,
    Math.round((taxableSubtotal - effectiveTax) * 100) / 100
  );
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
  const totalItemsCount = items.reduce((sum, i) => sum + i.quantity, 0);

  return (
    <aside
      aria-label="Resumen del pedido"
      className={
        embedded
          ? 'text-[#1c1917]'
          : 'rounded-[12px] border border-[#e7e0d6] bg-white text-[#1c1917] overflow-hidden lg:sticky lg:top-24'
      }
    >
      <div className="flex items-center justify-between gap-3 border-b border-[#e7e0d6] bg-[#f7f3ec]/60 px-5 sm:px-6 py-4">
        <div>
          <h2 className="font-display text-lg font-semibold text-[#1c1917]">Resumen del pedido</h2>
          <p className="text-xs text-[#57534e] mt-0.5">{storeName}</p>
        </div>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-white border border-[#e7e0d6] px-3 py-1 text-xs font-semibold text-[#005141] tabular-nums">
          <ShoppingBag className="w-3.5 h-3.5" aria-hidden="true" />
          <span>
            {totalItemsCount} {totalItemsCount === 1 ? 'ítem' : 'ítems'}
          </span>
        </span>
      </div>

      <div className="p-5 sm:p-6 space-y-4">
        {/* Live Reservation Countdown */}
        {checkoutResponse?.reservationExpiresAt && (
          <div
            role="status"
            className={`rounded-[10px] border p-3 flex items-center justify-between gap-3 text-xs ${
              secondsLeft <= 60
                ? 'bg-rose-50 border-rose-200 text-rose-800'
                : 'bg-[#f0fdfa] border-[#ccfbf1] text-[#005141]'
            }`}
          >
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 shrink-0" aria-hidden="true" />
              <span className="font-semibold">
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
            className="rounded-[10px] border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900 flex items-start gap-2.5"
          >
            <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
            <span>
              El monto mínimo para delivery en {storeName} es{' '}
              <strong className="font-mono tabular-nums">{formatCurrency(minDeliveryAmount, effectiveCurrency)}</strong>. Te faltan{' '}
              <strong className="font-mono tabular-nums">{formatCurrency(minDeliveryAmount - subtotal, effectiveCurrency)}</strong>.
            </span>
          </div>
        )}

        {/* Itemized list with thumbnails */}
        <ul className="divide-y divide-[#e7e0d6] max-h-60 overflow-y-auto pr-1">
          {items.map(({ product, quantity }) => (
            <li key={product.variantId || product.id} className="py-2.5 first:pt-0 last:pb-0 flex items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-3 min-w-0">
                <OrderItemThumbnail imageUrl={product.imageUrl} name={product.name} />
                <div className="min-w-0">
                  <p className="font-medium text-[#1c1917] truncate">
                    <span className="font-mono font-semibold text-[#005141] mr-1.5 tabular-nums">{quantity}×</span>
                    {product.name}
                  </p>
                  <p className="text-[11px] text-[#57534e] tabular-nums">
                    {formatCurrency(product.price, product.priceCurrency || effectiveCurrency)} c/u
                  </p>
                </div>
              </div>
              <span className="font-mono font-semibold text-[#1c1917] shrink-0 tabular-nums">
                {formatCurrency(product.price * quantity, product.priceCurrency || effectiveCurrency)}
              </span>
            </li>
          ))}
        </ul>

        {/* Progressive Disclosure Discount Code */}
        <div className="pt-3 border-t border-[#e7e0d6]">
          <button
            type="button"
            aria-expanded={isDiscountOpen}
            aria-controls="checkout-discount-panel"
            onClick={() => {
              setIsDiscountOpen((prev) => {
                const next = !prev;
                if (next) {
                  requestAnimationFrame(() => discountInputRef.current?.focus());
                }
                return next;
              });
            }}
            className="w-full flex items-center justify-between text-xs font-medium text-[#005141] hover:text-[#003d31] cursor-pointer py-0.5"
          >
            <span className="inline-flex items-center gap-1.5">
              <Tag className="w-3.5 h-3.5" aria-hidden="true" />
              <span>¿Tienes un código de descuento?</span>
            </span>
            <ChevronDown
              className={`w-3.5 h-3.5 transition-transform duration-200 ${isDiscountOpen ? 'rotate-180' : ''}`}
              aria-hidden="true"
            />
          </button>

          {isDiscountOpen && (
            <div id="checkout-discount-panel" className="mt-2.5 space-y-2">
              {appliedPromo ? (
                <div className="flex items-center justify-between gap-2 rounded-[8px] border border-[#005141]/30 bg-[#f0fdfa] px-3 py-2 text-xs">
                  <div className="min-w-0">
                    <span className="font-mono font-bold text-[#005141]">{appliedPromo.code}</span>
                    <span className="ml-2 text-[#005141] tabular-nums">
                      (-{formatCurrency(effectiveDiscount, effectiveCurrency)})
                    </span>
                  </div>
                  {onClearDiscount && (
                    <button
                      type="button"
                      onClick={onClearDiscount}
                      className="text-xs font-semibold text-[#57534e] hover:text-[#1c1917] underline cursor-pointer shrink-0"
                    >
                      Quitar
                    </button>
                  )}
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <input
                    ref={discountInputRef}
                    id="checkout-discount-code"
                    type="text"
                    maxLength={40}
                    value={discountCode}
                    onChange={(e) => onDiscountCodeChange(e.target.value.toUpperCase())}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && onApplyDiscount && discountCode.trim() && !isValidatingPromo) {
                        e.preventDefault();
                        onApplyDiscount();
                      }
                    }}
                    placeholder="EJ. BIENVENIDO10"
                    aria-label="Código de descuento"
                    className="flex-1 px-3 py-2 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none font-mono uppercase transition-colors"
                  />
                  {onApplyDiscount && (
                    <button
                      type="button"
                      onClick={onApplyDiscount}
                      disabled={!discountCode.trim() || isValidatingPromo}
                      className="rounded-full border border-[#e7e0d6] bg-white px-4 py-2 text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] disabled:opacity-40 cursor-pointer shrink-0 transition-colors"
                    >
                      {isValidatingPromo ? 'Validando...' : 'Aplicar'}
                    </button>
                  )}
                </div>
              )}

              {promoError && (
                <p role="alert" aria-live="polite" className="text-[11px] font-medium text-rose-700">
                  {promoError}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Pricing breakdown with semantic icons */}
        <div className="pt-3 border-t border-[#e7e0d6] space-y-2 text-xs">
          <div className="flex items-center justify-between text-[#57534e]">
            <span className="inline-flex items-center gap-1.5">
              <Receipt className="w-3.5 h-3.5 text-[#78716c]" aria-hidden="true" />
              <span>Subtotal productos</span>
            </span>
            <span className="font-mono font-medium text-[#1c1917] tabular-nums">
              {formatCurrency(effectiveSubtotal, effectiveCurrency)}
            </span>
          </div>

          {effectiveDiscount > 0 && (
            <div className="flex items-center justify-between text-[#005141] font-medium">
              <span className="inline-flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5" aria-hidden="true" />
                <span>Descuento {effectiveDiscountCode ? `(${effectiveDiscountCode})` : ''}</span>
              </span>
              <span className="font-mono tabular-nums">-{formatCurrency(effectiveDiscount, effectiveCurrency)}</span>
            </div>
          )}

          <div className="flex items-center justify-between text-[#57534e]">
            <span className="inline-flex items-center gap-1.5">
              <Truck className="w-3.5 h-3.5 text-[#78716c]" aria-hidden="true" />
              <span>Costo de envío</span>
            </span>
            <span className={`font-mono font-medium tabular-nums ${fulfillmentType === 'DELIVERY' ? 'text-[#1c1917]' : 'text-[#005141]'}`}>
              {fulfillmentType === 'DELIVERY'
                ? formatCurrency(effectiveDeliveryFee, effectiveCurrency)
                : 'Gratis'}
            </span>
          </div>

          <div className="flex items-center justify-between gap-2 text-[#57534e]">
            <span className="inline-flex items-center gap-1.5 min-w-0">
              <ShieldCheck className="w-3.5 h-3.5 text-[#78716c] shrink-0" aria-hidden="true" />
              <span className="truncate">
                {formatServiceFeeLabel(
                  pricing?.serviceFeeRate ?? serviceFeeRate,
                  serviceFeeCap,
                  effectiveCurrency
                )}
              </span>
              <span
                title="Cubre procesamiento seguro de pagos y soporte de plataforma"
                aria-label="Cubre procesamiento seguro de pagos y soporte de plataforma"
                className="inline-flex text-[#78716c] hover:text-[#1c1917] cursor-help shrink-0"
              >
                <Info className="w-3 h-3" aria-hidden="true" />
              </span>
            </span>
            <span className="font-mono font-medium text-[#1c1917] tabular-nums shrink-0">
              {formatCurrency(effectiveServiceFee, effectiveCurrency)}
            </span>
          </div>

          <div className="pt-2 border-t border-[#e7e0d6]/70 space-y-1.5 text-[11px] text-[#57534e]">
            {effectiveTax > 0 ? (
              <>
                <div className="flex items-center justify-between">
                  <span>Operación gravada (Base)</span>
                  <span className="font-mono tabular-nums">
                    {formatCurrency(effectiveBaseAmount, effectiveCurrency)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span>
                    IGV incluido ({Math.round((effectiveTaxRate > 0 ? effectiveTaxRate : 0.18) * 100)}%
                    {pricing ? '' : ' est.'})
                  </span>
                  <span className="font-mono tabular-nums">
                    {formatCurrency(effectiveTax, effectiveCurrency)}
                  </span>
                </div>
              </>
            ) : (
              <div className="flex items-center justify-between">
                <span>IGV (Exonerado 0%)</span>
                <span className="font-mono tabular-nums">
                  {formatCurrency(0, effectiveCurrency)}
                </span>
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-[#e7e0d6] flex items-center justify-between text-base font-semibold">
            <div>
              <span className="text-[#1c1917] block">Total a pagar</span>
              <span className="text-[11px] font-medium text-[#005141]">
                {pricing
                  ? 'Validado por servidor'
                  : 'Estimado sujeto a validación de servidor'}
              </span>
            </div>
            <span className="font-display text-xl font-bold text-[#005141] tabular-nums">
              {formatCurrency(effectiveTotal, effectiveCurrency)}
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
}

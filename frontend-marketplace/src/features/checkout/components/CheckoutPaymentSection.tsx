'use client';

import React from 'react';
import {
  CreditCard,
  Lock,
  MessageCircle,
  AlertCircle,
  Loader2,
  Smartphone,
  Landmark,
  Clock,
  ExternalLink,
  RefreshCw,
} from 'lucide-react';
import { PublicStoreSettings } from '@/features/storefront/types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

export interface ActivePaymentSession {
  paymentId: string;
  initPoint: string;
  popupBlocked: boolean;
}

export interface CheckoutPaymentSectionProps {
  storeSettings?: PublicStoreSettings | null;
  totalAmount: number;
  currency: string;
  canProceed: boolean;
  disabledHint?: string | null;
  isSubmittingCheckout: boolean;
  isInitiatingPayment: boolean;
  isRedirectingToMercadoPago: boolean;
  paymentError: string | null;
  hasActiveReservation: boolean;
  activePaymentSession?: ActivePaymentSession | null;
  onCheckPaymentStatus?: () => void;
  onPayWithMercadoPago: () => void;
  onProceedCashOnDelivery?: () => void;
  embedded?: boolean;
}

const PAYMENT_METHODS = [
  { label: 'Tarjetas', Icon: CreditCard },
  { label: 'Yape', Icon: Smartphone },
  { label: 'Transferencia', Icon: Landmark },
] as const;

export function CheckoutPaymentSection({
  storeSettings,
  totalAmount,
  currency,
  canProceed,
  disabledHint,
  isSubmittingCheckout,
  isInitiatingPayment,
  isRedirectingToMercadoPago,
  paymentError,
  hasActiveReservation,
  activePaymentSession = null,
  onCheckPaymentStatus,
  onPayWithMercadoPago,
  onProceedCashOnDelivery,
  embedded = false,
}: CheckoutPaymentSectionProps) {
  const onlinePaymentsEnabled = storeSettings?.onlinePaymentsEnabled ?? true;
  const cashOnDeliveryEnabled = storeSettings?.cashOnDeliveryEnabled ?? false;
  const acceptsPosOnDelivery = storeSettings?.acceptsPosOnDelivery ?? false;

  const isBusy = isSubmittingCheckout || isInitiatingPayment || isRedirectingToMercadoPago;

  return (
    <section
      id="checkout-payment-section"
      aria-label="Método de pago"
      className={
        embedded
          ? 'p-5 sm:p-6 bg-[#faf7f2]/55 text-[#1c1917] space-y-4'
          : 'bg-white rounded-[12px] p-5 sm:p-7 border border-[#e7e0d6] text-[#1c1917] space-y-5'
      }
    >
      <div className="flex items-center justify-between gap-3">
        <h3 className="font-display text-base font-semibold text-[#1c1917] flex items-center gap-2">
          <CreditCard className="w-4 h-4 text-[#005141]" aria-hidden="true" />
          <span>Pago seguro</span>
        </h3>
        <span className="inline-flex items-center gap-1 rounded-full bg-white border border-[#e7e0d6] px-2.5 py-0.5 text-[11px] font-medium text-[#57534e]">
          <Lock className="w-3 h-3 text-[#005141]" aria-hidden="true" />
          <span>TLS</span>
        </span>
      </div>

      {paymentError && (
        <div
          role="alert"
          className="rounded-[10px] border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-800 flex items-start gap-2.5"
        >
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
          <span>{paymentError}</span>
        </div>
      )}

      {isRedirectingToMercadoPago && (
        <div
          role="status"
          className="rounded-[10px] border border-[#ccfbf1] bg-[#f0fdfa] p-3.5 text-xs text-[#005141] flex items-center gap-3"
        >
          <Loader2 className="w-4 h-4 animate-spin shrink-0" aria-hidden="true" />
          <p className="font-semibold">Conectando con Mercado Pago...</p>
        </div>
      )}

      {activePaymentSession && (
        <div
          role="status"
          className={`rounded-[10px] border p-3.5 text-xs space-y-2.5 ${
            activePaymentSession.popupBlocked
              ? 'border-amber-200 bg-amber-50 text-amber-900'
              : 'border-[#ccfbf1] bg-[#f0fdfa] text-[#005141]'
          }`}
        >
          <div className="flex items-start gap-2">
            <ExternalLink className="w-4 h-4 shrink-0 mt-0.5" aria-hidden="true" />
            <div className="space-y-0.5">
              <p className="font-semibold">
                {activePaymentSession.popupBlocked
                  ? 'Tu navegador bloqueó la ventana emergente'
                  : 'Mercado Pago abierto en una nueva pestaña'}
              </p>
              <p className="text-[11px] opacity-90">
                {activePaymentSession.popupBlocked
                  ? 'Haz clic en el enlace debajo para abrir la pasarela de pago sin salir de JaldiShop.'
                  : 'Completa el pago en esa pestaña. Esta página verificará la confirmación automáticamente.'}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <a
              href={activePaymentSession.initPoint}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-full bg-[#005141] hover:bg-[#003d31] text-white font-semibold text-[11px] px-3.5 py-1.5 transition-colors"
            >
              <span>Abrir Mercado Pago</span>
              <ExternalLink className="w-3 h-3" aria-hidden="true" />
            </a>
            {onCheckPaymentStatus && (
              <button
                type="button"
                onClick={onCheckPaymentStatus}
                className="inline-flex items-center gap-1.5 rounded-full border border-[#e7e0d6] bg-white hover:bg-[#f7f3ec] text-[#1c1917] font-semibold text-[11px] px-3 py-1.5 cursor-pointer transition-colors"
              >
                <RefreshCw className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                <span>Verificar estado</span>
              </button>
            )}
          </div>
        </div>
      )}

      {onlinePaymentsEnabled ? (
        <div className="space-y-3">
          <div className="rounded-[10px] border border-[#e7e0d6] bg-white p-3.5 space-y-2.5">
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-semibold text-[#1c1917]">Mercado Pago</span>
              <span className="rounded-full bg-[#f0fdfa] border border-[#ccfbf1] px-2.5 py-0.5 text-[10px] font-semibold text-[#005141]">
                Recomendado
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-1.5">
              {PAYMENT_METHODS.map(({ label, Icon }) => (
                <span
                  key={label}
                  className="inline-flex items-center gap-1.5 rounded-full border border-[#e7e0d6] bg-[#faf7f2] px-2.5 py-1 text-[11px] font-medium text-[#57534e]"
                >
                  <Icon className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                  <span>{label}</span>
                </span>
              ))}
            </div>
          </div>

          {!canProceed && disabledHint && !isBusy && (
            <p
              role="status"
              className="rounded-[8px] border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900 flex items-center gap-2"
            >
              <AlertCircle className="w-3.5 h-3.5 text-amber-600 shrink-0" aria-hidden="true" />
              <span>{disabledHint}</span>
            </p>
          )}

          <button
            type="button"
            onClick={onPayWithMercadoPago}
            disabled={!canProceed || isBusy}
            className="w-full min-h-[48px] inline-flex items-center justify-center gap-2 rounded-full bg-[#005141] hover:bg-[#003d31] text-white font-semibold text-sm px-6 py-3 transition-all active:scale-[0.99] disabled:bg-[#e7e0d6] disabled:text-[#78716c] disabled:cursor-not-allowed cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]/40 tabular-nums"
          >
            {isBusy ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                <span>
                  {isRedirectingToMercadoPago
                    ? 'Conectando con Mercado Pago...'
                    : isInitiatingPayment
                    ? 'Generando enlace de pago...'
                    : 'Reservando horario...'}
                </span>
              </>
            ) : (
              <>
                <Lock className="w-4 h-4 shrink-0" aria-hidden="true" />
                <span>
                  {hasActiveReservation
                    ? `Ir a pagar con Mercado Pago (${formatCurrency(totalAmount, currency)})`
                    : `Reservar y pagar con Mercado Pago (${formatCurrency(totalAmount, currency)})`}
                </span>
              </>
            )}
          </button>
        </div>
      ) : (
        <div className="rounded-[10px] border border-[#e7e0d6] bg-white p-3.5 text-xs text-[#57534e]">
          Los pagos en línea están deshabilitados temporalmente por esta tienda.
        </div>
      )}

      {cashOnDeliveryEnabled && onProceedCashOnDelivery && (
        <div className="pt-2 border-t border-[#e7e0d6] space-y-2">
          <button
            type="button"
            onClick={onProceedCashOnDelivery}
            disabled={!canProceed || isBusy}
            className="w-full min-h-[42px] inline-flex items-center justify-center gap-2 rounded-full border border-[#e7e0d6] bg-white hover:bg-[#f7f3ec] text-[#1c1917] font-semibold text-xs px-5 py-2.5 transition-colors disabled:opacity-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]/30"
          >
            <MessageCircle className="w-4 h-4 text-[#005141]" aria-hidden="true" />
            <span>Coordinar pago contra entrega</span>
            {acceptsPosOnDelivery && (
              <span className="rounded-full bg-[#f0fdfa] border border-[#ccfbf1] px-2 py-0.5 text-[10px] font-medium text-[#005141]">
                POS
              </span>
            )}
          </button>
        </div>
      )}

      <div className="flex items-center gap-1.5 text-[11px] text-[#57534e]">
        <Clock className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
        <span>Cupo reservado 10 min al iniciar el pago</span>
      </div>
    </section>
  );
}

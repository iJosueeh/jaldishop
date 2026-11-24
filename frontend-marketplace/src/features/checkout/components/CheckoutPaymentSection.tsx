'use client';

import React from 'react';
import { CreditCard, ShieldCheck, Lock, MessageCircle, AlertCircle, Loader2, Banknote } from 'lucide-react';
import { PublicStoreSettings } from '@/features/storefront/types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

export interface CheckoutPaymentSectionProps {
  storeSettings?: PublicStoreSettings | null;
  totalAmount: number;
  currency: string;
  canProceed: boolean;
  isSubmittingCheckout: boolean;
  isInitiatingPayment: boolean;
  isRedirectingToMercadoPago: boolean;
  paymentError: string | null;
  hasActiveReservation: boolean;
  onPayWithMercadoPago: () => void;
  onProceedCashOnDelivery?: () => void;
}

export function CheckoutPaymentSection({
  storeSettings,
  totalAmount,
  currency,
  canProceed,
  isSubmittingCheckout,
  isInitiatingPayment,
  isRedirectingToMercadoPago,
  paymentError,
  hasActiveReservation,
  onPayWithMercadoPago,
  onProceedCashOnDelivery,
}: CheckoutPaymentSectionProps) {
  const onlinePaymentsEnabled = storeSettings?.onlinePaymentsEnabled ?? true;
  const cashOnDeliveryEnabled = storeSettings?.cashOnDeliveryEnabled ?? false;
  const acceptsPosOnDelivery = storeSettings?.acceptsPosOnDelivery ?? false;

  const isBusy = isSubmittingCheckout || isInitiatingPayment || isRedirectingToMercadoPago;

  return (
    <section
      aria-label="Método de pago"
      className="bg-[#061a1c] rounded-[12px] p-5 sm:p-7 border border-[#1e2c31] text-white space-y-5"
    >
      <div className="flex items-center justify-between pb-4 border-b border-[#1e2c31]">
        <div>
          <span className="text-[11px] font-semibold uppercase tracking-[0.06em] text-[#36f4a4] block">
            Paso final
          </span>
          <h3 className="text-base sm:text-lg font-medium text-white mt-0.5 flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-[#36f4a4]" aria-hidden="true" />
            <span>Pago seguro</span>
          </h3>
        </div>
        <span className="inline-flex items-center gap-1.5 text-xs text-[#99b3ad]">
          <Lock className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
          <span>Cifrado TLS</span>
        </span>
      </div>

      {paymentError && (
        <div
          role="alert"
          className="rounded-[12px] border border-rose-500/40 bg-rose-950/40 p-4 text-xs text-rose-200 flex items-start gap-2.5"
        >
          <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" aria-hidden="true" />
          <span>{paymentError}</span>
        </div>
      )}

      {isRedirectingToMercadoPago && (
        <div
          role="status"
          className="rounded-[12px] border border-[#36f4a4]/40 bg-[#072720] p-4 text-sm text-[#36f4a4] flex items-center gap-3"
        >
          <Loader2 className="w-5 h-5 animate-spin shrink-0" aria-hidden="true" />
          <div>
            <p className="font-semibold">Conectando con Mercado Pago...</p>
            <p className="text-xs text-[#99b3ad]">
              Te estamos redirigiendo a la pasarela oficial para completar tu pago de forma segura.
            </p>
          </div>
        </div>
      )}

      {onlinePaymentsEnabled ? (
        <div className="space-y-4">
          <div className="rounded-[12px] border border-[#133b32] bg-[#072720] p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-white">Mercado Pago</span>
              <span className="rounded-full bg-[#061a1c] border border-[#1e2c31] px-2.5 py-0.5 text-[11px] font-medium text-[#36f4a4]">
                Recomendado
              </span>
            </div>
            <p className="text-xs text-[#99b3ad] leading-relaxed">
              Paga con tarjeta de crédito, débito, Yape o transferencia mediante el link oficial de Mercado Pago.
            </p>
          </div>

          <button
            type="button"
            onClick={onPayWithMercadoPago}
            disabled={!canProceed || isBusy}
            className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-white hover:bg-stone-100 text-[#02090a] font-semibold text-sm px-6 py-3.5 transition-all active:scale-[0.98] disabled:bg-[#333333] disabled:text-[#a1a1aa] disabled:cursor-not-allowed cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
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
              <span>
                {hasActiveReservation
                  ? `Ir a pagar con Mercado Pago (${formatCurrency(totalAmount, currency)})`
                  : `Reservar y pagar con Mercado Pago (${formatCurrency(totalAmount, currency)})`}
              </span>
            )}
          </button>
        </div>
      ) : (
        <div className="rounded-[12px] border border-[#1e2c31] bg-[#02090a] p-4 text-xs text-[#99b3ad]">
          Los pagos en línea están deshabilitados temporalmente por esta tienda.
        </div>
      )}

      {cashOnDeliveryEnabled && onProceedCashOnDelivery && (
        <div className="pt-3 border-t border-[#1e2c31] space-y-3">
          <div className="flex items-center justify-between text-xs text-[#99b3ad]">
            <span className="flex items-center gap-1.5 font-medium text-white">
              <Banknote className="w-4 h-4 text-[#36f4a4]" aria-hidden="true" />
              <span>Pago contra entrega / Coordinación directa</span>
            </span>
            {acceptsPosOnDelivery && (
              <span className="rounded-full border border-[#1e2c31] px-2.5 py-0.5 text-[10px] text-[#36f4a4]">
                Acepta POS en entrega
              </span>
            )}
          </div>
          <button
            type="button"
            onClick={onProceedCashOnDelivery}
            disabled={!canProceed || isBusy}
            className="w-full inline-flex items-center justify-center gap-2 rounded-full border border-white/30 hover:bg-white/10 text-white font-semibold text-xs px-5 py-2.5 transition-colors disabled:opacity-40 cursor-pointer"
          >
            <MessageCircle className="w-4 h-4 text-[#36f4a4]" aria-hidden="true" />
            <span>Coordinar pago contra entrega</span>
          </button>
        </div>
      )}

      <div className="pt-2 flex items-center gap-2 text-[11px] text-[#9dabad]">
        <ShieldCheck className="w-4 h-4 text-[#36f4a4] shrink-0" aria-hidden="true" />
        <span>
          Tu reserva de capacidad se mantiene por 10 minutos mientras completas el pago en Mercado Pago.
        </span>
      </div>
    </section>
  );
}

'use client';

import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  CreditCard,
  ArrowRight,
  ShieldCheck,
  Clock,
  Lock,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';

export type PaymentMethod = 'yape' | 'plin' | 'transferencia';

export interface PaymentHubModalProps {
  isOpen: boolean;
  onClose: () => void;
  onProceedToWhatsApp?: (paymentDetails: {
    method: PaymentMethod;
    receiptFile: string | null;
    total: number;
  }) => void;
  onPayWithMercadoPago?: () => Promise<void> | void;
  isPaying?: boolean;
  total: number;
  storeName?: string;
  slotTime?: string;
  countdownSeconds?: number;
}

export function PaymentHubModal({
  isOpen,
  onClose,
  onProceedToWhatsApp,
  onPayWithMercadoPago,
  isPaying = false,
  total,
  storeName = 'Panadería Don Pepe',
  slotTime = '10:00 - 11:00 AM',
  countdownSeconds = 600,
}: PaymentHubModalProps) {
  const isMercadoPago = Boolean(onPayWithMercadoPago);

  const minutes = Math.floor(countdownSeconds / 60);
  const seconds = countdownSeconds % 60;
  const formattedCountdown = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/70 backdrop-blur-xs"
            aria-hidden="true"
          />

          {/* Modal Content */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="relative w-full max-w-lg bg-white rounded-[12px] border border-stone-200/90 shadow-2xl overflow-hidden z-10 flex flex-col max-h-[92vh]"
          >
            {/* Modal Header */}
            <div className="p-5 sm:p-6 pb-4 border-b border-stone-200 flex items-center justify-between bg-white sticky top-0 z-10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-[8px] bg-[#005141] text-white flex items-center justify-center shadow-xs">
                  <CreditCard className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-stone-900 tracking-tight">
                    {isMercadoPago ? 'Pagar y Confirmar Pedido' : 'Coordinar Pedido'}
                  </h3>
                  <p className="text-xs text-stone-600">
                    {isMercadoPago
                      ? 'Pago seguro procesado por Mercado Pago'
                      : 'Solicitud directa de coordinación con el comercio'}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-500 hover:text-stone-900 cursor-pointer transition-colors"
                aria-label="Cerrar modal de pago"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Hold Guarantee & Amount Banner */}
            <div className="bg-[#faf7f2] px-6 py-3.5 border-b border-stone-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] font-bold text-[#57534e] uppercase tracking-wider block">
                  {isMercadoPago ? 'Total a Pagar' : 'Total Estimado'}
                </span>
                <span className="text-xl sm:text-2xl font-black text-[#005141] font-mono leading-none">
                  {formatCurrency(total)}
                </span>
              </div>

              {/* Countdown Pill */}
              <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-stone-200 shadow-2xs">
                <Clock className="w-3.5 h-3.5 text-[#ea580c] animate-pulse" />
                <div className="text-right">
                  <div className="text-[9px] font-extrabold uppercase text-[#ea580c]">10 min de reserva</div>
                  <div className="font-mono font-black text-xs text-[#1c1917]">{formattedCountdown} restantes</div>
                </div>
              </div>
            </div>

            {/* Scrollable Body */}
            <div className="p-5 sm:p-6 overflow-y-auto space-y-5">
              {isMercadoPago ? (
                /* Real Mercado Pago flow - Only legitimate gateway presentation, no mock details */
                <div className="p-4 rounded-2xl bg-[#faf7f2] border border-stone-200/90 space-y-3.5">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-black text-[#005141] bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-200">
                        Mercado Pago Checkout
                      </span>
                      <span className="text-xs text-stone-600 font-medium">Pasarela oficial y segura</span>
                    </div>
                    <ShieldCheck className="w-4 h-4 text-[#005141]" />
                  </div>

                  <div className="space-y-3 text-xs text-stone-600 leading-relaxed">
                    <p>
                      Serás redirigido a la pasarela segura de <strong>Mercado Pago</strong> para completar tu pago con cualquiera de los métodos habilitados:
                    </p>
                    <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 font-medium text-stone-700">
                      <li className="flex items-center gap-2 p-2 rounded-xl bg-white border border-stone-200">
                        <span className="w-2 h-2 rounded-full bg-emerald-500" />
                        <span>Tarjetas Débito / Crédito</span>
                      </li>
                      <li className="flex items-center gap-2 p-2 rounded-xl bg-white border border-stone-200">
                        <span className="w-2 h-2 rounded-full bg-[#742284]" />
                        <span>Yape con código de aprobación</span>
                      </li>
                      <li className="flex items-center gap-2 p-2 rounded-xl bg-white border border-stone-200">
                        <span className="w-2 h-2 rounded-full bg-[#00a4e4]" />
                        <span>PagoEfectivo / Banca móvil</span>
                      </li>
                      <li className="flex items-center gap-2 p-2 rounded-xl bg-white border border-stone-200">
                        <span className="w-2 h-2 rounded-full bg-stone-500" />
                        <span>Dinero en cuenta Mercado Pago</span>
                      </li>
                    </ul>
                  </div>
                </div>
              ) : (
                /* Fallback coordination mode - No unverified banking data presented as real */
                <div className="p-4 rounded-2xl bg-[#faf7f2] border border-stone-200/90 space-y-3.5">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200">
                    <span className="text-xs font-black text-[#005141] bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-200">
                      Coordinación directa
                    </span>
                    <ShieldCheck className="w-4 h-4 text-[#005141]" />
                  </div>
                  <p className="text-xs text-stone-600 leading-relaxed">
                    Coordina directamente los detalles de pago y entrega con <strong>{storeName}</strong> vía WhatsApp. Los datos bancarios y comprobantes se validarán durante la comunicación con el negocio.
                  </p>
                </div>
              )}

              {/* Slot Protection Guarantee */}
              <div className="p-3 bg-emerald-50/70 rounded-[8px] border border-emerald-200/80 flex items-start gap-2.5 text-xs text-[#005141]">
                <Lock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
                <div className="leading-relaxed">
                  Tu turno para la franja <strong className="font-bold text-[#005141]">{slotTime}</strong> está apartado.{' '}
                  {isMercadoPago
                    ? 'Al pulsar en pagar, serás redirigido a la pasarela segura de Mercado Pago.'
                    : 'Al pulsar en coordinar, se redactará el mensaje oficial para WhatsApp.'}
                </div>
              </div>
            </div>

            {/* Modal Sticky Footer CTA */}
            <div className="p-5 border-t border-stone-200 bg-white sticky bottom-0 z-10 flex flex-col gap-2">
              <div className="flex flex-col sm:flex-row items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  size="md"
                  onClick={onClose}
                  disabled={isPaying}
                  className="w-full sm:w-auto text-xs font-bold rounded-full border-stone-300 hover:bg-stone-50"
                >
                  Volver al carrito
                </Button>

                <Button
                  type="button"
                  variant="terracotta"
                  size="md"
                  onClick={() => {
                    if (onPayWithMercadoPago) {
                      onPayWithMercadoPago();
                    } else if (onProceedToWhatsApp) {
                      onProceedToWhatsApp({
                        method: 'yape',
                        receiptFile: null,
                        total,
                      });
                    }
                  }}
                  isLoading={isPaying}
                  disabled={isPaying}
                  className="w-full sm:flex-1 py-3.5 rounded-full font-black text-xs shadow-md shadow-[#ea580c]/25 cursor-pointer border-0 bg-[#ea580c] hover:bg-[#c2410c] flex items-center justify-center gap-2 active:scale-95 transition-all"
                >
                  <span>{isMercadoPago ? 'Pagar con Mercado Pago' : 'Coordinar Pedido por WhatsApp'}</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </div>

              {isMercadoPago && onProceedToWhatsApp && (
                <div className="text-center pt-1">
                  <button
                    type="button"
                    onClick={() =>
                      onProceedToWhatsApp({
                        method: 'yape',
                        receiptFile: null,
                        total,
                      })
                    }
                    disabled={isPaying}
                    className="text-[11px] font-semibold text-stone-500 hover:text-stone-700 underline cursor-pointer disabled:opacity-50"
                  >
                    ¿Problemas con la pasarela? Solicitar coordinación por WhatsApp
                  </button>
                </div>
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

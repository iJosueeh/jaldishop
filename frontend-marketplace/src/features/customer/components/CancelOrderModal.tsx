'use client';

import React, { useState, useEffect } from 'react';
import { AlertCircle, AlertTriangle, Loader2, X } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';

interface CancelOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderNumber: string;
  storeName: string;
  totalAmount: number;
  currency?: string;
  onConfirm: (reason?: string) => Promise<void>;
  isCancelling: boolean;
  error: string | null;
  onClearError: () => void;
}

const MAX_REASON_LENGTH = 255;

export const CancelOrderModal: React.FC<CancelOrderModalProps> = ({
  isOpen,
  onClose,
  orderNumber,
  storeName,
  totalAmount,
  currency = 'PEN',
  onConfirm,
  isCancelling,
  error,
  onClearError,
}) => {
  const [reason, setReason] = useState('');

  // Reset internal state when modal opens or closes
  useEffect(() => {
    if (isOpen) {
      setReason('');
      onClearError();
    }
  }, [isOpen, onClearError]);

  if (!isOpen) return null;

  const isOverLimit = reason.length > MAX_REASON_LENGTH;
  const isSubmitDisabled = isCancelling || isOverLimit;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitDisabled) return;

    try {
      await onConfirm(reason.trim() || undefined);
      // Close only on success (onConfirm should throw on failure)
      onClose();
    } catch {
      // Error is caught and surfaced via error prop
    }
  };

  const handleClose = () => {
    if (isCancelling) return;
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="cancel-order-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs"
    >
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-2xl max-w-md w-full overflow-hidden animate-in fade-in duration-150">
        {/* Header */}
        <div className="p-6 pb-4 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h2 id="cancel-order-modal-title" className="text-base font-bold text-stone-900">
                ¿Deseas cancelar este pedido?
              </h2>
              <p className="text-xs text-stone-500 mt-0.5">
                Pedido #{orderNumber} &bull; {storeName}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={isCancelling}
            aria-label="Cerrar modal"
            className="p-1 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-100 transition-colors disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSubmit}>
          <div className="px-6 py-2 space-y-4">
            {/* Warning Details */}
            <div className="p-3.5 rounded-xl bg-amber-50/70 border border-amber-200/80 text-xs text-amber-900 space-y-1.5">
              <p className="font-semibold">
                Importe de compra: {currency === 'PEN' ? 'S/' : currency} {Number(totalAmount).toFixed(2)}
              </p>
              <p className="text-amber-800 leading-relaxed">
                El pedido será cancelado y la tienda liberará los artículos reservados. Si realizaste el pago mediante Mercado Pago, las operaciones de compensación y reembolso se tramitarán según los términos del comercio y la plataforma.
              </p>
            </div>

            {/* Error Banner */}
            {error && (
              <div
                role="alert"
                className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-start gap-2"
              >
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="font-semibold">No se pudo cancelar el pedido</p>
                  <p className="mt-0.5">{error}</p>
                </div>
              </div>
            )}

            {/* Optional Reason Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="cancel-reason-input"
                className="block text-xs font-semibold text-stone-700"
              >
                Motivo de la cancelación <span className="text-stone-400 font-normal">(opcional)</span>
              </label>
              <textarea
                id="cancel-reason-input"
                rows={3}
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value);
                  if (error) onClearError();
                }}
                disabled={isCancelling}
                maxLength={MAX_REASON_LENGTH + 5} // allow slightly more to show overflow validation
                placeholder="Indica brevemente el motivo para información de la tienda..."
                className={`w-full text-xs rounded-xl border p-3 text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-[#005141] transition-all resize-none ${
                  isOverLimit
                    ? 'border-rose-400 bg-rose-50/40 text-rose-950 focus:ring-rose-500'
                    : 'border-stone-200 bg-white'
                }`}
              />
              <div className="flex items-center justify-between text-[11px]">
                <span className={isOverLimit ? 'text-rose-600 font-medium' : 'text-stone-400'}>
                  {isOverLimit ? 'Supera el límite permitido' : 'Máximo 255 caracteres'}
                </span>
                <span
                  className={`font-mono ${
                    isOverLimit ? 'text-rose-600 font-bold' : 'text-stone-400'
                  }`}
                >
                  {reason.length}/{MAX_REASON_LENGTH}
                </span>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="p-6 pt-4 border-t border-stone-100 flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={handleClose}
              disabled={isCancelling}
              className="rounded-full px-5 text-xs font-semibold"
            >
              Mantener pedido
            </Button>
            <button
              type="submit"
              disabled={isSubmitDisabled}
              className="inline-flex items-center justify-center gap-1.5 px-5 py-2.5 rounded-full text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 active:bg-rose-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-xs"
            >
              {isCancelling ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Cancelando pedido...</span>
                </>
              ) : (
                <span>Confirmar cancelación</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

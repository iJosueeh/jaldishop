'use client';

import React from 'react';
import { CustomerAddress } from '../types/customer.types';
import { Button } from '@/shared/components/ui/Button';
import { AlertTriangle, Trash2, X } from 'lucide-react';

interface DeleteAddressModalProps {
  isOpen: boolean;
  onClose: () => void;
  address: CustomerAddress | null;
  onConfirm: (id: string) => Promise<void>;
  isDeleting: boolean;
  hasMultipleAddresses: boolean;
}

export function DeleteAddressModal({
  isOpen,
  onClose,
  address,
  onConfirm,
  isDeleting,
  hasMultipleAddresses,
}: DeleteAddressModalProps) {
  if (!isOpen || !address) return null;

  const handleDelete = async () => {
    try {
      await onConfirm(address.id);
      onClose();
    } catch {
      // Handled in parent/hook
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-address-title"
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
              <h2 id="delete-address-title" className="text-base font-bold text-stone-900">
                ¿Eliminar esta dirección?
              </h2>
              <p className="text-xs text-stone-500 mt-0.5">
                Esta acción no se puede deshacer.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar modal"
            className="p-1 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Details */}
        <div className="px-6 py-2 space-y-3">
          <div className="p-3.5 rounded-xl bg-stone-50 border border-stone-200/80 text-xs text-stone-700 space-y-1">
            <p className="font-bold text-stone-900">{address.alias}</p>
            <p className="text-stone-600 truncate">{address.addressLine}</p>
            <p className="text-stone-500">
              {address.district}, {address.province}, {address.department}
            </p>
          </div>

          {address.isDefault && hasMultipleAddresses && (
            <p className="text-xs text-amber-800 bg-amber-50 border border-amber-200/80 p-3 rounded-xl leading-relaxed">
              Esta es tu dirección predeterminada. Al eliminarla, otra de tus direcciones guardadas pasará a ser la predeterminada automáticamente.
            </p>
          )}

          <p className="text-xs text-stone-500 leading-relaxed">
            Los pedidos realizados anteriormente no se verán afectados, ya que conservan su propia dirección histórica de entrega.
          </p>
        </div>

        {/* Footer Actions */}
        <div className="p-6 pt-4 border-t border-stone-100 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            size="md"
            onClick={onClose}
            disabled={isDeleting}
            className="rounded-full px-5 text-xs font-semibold"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="danger"
            size="md"
            isLoading={isDeleting}
            disabled={isDeleting}
            onClick={handleDelete}
            className="rounded-full px-5 text-xs font-bold"
          >
            <Trash2 className="w-4 h-4 mr-1.5" />
            <span>Eliminar dirección</span>
          </Button>
        </div>
      </div>
    </div>
  );
}

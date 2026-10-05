'use client';

import React, { useEffect } from 'react';
import { X, ShoppingBag } from 'lucide-react';
import { CartItem, ProductItem } from '../../types/storefront.types';
import { CartSummarySidebar } from './CartSummarySidebar';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartItem[];
  storeName: string;
  subtotal: number;
  onChangeQuantity: (product: ProductItem, delta: number) => void;
  onCheckout?: () => void;
}

export function CartDrawer({
  isOpen,
  onClose,
  cart,
  storeName,
  subtotal,
  onChangeQuantity,
  onCheckout,
}: CartDrawerProps) {
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  // Cerrar con Escape y bloquear scroll del body mientras está abierto
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleKeyDown);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden" role="dialog" aria-modal="true" aria-label="Carrito de compras">
      {/* Backdrop con blur artesanal suave */}
      <div
        className="fixed inset-0 bg-stone-900/60 backdrop-blur-xs transition-opacity duration-300"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="fixed inset-y-0 right-0 flex max-w-full pl-10">
        <div className="w-screen max-w-md transform transition-all duration-300 ease-in-out bg-white shadow-2xl flex flex-col">
          {/* Header del Drawer */}
          <div className="flex items-center justify-between px-5 sm:px-6 py-4 border-b border-stone-200/80 bg-stone-50/70">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-emerald-50 text-[#005141] flex items-center justify-center">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <div>
                <h2 className="font-display font-bold text-base text-stone-900 leading-tight">
                  Tu pedido en {storeName}
                </h2>
                <p className="text-xs text-stone-500 font-medium">
                  {totalItems === 0
                    ? 'Carrito vacío'
                    : `${totalItems} ${totalItems === 1 ? 'producto' : 'productos'}`}
                </p>
              </div>
            </div>

            <button
              onClick={onClose}
              className="p-2 rounded-xl text-stone-400 hover:text-stone-700 hover:bg-stone-200/60 transition-colors"
              aria-label="Cerrar carrito"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Contenido con CartSummarySidebar adaptable */}
          <div className="flex-1 overflow-y-auto p-5 sm:p-6">
            <CartSummarySidebar
              cart={cart}
              storeName={storeName}
              subtotal={subtotal}
              onChangeQuantity={onChangeQuantity}
              onCheckout={onCheckout ? () => {
                onClose();
                onCheckout?.();
              } : undefined}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

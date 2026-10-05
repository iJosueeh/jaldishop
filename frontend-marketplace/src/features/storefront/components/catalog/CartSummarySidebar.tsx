'use client';

import React from 'react';
import { ShoppingBag, Plus, Minus, MessageCircle } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { CartItem, ProductItem } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

interface CartSummarySidebarProps {
  cart: CartItem[];
  storeName: string;
  subtotal: number;
  onChangeQuantity: (product: ProductItem, delta: number) => void;
  onClearCart?: () => void;
  onCheckout?: () => void;
  embedded?: boolean;
  pending?: boolean;
  currency?: string;
}

export function CartSummarySidebar({
  cart,
  storeName,
  subtotal,
  onChangeQuantity,
  onCheckout,
  embedded = false,
  pending = false,
  currency = 'PEN',
}: CartSummarySidebarProps) {
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <aside className={embedded ? 'space-y-4' : 'rounded-3xl border border-stone-200/90 bg-white p-5 sm:p-6 shadow-sm space-y-4'}>
      <div className="flex items-center justify-between pb-3 border-b border-stone-100">
        <h3 className="flex items-center gap-2 font-display text-base font-bold text-stone-900">
          <ShoppingBag className="h-5 w-5 text-[#005141]" />
          <span>Tu selección</span>
        </h3>
        {totalItems > 0 && (
          <span className="px-2.5 py-0.5 rounded-full bg-emerald-50 text-[#005141] font-bold text-xs">
            {totalItems} {totalItems === 1 ? 'ítem' : 'ítems'}
          </span>
        )}
      </div>

      {cart.length === 0 ? (
        <div className="py-6 text-center space-y-2">
          <p className="text-sm text-stone-500">
            Añade productos de {storeName} para armar tu pedido.
          </p>
          <span className="text-xs text-stone-400 block">
            Selecciona tus favoritos del menú
          </span>
        </div>
      ) : (
        <div className="space-y-3 divide-y divide-stone-100 max-h-80 overflow-y-auto pr-1">
          {cart.map(({ product, quantity, available }) => (
            <div key={product.variantId || product.id} className="pt-3 first:pt-0 space-y-1.5">
              <div className="flex justify-between items-start gap-2">
                <p className="text-sm font-bold text-stone-800 line-clamp-1">
                  {product.name}
                </p>
                <span className="text-xs font-mono font-bold text-stone-700 shrink-0">
                  {formatCurrency(product.price * quantity, product.priceCurrency || currency)}
                </span>
              </div>
              {available === false && <p className="text-xs text-red-700">Esta presentación ya no está disponible. Retírala de tu carrito.</p>}

              <div className="flex items-center justify-between gap-2">
                <span className="text-xs text-stone-500">
                  {formatCurrency(product.price, product.priceCurrency || currency)} c/u
                </span>
                <div className="flex items-center gap-2 bg-stone-50 rounded-xl p-1 border border-stone-200/60">
                  <button
                    aria-label={`Quitar una unidad de ${product.name}`}
                    disabled={pending}
                    onClick={() => onChangeQuantity(product, -1)}
                    className="p-1 rounded-lg hover:bg-stone-200 text-stone-700 transition-colors cursor-pointer"
                  >
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="text-xs font-bold w-4 text-center text-stone-800">
                    {quantity}
                  </span>
                  <button
                    aria-label={`Añadir una unidad de ${product.name}`}
                    disabled={pending || available === false}
                    onClick={() => onChangeQuantity(product, 1)}
                    className="p-1 rounded-lg hover:bg-stone-200 text-stone-700 transition-colors cursor-pointer"
                  >
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {cart.length > 0 && (
        <div className="pt-3 border-t border-stone-200/80 space-y-3">
          <div className="flex justify-between items-center text-sm font-bold text-stone-900">
            <span>Subtotal estimado</span>
            <span className="font-display font-black text-lg text-[#005141]">
              {formatCurrency(subtotal, currency)}
            </span>
          </div>

          <p className="text-xs text-stone-500 leading-relaxed">
            Tu selección todavía no crea un pedido ni reserva un horario.
          </p>

          <Button
            onClick={onCheckout}
            disabled={!onCheckout || pending || cart.some((item) => item.available === false)}
            className="w-full rounded-2xl bg-[#005141] hover:bg-[#00382d] text-white font-bold text-sm shadow-md shadow-[#005141]/20 py-3"
            leftIcon={<MessageCircle className="h-4 w-4 text-[#feae2c]" />}
          >
            {onCheckout ? 'Ver opciones de entrega' : 'Compra en línea no disponible'}
          </Button>
        </div>
      )}
    </aside>
  );
}

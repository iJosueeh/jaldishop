'use client';

import React from 'react';
import { ShoppingBag, Plus, Minus, Trash2, AlertTriangle, ArrowRight } from 'lucide-react';
import { CartItem, ProductItem } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

interface CartSummarySidebarProps {
  cart: CartItem[];
  storeName: string;
  subtotal: number;
  onChangeQuantity: (product: ProductItem, delta: number) => void;
  onRemoveItem?: (variantId: string) => void;
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
  onRemoveItem,
  onCheckout,
  embedded = false,
  pending = false,
  currency = 'PEN',
}: CartSummarySidebarProps) {
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  const hasUnavailableItems = cart.some((item) => item.available === false);

  return (
    <aside
      className={
        embedded
          ? 'space-y-4 text-[#1c1917]'
          : 'rounded-[12px] border border-[#e7e0d6] bg-white p-5 sm:p-6 text-[#1c1917] shadow-xs space-y-4'
      }
    >
      <div className="flex items-center justify-between pb-3 border-b border-[#e7e0d6]">
        <h3 className="flex items-center gap-2 font-display text-base font-semibold text-[#1c1917]">
          <ShoppingBag className="h-5 w-5 text-[#005141]" aria-hidden="true" />
          <span>Tu selección</span>
        </h3>
        {totalItems > 0 && (
          <span className="px-2.5 py-0.5 rounded-full bg-[#f0fdfa] text-[#005141] font-semibold text-xs border border-[#ccfbf1]">
            {totalItems} {totalItems === 1 ? 'ítem' : 'ítems'}
          </span>
        )}
      </div>

      {hasUnavailableItems && (
        <div
          role="alert"
          className="rounded-[12px] border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900 flex items-start gap-2.5"
        >
          <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
          <span>
            Hay productos sin stock o no disponibles en este momento. Retíralos para continuar al checkout.
          </span>
        </div>
      )}

      {cart.length === 0 ? (
        <div className="py-6 text-center space-y-2">
          <p className="text-sm text-[#57534e]">
            Añade productos de {storeName} para armar tu pedido.
          </p>
          <span className="text-xs text-[#78716c] block">
            Selecciona tus favoritos del catálogo
          </span>
        </div>
      ) : (
        <div className="space-y-3 divide-y divide-[#e7e0d6]/70 max-h-96 overflow-y-auto pr-1">
          {cart.map(({ product, quantity, available }) => {
            const itemKey = product.variantId || product.id;
            const isUnavailable = available === false;

            return (
              <div key={itemKey} className="pt-3 first:pt-0 flex items-start gap-3">
                {/* Product thumbnail */}
                <div className="h-14 w-14 shrink-0 rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] overflow-hidden flex items-center justify-center">
                  {product.imageUrl ? (
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <ShoppingBag className="h-4 w-4 text-[#78716c]" aria-hidden="true" />
                  )}
                </div>

                <div className="min-w-0 flex-1 space-y-1.5">
                  <div className="flex justify-between items-start gap-2">
                    <p className="text-sm font-semibold text-[#1c1917] line-clamp-2 leading-snug">
                      {product.name}
                    </p>
                    <span className="text-xs font-mono font-bold text-[#1c1917] shrink-0">
                      {formatCurrency(product.price * quantity, product.priceCurrency || currency)}
                    </span>
                  </div>

                  {isUnavailable && (
                    <p className="inline-flex items-center gap-1 rounded-full bg-rose-50 border border-rose-200 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700">
                      No disponible — retira este ítem
                    </p>
                  )}

                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs text-[#57534e]">
                      {formatCurrency(product.price, product.priceCurrency || currency)} c/u
                    </span>

                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-1 bg-[#faf7f2] rounded-full px-1.5 py-0.5 border border-[#e7e0d6]">
                        <button
                          type="button"
                          aria-label={`Quitar una unidad de ${product.name}`}
                          disabled={pending}
                          onClick={() => onChangeQuantity(product, -1)}
                          className="p-1.5 rounded-full hover:bg-[#e7e0d6]/60 text-[#1c1917] transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                        >
                          <Minus className="h-3 w-3" />
                        </button>
                        <span className="text-xs font-bold w-5 text-center text-[#1c1917] tabular-nums">
                          {quantity}
                        </span>
                        <button
                          type="button"
                          aria-label={`Añadir una unidad de ${product.name}`}
                          disabled={pending || isUnavailable}
                          onClick={() => onChangeQuantity(product, 1)}
                          className="p-1.5 rounded-full hover:bg-[#e7e0d6]/60 text-[#1c1917] transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                      </div>

                      {onRemoveItem && product.variantId && (
                        <button
                          type="button"
                          aria-label={`Eliminar ${product.name}`}
                          disabled={pending}
                          onClick={() => onRemoveItem(product.variantId!)}
                          className="p-1.5 rounded-full text-[#78716c] hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {cart.length > 0 && (
        <div className="pt-3 border-t border-[#e7e0d6] space-y-3">
          <div className="flex justify-between items-center text-sm font-medium text-[#57534e]">
            <span>Subtotal estimado</span>
            <span className="font-display font-bold text-lg text-[#005141] tabular-nums">
              {formatCurrency(subtotal, currency)}
            </span>
          </div>

          <p className="text-xs text-[#78716c] leading-relaxed">
            Tu selección todavía no crea un pedido ni reserva un horario.
          </p>

          <button
            type="button"
            onClick={onCheckout}
            disabled={!onCheckout || pending || hasUnavailableItems}
            className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-[#005141] hover:bg-[#003d31] text-white font-semibold text-sm px-6 py-3 transition-all active:scale-[0.98] disabled:bg-stone-200 disabled:text-stone-500 disabled:cursor-not-allowed cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
          >
            <span>{onCheckout ? 'Continuar al checkout' : 'Compra en línea no disponible'}</span>
            {onCheckout && <ArrowRight className="h-4 w-4" aria-hidden="true" />}
          </button>
        </div>
      )}
    </aside>
  );
}

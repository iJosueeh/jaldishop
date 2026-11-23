'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Plus, Minus, Clock, Heart, ShoppingBag, Layers, ShieldCheck } from 'lucide-react';
import { ProductItem, ProductVariantItem } from '../../types/storefront.types';
import { ProductVisual } from '../ProductVisual';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { cn } from '@/shared/utils/cn';

export interface ProductDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  product: ProductItem | null;
  onAdd: (product: ProductItem, quantity?: number) => void;
  currentQuantity?: number;
  isFavorite?: boolean;
  onToggleFavorite?: () => Promise<void> | void;
  isTogglingFavorite?: boolean;
  currency?: string;
  pending?: boolean;
}

export function ProductDetailModal({
  isOpen,
  onClose,
  product,
  onAdd,
  currentQuantity = 0,
  isFavorite = false,
  onToggleFavorite,
  isTogglingFavorite = false,
  currency = 'PEN',
  pending = false,
}: ProductDetailModalProps) {
  const [selectedVariantId, setSelectedVariantId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(1);

  const variants = useMemo(
    () => product?.variants?.filter((v) => v.status === 'ACTIVE') || [],
    [product?.variants]
  );

  // Reset state when modal opens or product changes
  useEffect(() => {
    if (isOpen && product) {
      if (variants.length > 0) {
        setSelectedVariantId(variants[0].id);
      } else {
        setSelectedVariantId('');
      }
      setQuantity(1);
    }
  }, [isOpen, product, variants]);

  // Handle escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !product) return null;

  const selectedVariant: ProductVariantItem | undefined = variants.find(
    (v) => v.id === selectedVariantId
  ) || (variants.length === 1 ? variants[0] : undefined);

  const unitPrice = selectedVariant
    ? selectedVariant.priceAmount
    : product.price;

  const totalPrice = unitPrice * quantity;

  const handleAddToCart = () => {
    const productToAdd: ProductItem = {
      ...product,
      variantId: selectedVariant?.id,
      price: unitPrice,
      name: selectedVariant
        ? `${product.name} — ${selectedVariant.presentationName}`
        : product.name,
    };
    onAdd(productToAdd, quantity);
    onClose();
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-black/60 backdrop-blur-xs"
          aria-hidden="true"
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ type: 'spring', damping: 28, stiffness: 320 }}
          role="dialog"
          aria-modal="true"
          aria-labelledby="product-modal-title"
          className="relative w-full max-w-lg bg-white rounded-[12px] border border-stone-200/90 shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh]"
        >
          {/* Header Actions */}
          <div className="absolute top-3 right-3 z-20 flex items-center gap-2">
            {onToggleFavorite && (
              <button
                type="button"
                onClick={onToggleFavorite}
                disabled={isTogglingFavorite}
                aria-label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                className={cn(
                  'w-9 h-9 rounded-full flex items-center justify-center transition-all shadow-md backdrop-blur-md cursor-pointer',
                  isFavorite
                    ? 'bg-rose-50 text-rose-600 border border-rose-200 hover:bg-rose-100'
                    : 'bg-white/90 text-stone-600 hover:text-stone-900 hover:bg-white border border-stone-200/80'
                )}
              >
                <Heart
                  className={cn(
                    'w-4 h-4 transition-transform active:scale-125',
                    isFavorite && 'fill-rose-500 text-rose-500'
                  )}
                />
              </button>
            )}

            <button
              type="button"
              onClick={onClose}
              aria-label="Cerrar detalle de producto"
              className="w-9 h-9 rounded-full bg-white/90 hover:bg-white text-stone-600 hover:text-stone-900 flex items-center justify-center shadow-md border border-stone-200/80 cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Scrollable Content */}
          <div className="overflow-y-auto flex-1">
            {/* Visual Cover */}
            <div className="relative aspect-16/10 bg-stone-100 overflow-hidden">
              <ProductVisual
                name={product.name}
                imageUrl={product.imageUrl}
                imageBg={product.imageBg || 'from-amber-700/20 to-stone-800/30'}
                iconText={product.iconText || product.name.substring(0, 2).toUpperCase()}
              />

              {/* Badges */}
              <div className="absolute bottom-3 left-3 flex flex-wrap gap-1.5 z-10">
                {product.category && product.category !== 'General' && (
                  <span className="px-2.5 py-1 rounded-full bg-stone-900/85 backdrop-blur-md text-[11px] font-bold uppercase tracking-wider text-white shadow-2xs">
                    {product.category}
                  </span>
                )}
                {product.prepTimeMinutes ? (
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-white/95 backdrop-blur-md text-[11px] font-bold text-stone-800 border border-stone-200/80 shadow-2xs">
                    <Clock className="w-3.5 h-3.5 text-[#ea580c]" />
                    <span>~{product.prepTimeMinutes} min</span>
                  </span>
                ) : null}
              </div>
            </div>

            {/* Product Info */}
            <div className="p-5 sm:p-6 space-y-5">
              <div>
                <h2
                  id="product-modal-title"
                  className="font-display text-xl sm:text-2xl font-bold text-stone-900 leading-tight"
                >
                  {product.name}
                </h2>
                <div className="mt-1 flex items-baseline gap-2">
                  <span className="font-mono text-2xl font-black text-[#005141]">
                    {formatCurrency(unitPrice, currency)}
                  </span>
                  {selectedVariant && (
                    <span className="text-xs font-medium text-stone-500">
                      por {selectedVariant.presentationName}
                    </span>
                  )}
                  {currentQuantity > 0 && (
                    <span className="text-xs font-semibold text-[#ea580c] bg-orange-50 px-2 py-0.5 rounded-full border border-orange-200">
                      {currentQuantity} en carrito
                    </span>
                  )}
                </div>
              </div>

              {/* Description */}
              {product.description ? (
                <div className="space-y-1.5">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-stone-500">
                    Descripción
                  </h3>
                  <p className="text-sm text-stone-700 leading-relaxed whitespace-pre-line">
                    {product.description}
                  </p>
                </div>
              ) : null}

              {/* Variant Selector */}
              {variants.length > 1 && (
                <div className="space-y-2 pt-2 border-t border-stone-100">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-stone-600">
                    <Layers className="w-3.5 h-3.5 text-[#ea580c]" />
                    <span>Selecciona una presentación</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {variants.map((v) => {
                      const isSelected = v.id === selectedVariantId;
                      return (
                        <button
                          key={v.id}
                          type="button"
                          onClick={() => setSelectedVariantId(v.id)}
                          className={cn(
                            'flex items-center justify-between p-3 rounded-xl border text-left transition-all cursor-pointer text-xs',
                            isSelected
                              ? 'border-[#005141] bg-emerald-50/60 ring-1 ring-[#005141] text-[#005141] font-bold'
                              : 'border-stone-200 bg-white hover:bg-stone-50 text-stone-700 font-medium'
                          )}
                        >
                          <span>{v.presentationName}</span>
                          <span className="font-mono font-bold">
                            {formatCurrency(v.priceAmount, currency)}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Guarantee Note */}
              <div className="p-3 rounded-xl bg-[#faf7f2] border border-stone-200/90 flex items-center gap-2.5 text-xs text-stone-600">
                <ShieldCheck className="w-4 h-4 text-[#005141] shrink-0" />
                <span>
                  Elaboración fresca y cupo protegido con control de capacidad en tiempo real.
                </span>
              </div>
            </div>
          </div>

          {/* Sticky Modal Footer: Quantity + Add CTA */}
          <div className="p-4 sm:p-5 border-t border-stone-200 bg-white sticky bottom-0 z-10 flex flex-col sm:flex-row items-center gap-3">
            {/* Quantity Controls */}
            <div className="flex items-center justify-between sm:justify-start w-full sm:w-auto bg-stone-100 rounded-full p-1 border border-stone-200">
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                disabled={quantity <= 1 || pending}
                aria-label="Disminuir cantidad"
                className="w-8 h-8 rounded-full bg-white text-stone-700 hover:text-stone-900 disabled:opacity-40 flex items-center justify-center shadow-xs cursor-pointer disabled:cursor-not-allowed transition-colors"
              >
                <Minus className="w-3.5 h-3.5" />
              </button>
              <span className="w-10 text-center font-mono font-bold text-sm text-stone-900">
                {quantity}
              </span>
              <button
                type="button"
                onClick={() => setQuantity((q) => q + 1)}
                disabled={pending}
                aria-label="Aumentar cantidad"
                className="w-8 h-8 rounded-full bg-white text-stone-700 hover:text-stone-900 disabled:opacity-40 flex items-center justify-center shadow-xs cursor-pointer disabled:cursor-not-allowed transition-colors"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Add to Cart Button */}
            <Button
              type="button"
              variant="terracotta"
              size="md"
              onClick={handleAddToCart}
              isLoading={pending}
              disabled={pending}
              className="w-full sm:flex-1 py-3 rounded-full font-bold text-xs bg-[#ea580c] hover:bg-[#c2410c] text-white shadow-md shadow-[#ea580c]/20 flex items-center justify-center gap-2 cursor-pointer active:scale-95 transition-all"
            >
              <ShoppingBag className="w-4 h-4" />
              <span>
                Añadir al carrito &bull; {formatCurrency(totalPrice, currency)}
              </span>
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

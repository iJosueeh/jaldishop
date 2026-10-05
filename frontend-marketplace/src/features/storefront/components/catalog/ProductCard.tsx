'use client';

import React, { useState } from 'react';
import { Plus, Clock, Layers } from 'lucide-react';
import { motion } from 'framer-motion';
import { Button } from '@/shared/components/ui/Button';
import { ProductVisual } from '../ProductVisual';
import { ProductItem } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

interface ProductCardProps {
  product: ProductItem;
  onAdd: (product: ProductItem) => void;
  currentQuantity?: number;
  pending?: boolean;
}

export function ProductCard({ product, onAdd, currentQuantity = 0, pending = false }: ProductCardProps) {
  const variants = product.variants?.filter((variant) => variant.status === 'ACTIVE') || [];
  const [variantId, setVariantId] = useState('');
  const selected = variants.length === 1 ? variants[0] : variants.find((variant) => variant.id === variantId);
  const hasPriceRange =
    product.minPrice != null &&
    product.maxPrice != null &&
    product.minPrice !== product.maxPrice;

  const displayPrice = hasPriceRange
    ? `${formatCurrency(product.minPrice!)} - ${formatCurrency(product.maxPrice!)}`
    : formatCurrency(product.price);

  return (
    <motion.article
      whileHover={{ y: -4 }}
      transition={{ duration: 0.2 }}
      className="group relative flex flex-col justify-between overflow-hidden rounded-3xl border border-stone-200/90 bg-white shadow-xs hover:shadow-xl hover:border-[#005141]/50 transition-all duration-300"
    >
      <div>
        {/* Cabecera visual con la fotografía o fallback */}
        <div className="relative">
          <ProductVisual
            name={product.name}
            imageUrl={product.imageUrl}
            imageBg={product.imageBg || 'from-amber-700/20 to-stone-800/30'}
            iconText={product.iconText || product.name.substring(0, 2).toUpperCase()}
          />

          {/* Insignia Flotante de Categoría o Estado */}
          <div className="absolute top-3 left-3 flex flex-wrap gap-1.5 z-10 pointer-events-none">
            {product.category && product.category !== 'General' && (
              <span className="px-2.5 py-1 rounded-lg bg-black/60 backdrop-blur-md text-[10px] font-bold uppercase tracking-wider text-white shadow-xs">
                {product.category}
              </span>
            )}
            {product.variants && product.variants.length > 1 && (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-[#005141]/85 backdrop-blur-md text-[10px] font-bold text-emerald-100 shadow-xs">
                <Layers className="w-3 h-3 text-[#feae2c]" />
                <span>Formatos</span>
              </span>
            )}
          </div>
        </div>

        {/* Contenido Textual */}
        <div className="p-4 sm:p-5 space-y-2">
          <h3 className="font-display text-base sm:text-lg font-bold text-stone-900 group-hover:text-[#005141] transition-colors line-clamp-2">
            {product.name}
          </h3>

          {product.description ? (
            <p className="text-xs sm:text-sm text-stone-600 line-clamp-2 leading-relaxed">
              {product.description}
            </p>
          ) : (
            <div className="h-4" />
          )}

          {/* Tag de tiempo de preparación si existe */}
          {product.prepTimeMinutes ? (
            <div className="flex items-center gap-1 text-[11px] font-medium text-stone-500 pt-1">
              <Clock className="w-3.5 h-3.5 text-[#005141]" />
              <span>Preparación ~{product.prepTimeMinutes} min</span>
            </div>
          ) : null}
          {variants.length > 1 && <label className="block text-xs font-semibold text-stone-600">
            Presentación
            <select aria-label={`Presentación de ${product.name}`} value={variantId} onChange={(event) => setVariantId(event.target.value)} className="mt-1 w-full rounded-xl border border-stone-200 bg-white p-2.5 text-stone-900">
              <option value="">Elige una presentación</option>
              {variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.presentationName} · {formatCurrency(variant.priceAmount, variant.priceCurrency)}</option>)}
            </select>
          </label>}
        </div>
      </div>

      {/* Pie de Card con Precio y Botón de Añadir */}
      <div className="p-4 sm:p-5 pt-0">
        <div className="flex items-center justify-between gap-3 border-t border-stone-100 pt-3.5">
          <div className="flex flex-col">
            <span className="text-[10px] font-bold uppercase tracking-wider text-stone-400">
              Precio
            </span>
            <span className="font-display font-black text-base sm:text-lg text-[#005141]">
              {selected ? formatCurrency(selected.priceAmount, selected.priceCurrency) : displayPrice}
            </span>
          </div>

          <Button
            size="sm"
            disabled={pending || !selected}
            onClick={() => selected && onAdd({ ...product, variantId: selected.id, price: Number(selected.priceAmount) })}
            className="rounded-xl bg-[#005141] hover:bg-[#00382d] text-white text-xs font-bold shadow-sm hover:shadow-md transition-all active:scale-95"
            leftIcon={<Plus className="h-4 w-4 text-[#feae2c]" />}
          >
            {variants.length === 0 ? 'No disponible' : currentQuantity > 0 ? `Añadir (${currentQuantity})` : 'Añadir'}
          </Button>
        </div>
      </div>
    </motion.article>
  );
}

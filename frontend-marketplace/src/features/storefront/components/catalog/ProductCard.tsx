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
      whileHover={{ y: -3 }}
      transition={{ duration: 0.2 }}
      className="group relative flex flex-col justify-between overflow-hidden rounded-[12px] border border-stone-200/90 bg-white shadow-2xs hover:shadow-lg hover:border-stone-300 transition-all duration-300"
    >
      <div>
        {/* Cabecera visual con fotografía editorial (aspect 4:3) */}
        <div className="relative overflow-hidden rounded-t-[12px] bg-stone-100">
          <ProductVisual
            name={product.name}
            imageUrl={product.imageUrl}
            imageBg={product.imageBg || 'from-amber-700/20 to-stone-800/30'}
            iconText={product.iconText || product.name.substring(0, 2).toUpperCase()}
          />

          {/* Insignias Flotantes estilo Shopify */}
          <div className="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5 z-10 pointer-events-none">
            {product.category && product.category !== 'General' && (
              <span className="px-2.5 py-0.5 rounded-full bg-stone-900/80 backdrop-blur-md text-[10px] font-bold uppercase tracking-wider text-white shadow-2xs">
                {product.category}
              </span>
            )}
            {product.variants && product.variants.length > 1 && (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white/95 backdrop-blur-md text-[10px] font-bold text-[#005141] border border-stone-200/80 shadow-2xs">
                <Layers className="w-3 h-3 text-[#ea580c]" />
                <span>Formatos</span>
              </span>
            )}
          </div>
        </div>

        {/* Contenido Editorial del Producto */}
        <div className="p-4 sm:p-5 space-y-2">
          <h3 className="font-display text-base sm:text-lg font-bold text-stone-900 group-hover:text-[#005141] transition-colors line-clamp-2 leading-snug">
            {product.name}
          </h3>

          {product.description ? (
            <p className="text-xs sm:text-sm text-stone-600 line-clamp-2 leading-relaxed">
              {product.description}
            </p>
          ) : (
            <div className="h-4" />
          )}

          {/* Tag de tiempo de preparación artesanal */}
          {product.prepTimeMinutes ? (
            <div className="flex items-center gap-1.5 text-[11px] font-medium text-stone-500 pt-0.5">
              <Clock className="w-3.5 h-3.5 text-[#ea580c]" />
              <span>Preparación ~{product.prepTimeMinutes} min</span>
            </div>
          ) : null}

          {variants.length > 1 && (
            <label className="block pt-1 text-xs font-semibold text-stone-700">
              Presentación
              <select
                aria-label={`Presentación de ${product.name}`}
                value={variantId}
                onChange={(event) => setVariantId(event.target.value)}
                className="mt-1 w-full rounded-[8px] border border-stone-200/90 bg-stone-50/70 p-2 text-xs font-medium text-stone-900 focus:bg-white focus:border-[#005141] focus:outline-none transition-colors"
              >
                <option value="">Elige una presentación</option>
                {variants.map((variant) => (
                  <option key={variant.id} value={variant.id}>
                    {variant.presentationName} · {formatCurrency(variant.priceAmount, variant.priceCurrency)}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      </div>

      {/* Pie de Card: Precio y Botón Pill de Añadir */}
      <div className="p-4 sm:p-5 pt-0">
        <div className="flex items-center justify-between gap-3 border-t border-stone-100 pt-3">
          <div className="flex flex-col">
            <span className="text-[10px] font-bold uppercase tracking-wider text-stone-400">
              Precio
            </span>
            <span className="font-display font-black text-base sm:text-lg text-stone-900">
              {selected ? formatCurrency(selected.priceAmount, selected.priceCurrency) : displayPrice}
            </span>
          </div>

          <Button
            size="sm"
            disabled={pending || !selected}
            onClick={() => selected && onAdd({ ...product, variantId: selected.id, price: Number(selected.priceAmount) })}
            className="rounded-full bg-[#005141] hover:bg-[#00382d] text-white text-xs font-bold px-4 py-2 shadow-xs hover:shadow-md transition-all active:scale-95"
            leftIcon={<Plus className="h-3.5 w-3.5 text-[#feae2c]" />}
          >
            {variants.length === 0 ? 'No disponible' : currentQuantity > 0 ? `Añadir (${currentQuantity})` : 'Añadir'}
          </Button>
        </div>
      </div>
    </motion.article>
  );
}

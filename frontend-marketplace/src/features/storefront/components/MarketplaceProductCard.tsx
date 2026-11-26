'use client';

import React from 'react';
import Link from 'next/link';
import { Store, ArrowRight, Images, Bike, ShoppingBag } from 'lucide-react';
import { MarketplaceProductItem } from '../types/storefront.types';
import { SafeProductImage } from './SafeProductImage';
import { formatCurrency } from '@/shared/utils/formatters';
import { cn } from '@/shared/utils/cn';

export interface MarketplaceProductCardProps {
  product: MarketplaceProductItem;
  layout?: 'grid' | 'list';
}

export function MarketplaceProductCard({
  product,
  layout = 'grid',
}: MarketplaceProductCardProps) {
  const isList = layout === 'list';
  const href = product.slug
    ? `/tienda/${product.storeSlug}/producto/${product.slug}`
    : `/tienda/${product.storeSlug}`;

  const hasValidPrice =
    typeof product.price === 'number' &&
    Number.isFinite(product.price) &&
    product.price >= 0;

  return (
    <Link
      href={href}
      className={cn(
        'group rounded-2xl bg-white border border-[#e7e0d6] overflow-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141] [@media(hover:hover)]:hover:border-[#005141]/40',
        isList
          ? 'flex flex-col sm:flex-row'
          : 'flex flex-col justify-between h-full'
      )}
    >
      {/* Image */}
      <div
        className={cn(
          'relative overflow-hidden bg-[#faf7f2] shrink-0',
          isList ? 'aspect-[4/3] sm:aspect-auto sm:w-44 md:w-52 sm:min-h-[156px]' : 'aspect-[4/3] w-full'
        )}
      >
        <SafeProductImage
          src={product.imageUrl}
          alt={product.name}
          variant="card"
          category={product.category}
          sizes={
            isList
              ? '(max-width: 640px) 100vw, 208px'
              : '(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw'
          }
          className="[@media(hover:hover)]:group-hover:scale-105"
        />
        {product.images && product.images.length > 1 && (
          <span
            aria-label={`${product.images.length} fotografías`}
            className="absolute bottom-2.5 left-2.5 z-10 inline-flex items-center gap-1 rounded-full bg-[#02090a]/85 px-2.5 py-0.5 text-[11px] font-semibold text-white tabular-nums"
          >
            <Images className="h-3 w-3 text-[#36f4a4]" aria-hidden="true" />
            <span>{product.images.length}</span>
          </span>
        )}
      </div>

      {/* Content + Footer */}
      <div className="flex-1 flex flex-col justify-between min-w-0">
        <div className="p-4 space-y-2 min-w-0">
          <div className="flex items-center justify-between gap-2 text-xs font-semibold text-stone-600 min-w-0">
            <span className="inline-flex items-center gap-1 min-w-0 truncate">
              <Store className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
              <span className="truncate">{product.storeName}</span>
            </span>
            {product.category && (
              <span className="shrink-0 text-[#005141]">{product.category}</span>
            )}
          </div>

          <h3 className="font-display text-base font-bold text-stone-900 group-hover:text-[#005141] line-clamp-2">
            {product.name}
          </h3>

          {isList && product.description && (
            <p className="text-xs text-stone-600 line-clamp-2 leading-relaxed">
              {product.description}
            </p>
          )}

          {(product.deliveryEnabled || product.pickupEnabled) && (
            <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
              {product.deliveryEnabled && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#faf7f2] border border-[#e7e0d6] text-[11px] font-semibold text-stone-700">
                  <Bike className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                  <span>Delivery</span>
                </span>
              )}
              {product.pickupEnabled && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#faf7f2] border border-[#e7e0d6] text-[11px] font-semibold text-stone-700">
                  <ShoppingBag className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                  <span>Retiro</span>
                </span>
              )}
            </div>
          )}
        </div>

        <div className="px-4 pb-4 pt-3 border-t border-stone-100 flex items-center justify-between gap-3">
          <span className="text-base font-bold text-[#005141] tabular-nums">
            {hasValidPrice
              ? formatCurrency(product.price, product.priceCurrency || 'PEN')
              : 'Consultar'}
          </span>
          <span className="inline-flex items-center gap-1 text-xs font-semibold text-stone-700 group-hover:text-[#005141]">
            <span>Ver</span>
            <ArrowRight
              aria-hidden="true"
              className="w-3.5 h-3.5 transition-transform duration-150 ease-out motion-reduce:transition-none [@media(hover:hover)]:group-hover:translate-x-0.5"
            />
          </span>
        </div>
      </div>
    </Link>
  );
}

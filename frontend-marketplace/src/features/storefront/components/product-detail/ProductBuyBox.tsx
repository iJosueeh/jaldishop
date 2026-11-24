'use client';

import React, { useMemo } from 'react';
import Link from 'next/link';
import {
  Check,
  MessageCircle,
  Minus,
  Plus,
  Share2,
  ShoppingCart,
  Star,
  Store as StoreIcon,
} from 'lucide-react';
import { toast } from 'sonner';
import {
  ProductItem,
  ProductReviewsSummary,
  ProductVariantItem,
  PublicStore,
} from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';
import { FavoriteButton } from '../FavoriteButton';
import { cn } from '@/shared/utils/cn';

interface ProductBuyBoxProps {
  store: PublicStore;
  product: ProductItem;
  variants: ProductVariantItem[];
  selectedVariant: ProductVariantItem | undefined;
  onSelectVariant: (variantId: string) => void;
  quantity: number;
  onChangeQuantity: (next: number) => void;
  inCartQty: number;
  onAddToCart: () => Promise<void>;
  pending: boolean;
  reviewsSummary?: ProductReviewsSummary | null;
  liveAnnouncement?: string;
}

export function ProductBuyBox({
  store,
  product,
  variants,
  selectedVariant,
  onSelectVariant,
  quantity,
  onChangeQuantity,
  inCartQty,
  onAddToCart,
  pending,
  reviewsSummary,
  liveAnnouncement,
}: ProductBuyBoxProps) {
  const hasPriceRange =
    !selectedVariant &&
    product.minPrice != null &&
    product.maxPrice != null &&
    product.minPrice !== product.maxPrice;

  const currency = selectedVariant?.priceCurrency ?? product.priceCurrency ?? 'PEN';
  const unitPrice = selectedVariant ? Number(selectedVariant.priceAmount) : Number(product.price);
  const displayPrice = selectedVariant
    ? formatCurrency(selectedVariant.priceAmount, currency)
    : hasPriceRange
      ? `${formatCurrency(product.minPrice!, currency)} - ${formatCurrency(product.maxPrice!, currency)}`
      : formatCurrency(product.price, currency);

  const tracksInventory = Boolean(selectedVariant?.tracksInventory);
  const availableStock =
    selectedVariant && typeof selectedVariant.availableQuantity === 'number'
      ? selectedVariant.availableQuantity
      : null;

  const remainingForUser =
    tracksInventory && availableStock !== null ? Math.max(0, availableStock - inCartQty) : 20;
  const maxSelectableQty = Math.min(20, Math.max(1, remainingForUser));

  const isOutOfStock =
    variants.length === 0 ||
    (tracksInventory && availableStock !== null && availableStock <= 0);

  const isMaxInCart =
    tracksInventory && availableStock !== null && availableStock > 0 && remainingForUser <= 0;

  const canAddToCart =
    !pending &&
    !isOutOfStock &&
    !isMaxInCart &&
    (variants.length === 0 || Boolean(selectedVariant));

  const stockBadge = useMemo(() => {
    if (variants.length === 0) {
      return { label: 'Sin presentaciones activas', tone: 'danger' as const };
    }
    if (!selectedVariant) return null;
    if (!selectedVariant.tracksInventory) {
      return { label: 'Disponible', tone: 'ok' as const };
    }
    if (availableStock === null) {
      return { label: 'Control de stock activo', tone: 'neutral' as const };
    }
    if (availableStock <= 0) {
      return { label: 'Agotado', tone: 'danger' as const };
    }
    if (availableStock <= 5) {
      return {
        label: `¡Últimas ${availableStock} ${availableStock === 1 ? 'unidad' : 'unidades'}!`,
        tone: 'warn' as const,
      };
    }
    return { label: `${availableStock} disponibles`, tone: 'ok' as const };
  }, [availableStock, selectedVariant, variants.length]);

  const contactNumber = store.whatsappNumber || store.contactPhone || store.phone;
  const cleanDigits = contactNumber?.replace(/\D/g, '') || '';
  const whatsappHref = cleanDigits
    ? `https://wa.me/${cleanDigits}?text=${encodeURIComponent(
        `Hola ${store.name}, tengo una consulta sobre "${product.name}"${
          selectedVariant ? ` (${selectedVariant.presentationName})` : ''
        }.`
      )}`
    : null;

  const handleShare = async () => {
    const url = window.location.href;
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: `${product.name} — ${store.name}`,
          text: product.description || `Mira ${product.name} en ${store.name}`,
          url,
        });
        return;
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
      }
    }

    try {
      await navigator.clipboard.writeText(url);
      toast.success('Enlace del producto copiado.');
    } catch {
      toast.error('No pudimos copiar el enlace.');
    }
  };

  const handleVariantKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (variants.length <= 1) return;
    let nextIndex = -1;
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      nextIndex = (index + 1) % variants.length;
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      nextIndex = (index - 1 + variants.length) % variants.length;
    }
    if (nextIndex >= 0) {
      event.preventDefault();
      onSelectVariant(variants[nextIndex].id);
    }
  };

  return (
    <div className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-xs space-y-5">
      {/* Encabezado: Categoría, Título y Favorito */}
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1.5 min-w-0">
          {product.category && (
            <span className="inline-block rounded-full bg-[#072720] border border-[#133b32] px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider text-[#36f4a4]">
              {product.category}
            </span>
          )}
          <h1 className="font-display text-2xl sm:text-3xl font-extrabold leading-tight text-stone-950">
            {product.name}
          </h1>
        </div>
        <FavoriteButton productId={product.id} />
      </div>

      {/* Rating */}
      {reviewsSummary && reviewsSummary.totalReviews > 0 && (
        <div className="flex flex-wrap items-center gap-3 text-xs">
          <a
            href="#product-reviews-heading"
            className="inline-flex items-center gap-1.5 font-semibold text-stone-600 hover:text-[#005141] transition-colors"
          >
            <Star className="h-4 w-4 fill-amber-400 text-amber-500" />
            <span className="text-stone-950 font-bold">{reviewsSummary.averageRating.toFixed(1)}</span>
            <span>({reviewsSummary.totalReviews} {reviewsSummary.totalReviews === 1 ? 'reseña' : 'reseñas'})</span>
          </a>
        </div>
      )}

      {/* Precio + Stock en vivo */}
      <div className="border-y border-stone-100 py-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-stone-400">Precio</p>
          <p className="font-display text-3xl font-black text-[#005141] tabular-nums mt-0.5 transition-opacity duration-150 motion-reduce:transition-none">
            {displayPrice}
          </p>
          {quantity > 1 && !hasPriceRange && (
            <p className="text-xs text-stone-500 mt-0.5 tabular-nums">
              Subtotal ({quantity} unid.):{' '}
              <span className="font-semibold text-stone-800">
                {formatCurrency(unitPrice * quantity, currency)}
              </span>
            </p>
          )}
        </div>

        {stockBadge && (
          <span
            className={cn(
              'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold border',
              stockBadge.tone === 'ok' && 'bg-emerald-50 border-emerald-200 text-emerald-800',
              stockBadge.tone === 'warn' && 'bg-amber-50 border-amber-200 text-amber-800',
              stockBadge.tone === 'danger' && 'bg-rose-50 border-rose-200 text-rose-700',
              stockBadge.tone === 'neutral' && 'bg-stone-100 border-stone-200 text-stone-700'
            )}
          >
            <span
              className={cn(
                'h-2 w-2 rounded-full',
                stockBadge.tone === 'ok' && 'bg-emerald-500',
                stockBadge.tone === 'warn' && 'bg-amber-500',
                stockBadge.tone === 'danger' && 'bg-rose-500',
                stockBadge.tone === 'neutral' && 'bg-stone-400'
              )}
              aria-hidden="true"
            />
            {stockBadge.label}
          </span>
        )}
      </div>

      {/* Selector de presentaciones */}
      {variants.length > 0 && (
        <div className="space-y-2.5">
          <div className="flex items-center justify-between text-xs">
            <span id="variant-group-label" className="font-bold text-stone-800">
              Presentación
            </span>
            {selectedVariant && (
              <span className="font-medium text-stone-500">{selectedVariant.presentationName}</span>
            )}
          </div>
          <div
            role="radiogroup"
            aria-labelledby="variant-group-label"
            className="grid grid-cols-1 sm:grid-cols-2 gap-2.5"
          >
            {variants.map((variant, index) => {
              const isSelected = variant.id === selectedVariant?.id;
              const variantOutOfStock =
                variant.tracksInventory &&
                typeof variant.availableQuantity === 'number' &&
                variant.availableQuantity <= 0;

              return (
                <button
                  key={variant.id}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  onClick={() => onSelectVariant(variant.id)}
                  onKeyDown={(e) => handleVariantKeyDown(e, index)}
                  className={cn(
                    'relative flex flex-col items-start rounded-xl border p-3 text-left min-h-[56px] transition-all duration-150 ease-out motion-reduce:transition-none cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]',
                    isSelected
                      ? 'border-[#005141] bg-emerald-50/70 ring-1 ring-[#005141] shadow-2xs'
                      : 'border-stone-200 bg-white hover:border-stone-300 hover:bg-stone-50/50',
                    variantOutOfStock && 'opacity-65'
                  )}
                >
                  <div className="flex w-full items-center justify-between gap-2">
                    <span className="text-xs font-bold text-stone-900 line-clamp-1">
                      {variant.presentationName}
                    </span>
                    {isSelected && <Check className="h-3.5 w-3.5 text-[#005141] shrink-0" />}
                  </div>
                  <div className="mt-1 flex w-full items-center justify-between gap-2">
                    <span className="text-xs font-bold text-[#005141] tabular-nums">
                      {formatCurrency(variant.priceAmount, variant.priceCurrency)}
                    </span>
                    {variant.tracksInventory && typeof variant.availableQuantity === 'number' && (
                      <span
                        className={cn(
                          'text-[10px] font-semibold',
                          variant.availableQuantity <= 0 ? 'text-rose-600' : 'text-stone-500'
                        )}
                      >
                        {variant.availableQuantity <= 0
                          ? 'Agotado'
                          : `${variant.availableQuantity} disp.`}
                      </span>
                    )}
                  </div>
                  {variant.attributes && variant.attributes.length > 0 && (
                    <div className="mt-1.5 flex flex-wrap gap-1">
                      {variant.attributes.slice(0, 2).map((attr) => (
                        <span
                          key={`${variant.id}-${attr.name}`}
                          className="rounded bg-stone-100 px-1.5 py-0.5 text-[10px] text-stone-600"
                        >
                          {attr.name}: {attr.value}
                        </span>
                      ))}
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Región viva accesible para cambios de variante / stock */}
      <div className="sr-only" aria-live="polite" aria-atomic="true">
        {liveAnnouncement}
      </div>

      {/* Stepper de cantidad + CTA */}
      <div className="space-y-3 pt-1">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <span className="text-xs font-bold text-stone-800 block">Cantidad</span>
            {inCartQty > 0 && (
              <span className="text-[11px] font-semibold text-[#005141]">
                Ya tienes {inCartQty} {inCartQty === 1 ? 'unidad' : 'unidades'} en tu carrito
              </span>
            )}
          </div>
          <div className="inline-flex items-center rounded-xl border border-stone-200 bg-stone-50">
            <button
              type="button"
              aria-label="Disminuir cantidad"
              onClick={() => onChangeQuantity(Math.max(1, quantity - 1))}
              disabled={quantity <= 1 || isOutOfStock}
              className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center text-stone-600 hover:text-stone-900 disabled:opacity-40 cursor-pointer"
            >
              <Minus className="h-3.5 w-3.5" />
            </button>
            <span className="w-8 text-center text-sm font-bold text-stone-900 tabular-nums">
              {quantity}
            </span>
            <button
              type="button"
              aria-label="Aumentar cantidad"
              onClick={() => onChangeQuantity(Math.min(maxSelectableQty, quantity + 1))}
              disabled={quantity >= maxSelectableQty || isOutOfStock || isMaxInCart}
              className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center text-stone-600 hover:text-stone-900 disabled:opacity-40 cursor-pointer"
            >
              <Plus className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        <button
          type="button"
          onClick={onAddToCart}
          disabled={!canAddToCart}
          className="w-full min-h-[48px] inline-flex items-center justify-center gap-2 rounded-full bg-[#005141] px-6 py-3.5 text-sm font-bold text-white shadow-md shadow-[#005141]/20 transition-all duration-150 hover:bg-[#00382d] active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
        >
          <ShoppingCart className="h-4 w-4 text-[#36f4a4]" />
          <span>
            {isOutOfStock
              ? 'Producto agotado'
              : isMaxInCart
                ? 'Stock máximo en tu carrito'
                : inCartQty > 0
                  ? `Añadir más (${inCartQty} en carrito)`
                  : 'Añadir al carrito'}
          </span>
        </button>

        {/* Acciones secundarias */}
        <div className="grid grid-cols-2 gap-2 pt-1">
          <Link
            href={`/tienda/${store.slug}`}
            className="min-h-[44px] inline-flex items-center justify-center gap-1.5 rounded-full border border-stone-200 bg-white px-3 py-2 text-xs font-semibold text-stone-700 transition-colors hover:border-[#005141] hover:text-[#005141]"
          >
            <StoreIcon className="h-3.5 w-3.5" />
            <span>Ir a la tienda</span>
          </Link>

          {whatsappHref ? (
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              className="min-h-[44px] inline-flex items-center justify-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50/70 px-3 py-2 text-xs font-semibold text-[#005141] transition-colors hover:bg-emerald-100"
            >
              <MessageCircle className="h-3.5 w-3.5" />
              <span>WhatsApp</span>
            </a>
          ) : (
            <button
              type="button"
              onClick={handleShare}
              className="min-h-[44px] inline-flex items-center justify-center gap-1.5 rounded-full border border-stone-200 bg-white px-3 py-2 text-xs font-semibold text-stone-700 transition-colors hover:border-[#005141] hover:text-[#005141] cursor-pointer"
            >
              <Share2 className="h-3.5 w-3.5" />
              <span>Compartir</span>
            </button>
          )}
        </div>

        {whatsappHref && (
          <button
            type="button"
            onClick={handleShare}
            className="w-full min-h-[40px] inline-flex items-center justify-center gap-1.5 rounded-full border border-stone-200/80 bg-stone-50 px-3 py-1.5 text-xs font-semibold text-stone-600 transition-colors hover:text-stone-900 cursor-pointer"
          >
            <Share2 className="h-3.5 w-3.5" />
            <span>Compartir enlace del producto</span>
          </button>
        )}
      </div>
    </div>
  );
}

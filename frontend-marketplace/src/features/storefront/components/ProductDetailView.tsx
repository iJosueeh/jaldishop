'use client';

import Link from 'next/link';
import { useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Clock,
  Minus,
  Plus,
  ShieldCheck,
  ShoppingBag,
  Star,
  Store as StoreIcon,
  Truck,
} from 'lucide-react';
import { toast } from 'sonner';
import { ProductVisual } from './ProductVisual';
import {
  ProductItem,
  ProductReviewsSummary,
  PublicStore,
  PublicStoreSettings,
} from '../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';
import { FavoriteButton } from './FavoriteButton';
import { cn } from '@/shared/utils/cn';

interface ProductDetailViewProps {
  store: PublicStore;
  product: ProductItem;
  storeSettings?: PublicStoreSettings | null;
  reviewsSummary?: ProductReviewsSummary | null;
}

export function ProductDetailView({
  store,
  product,
  storeSettings,
  reviewsSummary,
}: ProductDetailViewProps) {
  const variants = product.variants?.filter((variant) => variant.status === 'ACTIVE') ?? [];
  const [variantId, setVariantId] = useState(variants.length > 0 ? variants[0].id : '');
  const [quantity, setQuantity] = useState(1);
  const { selectStore, changeQuantity, pending } = useStorefrontCart();

  const selected = variants.find((variant) => variant.id === variantId);
  const hasPriceRange =
    !selected &&
    product.minPrice != null &&
    product.maxPrice != null &&
    product.minPrice !== product.maxPrice;

  const displayPrice = selected
    ? formatCurrency(selected.priceAmount, selected.priceCurrency)
    : hasPriceRange
      ? `${formatCurrency(product.minPrice!)} - ${formatCurrency(product.maxPrice!)}`
      : formatCurrency(product.price);

  const canAddToCart = !pending && (variants.length === 0 || Boolean(selected));

  const addToCart = async () => {
    if (!store.slug || !canAddToCart) return;
    selectStore({ id: store.id, name: store.name, slug: store.slug });
    const itemToAdd: ProductItem = selected
      ? {
          ...product,
          variantId: selected.id,
          price: Number(selected.priceAmount),
          name: `${product.name} — ${selected.presentationName}`,
        }
      : product;

    await changeQuantity(itemToAdd, quantity);
    toast.success(`${quantity} × ${product.name} añadido al carrito`);
  };

  return (
    <div className="min-h-screen bg-[#fcfbfa] py-6 sm:py-10">
      <main className="mx-auto w-full max-w-6xl px-4 sm:px-6 lg:px-8 space-y-6">
        {/* Breadcrumb Marketplace */}
        <nav aria-label="Ruta de navegación" className="flex flex-wrap items-center gap-1.5 text-xs font-medium text-stone-500">
          <Link href="/" className="hover:text-[#005141] transition-colors">
            Inicio
          </Link>
          <ChevronRight className="h-3.5 w-3.5 text-stone-400" />
          <Link href={`/tienda/${store.slug}`} className="hover:text-[#005141] transition-colors">
            {store.name}
          </Link>
          {product.category && (
            <>
              <ChevronRight className="h-3.5 w-3.5 text-stone-400" />
              <span className="text-stone-600">{product.category}</span>
            </>
          )}
          <ChevronRight className="h-3.5 w-3.5 text-stone-400" />
          <span className="text-stone-900 font-semibold truncate max-w-[220px] sm:max-w-xs">
            {product.name}
          </span>
        </nav>

        <Link
          href={`/tienda/${store.slug}`}
          className="inline-flex items-center gap-2 text-sm font-semibold text-stone-600 transition-colors hover:text-[#005141]"
        >
          <ArrowLeft className="h-4 w-4" /> Volver a {store.name}
        </Link>

        {/* Grid principal estilo Marketplace */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 items-start">
          {/* Columna Izquierda: Galería, Descripción, Especificaciones y Reseñas */}
          <div className="lg:col-span-7 space-y-6">
            <div className="relative overflow-hidden rounded-[12px] border border-stone-200/90 bg-white shadow-2xs">
              <ProductVisual
                name={product.name}
                imageUrl={product.imageUrl}
                imageBg={product.imageBg ?? 'from-amber-700/20 to-stone-800/30'}
                iconText={product.iconText ?? product.name.substring(0, 2).toUpperCase()}
              />
              {product.prepTimeMinutes ? (
                <div className="absolute bottom-3 left-3 inline-flex items-center gap-1.5 rounded-full bg-white/95 backdrop-blur-md px-3 py-1.5 text-xs font-semibold text-stone-700 border border-stone-200 shadow-xs">
                  <Clock className="h-3.5 w-3.5 text-[#ea580c]" />
                  <span>Preparación ~{product.prepTimeMinutes} min</span>
                </div>
              ) : null}
            </div>

            {/* Descripción */}
            <section className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-3">
              <h2 className="font-display text-lg font-bold text-stone-900">Descripción del producto</h2>
              <p className="text-sm leading-relaxed text-stone-600">
                {product.description || 'La tienda no agregó una descripción detallada para este producto.'}
              </p>
            </section>

            {/* Especificaciones / Atributos de variante */}
            {selected?.attributes?.length ? (
              <section className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-3">
                <h2 className="font-display text-lg font-bold text-stone-900">Características principales</h2>
                <dl className="divide-y divide-stone-100 rounded-lg border border-stone-200 overflow-hidden">
                  {selected.attributes.map((attribute, idx) => (
                    <div
                      key={attribute.name}
                      className={cn(
                        'grid grid-cols-2 gap-4 px-4 py-2.5 text-xs sm:text-sm',
                        idx % 2 === 0 ? 'bg-stone-50/80' : 'bg-white'
                      )}
                    >
                      <dt className="font-semibold text-stone-600">{attribute.name}</dt>
                      <dd className="text-stone-900 font-medium">{attribute.value}</dd>
                    </div>
                  ))}
                </dl>
              </section>
            ) : null}

            {/* Opiniones del producto */}
            <section
              aria-labelledby="product-reviews-heading"
              className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 pb-3">
                <h2 id="product-reviews-heading" className="font-display text-lg font-bold text-stone-900">
                  Opiniones del producto
                </h2>
                {reviewsSummary && reviewsSummary.totalReviews > 0 ? (
                  <div className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs font-bold text-amber-800">
                    <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-500" />
                    <span>{reviewsSummary.averageRating.toFixed(1)}</span>
                    <span className="font-normal text-amber-700">
                      ({reviewsSummary.totalReviews} {reviewsSummary.totalReviews === 1 ? 'opinión' : 'opiniones'})
                    </span>
                  </div>
                ) : null}
              </div>

              {reviewsSummary && reviewsSummary.reviews.length > 0 ? (
                <div className="space-y-3">
                  {reviewsSummary.reviews.map((review) => (
                    <article key={review.id} className="rounded-xl border border-stone-100 bg-stone-50/70 p-4 space-y-1.5">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-bold text-stone-900">{review.customerName}</span>
                        <span className="text-[11px] text-stone-400">
                          {new Date(review.createdAt).toLocaleDateString('es-PE')}
                        </span>
                      </div>
                      <div className="flex items-center gap-0.5" aria-label={`Calificación ${review.rating} de 5`}>
                        {[1, 2, 3, 4, 5].map((star) => (
                          <Star
                            key={star}
                            className={cn(
                              'h-3.5 w-3.5',
                              star <= review.rating ? 'fill-amber-400 text-amber-500' : 'text-stone-300'
                            )}
                          />
                        ))}
                      </div>
                      {review.comment && <p className="text-xs sm:text-sm text-stone-600">{review.comment}</p>}
                    </article>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-stone-500">
                  Aún no hay opiniones publicadas para este producto.
                </p>
              )}
            </section>
          </div>

          {/* Columna Derecha Sticky: Buy Box estilo Marketplace */}
          <aside className="lg:col-span-5 lg:sticky lg:top-24 space-y-4">
            <div className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-xs space-y-5">
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1.5">
                  {product.category && (
                    <span className="inline-block rounded-full bg-orange-50 border border-orange-200 px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider text-[#ea580c]">
                      {product.category}
                    </span>
                  )}
                  <h1 className="font-display text-2xl sm:text-3xl font-black leading-tight text-stone-900">
                    {product.name}
                  </h1>
                </div>
                <FavoriteButton productId={product.id} />
              </div>

              {reviewsSummary && reviewsSummary.totalReviews > 0 && (
                <a
                  href="#product-reviews-heading"
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-stone-600 hover:text-[#005141]"
                >
                  <Star className="h-4 w-4 fill-amber-400 text-amber-500" />
                  <span className="text-stone-900 font-bold">{reviewsSummary.averageRating.toFixed(1)}</span>
                  <span>({reviewsSummary.totalReviews} reseñas)</span>
                </a>
              )}

              {/* Precio destacado */}
              <div className="border-y border-stone-100 py-4">
                <p className="text-[11px] font-bold uppercase tracking-wider text-stone-400">Precio</p>
                <p className="font-display text-3xl font-black text-[#005141] tabular-nums mt-0.5">
                  {displayPrice}
                </p>
              </div>

              {/* Selector de presentaciones */}
              {variants.length > 0 && (
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-stone-800">Presentación</span>
                    {selected && <span className="text-stone-500">{selected.presentationName}</span>}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2" role="group" aria-label="Presentaciones disponibles">
                    {variants.map((variant) => {
                      const isSelected = variant.id === variantId;
                      return (
                        <button
                          key={variant.id}
                          type="button"
                          aria-pressed={isSelected}
                          onClick={() => setVariantId(variant.id)}
                          className={cn(
                            'flex flex-col items-start rounded-xl border p-3 text-left transition-all cursor-pointer',
                            isSelected
                              ? 'border-[#005141] bg-emerald-50/60 ring-1 ring-[#005141]'
                              : 'border-stone-200 bg-white hover:border-stone-300'
                          )}
                        >
                          <span className="text-xs font-bold text-stone-900">{variant.presentationName}</span>
                          <span className="text-xs font-semibold text-[#005141] mt-0.5">
                            {formatCurrency(variant.priceAmount, variant.priceCurrency)}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Stepper de cantidad + CTA */}
              <div className="space-y-3 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-stone-800">Cantidad</span>
                  <div className="inline-flex items-center rounded-xl border border-stone-200 bg-stone-50">
                    <button
                      type="button"
                      aria-label="Disminuir cantidad"
                      onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                      disabled={quantity <= 1}
                      className="p-2 text-stone-600 hover:text-stone-900 disabled:opacity-40 cursor-pointer"
                    >
                      <Minus className="h-3.5 w-3.5" />
                    </button>
                    <span className="w-8 text-center text-sm font-bold text-stone-900 tabular-nums">
                      {quantity}
                    </span>
                    <button
                      type="button"
                      aria-label="Aumentar cantidad"
                      onClick={() => setQuantity((q) => Math.min(20, q + 1))}
                      disabled={quantity >= 20}
                      className="p-2 text-stone-600 hover:text-stone-900 disabled:opacity-40 cursor-pointer"
                    >
                      <Plus className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={addToCart}
                  disabled={!canAddToCart}
                  className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-[#005141] px-6 py-3.5 text-sm font-bold text-white shadow-md shadow-[#005141]/20 transition hover:bg-[#00382d] active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
                >
                  <ShoppingBag className="h-4 w-4 text-[#feae2c]" />
                  <span>Añadir al carrito</span>
                </button>
              </div>
            </div>

            {/* Tarjeta de Confianza y Tienda */}
            <div className="rounded-[12px] border border-stone-200/90 bg-white p-5 shadow-2xs space-y-3 text-xs text-stone-600">
              <div className="flex items-center gap-2.5 text-stone-900 font-bold">
                <StoreIcon className="h-4 w-4 text-[#005141]" />
                <span>Vendido por {store.name}</span>
              </div>
              <div className="flex items-start gap-2.5">
                <Truck className="h-4 w-4 text-[#ea580c] shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-stone-800">
                    {store.deliveryEnabled !== false && store.pickupEnabled !== false
                      ? 'Delivery y recojo en tienda disponibles'
                      : store.pickupEnabled
                        ? 'Recojo en tienda disponible'
                        : 'Delivery disponible'}
                  </p>
                  {storeSettings?.estimatedPrepTimeMinutes ? (
                    <p className="text-stone-500 mt-0.5">
                      Tiempo estimado de preparación: ~{storeSettings.estimatedPrepTimeMinutes} min
                    </p>
                  ) : null}
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <ShieldCheck className="h-4 w-4 text-emerald-700 shrink-0 mt-0.5" />
                <p>
                  Compra protegida con Mercado Pago y reserva de capacidad garantizada.
                </p>
              </div>
              {storeSettings?.cancellationPolicy && (
                <div className="flex items-start gap-2.5 pt-1 border-t border-stone-100">
                  <CheckCircle2 className="h-4 w-4 text-stone-400 shrink-0 mt-0.5" />
                  <p className="text-[11px] text-stone-500 line-clamp-2">{storeSettings.cancellationPolicy}</p>
                </div>
              )}
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}

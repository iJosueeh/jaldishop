'use client';

import Link from 'next/link';
import { useState } from 'react';
import { ArrowLeft, ChevronRight, ShoppingCart } from 'lucide-react';
import { toast } from 'sonner';
import { Container } from '@/shared/components/ui/Container';
import {
  ProductItem,
  ProductReviewsSummary,
  PublicStore,
  PublicStoreSettings,
} from '../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';
import { useProductLiveSync } from '../hooks/useProductLiveSync';
import {
  ProductBuyBox,
  ProductGallery,
  ProductReviewsSection,
  RelatedStoreProducts,
  StoreTrustCard,
} from './product-detail';

interface ProductDetailViewProps {
  store: PublicStore;
  product: ProductItem;
  storeSettings?: PublicStoreSettings | null;
  reviewsSummary?: ProductReviewsSummary | null;
  relatedProducts?: ProductItem[];
}

export function ProductDetailView({
  store,
  product: initialProduct,
  storeSettings,
  reviewsSummary: initialReviewsSummary,
  relatedProducts = [],
}: ProductDetailViewProps) {
  const { product, reviewsSummary, refreshReviews } = useProductLiveSync({
    storeSlug: store.slug,
    initialProduct,
    initialReviewsSummary,
  });

  const variants = product.variants?.filter((variant) => variant.status === 'ACTIVE') ?? [];
  const [variantId, setVariantId] = useState(variants.length > 0 ? variants[0].id : '');
  const [quantity, setQuantity] = useState(1);
  const [liveAnnouncement, setLiveAnnouncement] = useState('');
  const { items = [], selectStore, changeQuantity, pending } = useStorefrontCart();

  const selected = variants.find((variant) => variant.id === variantId) ?? variants[0];

  const inCartQty = selected
    ? items
        .filter((entry) => entry.product.variantId === selected.id)
        .reduce((sum, entry) => sum + entry.quantity, 0)
    : items
        .filter((entry) => entry.product.id === product.id)
        .reduce((sum, entry) => sum + entry.quantity, 0);

  const isOutOfStock =
    variants.length === 0 ||
    Boolean(
      selected?.tracksInventory &&
        typeof selected.availableQuantity === 'number' &&
        selected.availableQuantity <= 0
    );

  const handleSelectVariant = (nextId: string) => {
    setVariantId(nextId);
    const nextVariant = variants.find((v) => v.id === nextId);
    if (!nextVariant) return;

    const nextInCart = items
      .filter((entry) => entry.product.variantId === nextVariant.id)
      .reduce((sum, entry) => sum + entry.quantity, 0);

    let nextQty = quantity;
    if (nextVariant.tracksInventory && typeof nextVariant.availableQuantity === 'number') {
      const remaining = Math.max(0, nextVariant.availableQuantity - nextInCart);
      if (remaining > 0 && quantity > remaining) {
        nextQty = remaining;
        setQuantity(remaining);
      } else if (remaining === 0) {
        nextQty = 1;
        setQuantity(1);
      }
    }

    const priceText = formatCurrency(nextVariant.priceAmount, nextVariant.priceCurrency);
    const stockText =
      nextVariant.tracksInventory && typeof nextVariant.availableQuantity === 'number'
        ? nextVariant.availableQuantity <= 0
          ? 'Agotado.'
          : `${nextVariant.availableQuantity} disponibles.`
        : 'Disponible.';

    setLiveAnnouncement(
      `Presentación ${nextVariant.presentationName} seleccionada. Precio ${priceText}. ${stockText} Cantidad ${nextQty}.`
    );
  };

  const addToCart = async () => {
    if (!store.slug || pending || isOutOfStock) return;
    if (variants.length > 0 && !selected) return;

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

  const mobileDisplayPrice = selected
    ? formatCurrency(selected.priceAmount, selected.priceCurrency)
    : formatCurrency(product.price, product.priceCurrency ?? 'PEN');

  return (
    <div className="min-h-screen bg-[#faf7f2] pb-24 lg:pb-12">
      {/* Banda superior Midnight Greenhouse (DESIGN.md) */}
      <header className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] text-white border-b border-[#1e2c31] pt-24 sm:pt-28 pb-5 sm:pb-6">
        <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12 space-y-3">
          <nav
            aria-label="Ruta de navegación"
            className="flex flex-wrap items-center gap-1.5 text-xs font-medium text-[#99b3ad]"
          >
            <Link href="/" className="hover:text-[#36f4a4] transition-colors">
              Inicio
            </Link>
            <ChevronRight className="h-3.5 w-3.5 text-[#71717a]" />
            <Link href={`/tienda/${store.slug}`} className="hover:text-[#36f4a4] transition-colors">
              {store.name}
            </Link>
            {product.category && (
              <>
                <ChevronRight className="h-3.5 w-3.5 text-[#71717a]" />
                <span className="text-[#9dabad]">{product.category}</span>
              </>
            )}
            <ChevronRight className="h-3.5 w-3.5 text-[#71717a]" />
            <span className="text-white font-semibold truncate max-w-[220px] sm:max-w-md">
              {product.name}
            </span>
          </nav>

          <Link
            href={`/tienda/${store.slug}`}
            className="inline-flex items-center gap-2 text-xs sm:text-sm font-semibold text-[#36f4a4] transition-colors hover:underline underline-offset-4"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Volver al catálogo de {store.name}</span>
          </Link>
        </Container>
      </header>

      <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12 py-6 sm:py-8 lg:py-10 space-y-10">
        {/* Grid principal asimétrico */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 xl:gap-10 items-start">
          {/* Columna Izquierda: Galería, Descripción, Ficha técnica y Reseñas */}
          <div className="lg:col-span-7 space-y-6 min-w-0">
            <ProductGallery
              name={product.name}
              imageUrl={product.imageUrl}
              category={product.category}
              prepTimeMinutes={product.prepTimeMinutes}
              isAvailable={variants.length > 0}
              isOutOfStock={isOutOfStock}
            />

            {/* Descripción */}
            <section className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-3">
              <h2 className="font-display text-lg font-bold text-stone-950">Descripción del producto</h2>
              <p className="text-sm sm:text-base leading-relaxed text-stone-600">
                {product.description || 'La tienda no agregó una descripción detallada para este producto.'}
              </p>
            </section>

            {/* Características principales orientadas al comprador */}
            <section className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="font-display text-lg font-bold text-stone-950">
                  Características principales
                </h2>
                {selected?.presentationName && (
                  <span className="rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-semibold text-stone-700">
                    {selected.presentationName}
                  </span>
                )}
              </div>

              {selected?.attributes && selected.attributes.length > 0 && (
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 transition-opacity duration-150 motion-reduce:transition-none">
                  {selected.attributes.map((attribute) => (
                    <div
                      key={`${selected.id}-${attribute.name}`}
                      className="rounded-xl border border-stone-200/70 bg-stone-50/80 px-3.5 py-2.5"
                    >
                      <dt className="text-[11px] font-bold uppercase tracking-wider text-stone-500">
                        {attribute.name}
                      </dt>
                      <dd className="text-sm font-semibold text-stone-900 mt-0.5">{attribute.value}</dd>
                    </div>
                  ))}
                </dl>
              )}

              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs sm:text-sm">
                <div className="rounded-xl border border-stone-200/70 bg-white px-3.5 py-2.5">
                  <dt className="text-[11px] font-bold uppercase tracking-wider text-stone-500">
                    Disponibilidad
                  </dt>
                  <dd className="font-semibold text-stone-900 mt-0.5">
                    {!selected
                      ? 'Sin presentaciones activas'
                      : !selected.tracksInventory
                        ? 'Preparación bajo pedido'
                        : typeof selected.availableQuantity === 'number'
                          ? selected.availableQuantity <= 0
                            ? 'Agotado temporalmente'
                            : selected.availableQuantity <= 5
                              ? `¡Últimas ${selected.availableQuantity} unidades!`
                              : `${selected.availableQuantity} unidades disponibles`
                          : 'Stock verificado por la tienda'}
                  </dd>
                </div>

                <div className="rounded-xl border border-stone-200/70 bg-white px-3.5 py-2.5">
                  <dt className="text-[11px] font-bold uppercase tracking-wider text-stone-500">
                    Modalidad de entrega
                  </dt>
                  <dd className="font-semibold text-stone-900 mt-0.5">
                    {store.deliveryEnabled !== false && store.pickupEnabled !== false
                      ? 'Delivery y recojo en tienda'
                      : store.pickupEnabled
                        ? 'Solo recojo en tienda'
                        : 'Solo delivery'}
                  </dd>
                </div>

                {(product.prepTimeMinutes || storeSettings?.estimatedPrepTimeMinutes) && (
                  <div className="rounded-xl border border-stone-200/70 bg-white px-3.5 py-2.5">
                    <dt className="text-[11px] font-bold uppercase tracking-wider text-stone-500">
                      Tiempo de preparación
                    </dt>
                    <dd className="font-semibold text-stone-900 mt-0.5">
                      ~{product.prepTimeMinutes ?? storeSettings?.estimatedPrepTimeMinutes} min aprox.
                    </dd>
                  </div>
                )}

                {storeSettings && (
                  <div className="rounded-xl border border-stone-200/70 bg-white px-3.5 py-2.5">
                    <dt className="text-[11px] font-bold uppercase tracking-wider text-stone-500">
                      Medios de pago
                    </dt>
                    <dd className="font-semibold text-stone-900 mt-0.5">
                      {[
                        storeSettings.onlinePaymentsEnabled ? 'Pago en línea' : null,
                        storeSettings.cashOnDeliveryEnabled ? 'Efectivo' : null,
                        storeSettings.acceptsPosOnDelivery ? 'POS contra entrega' : null,
                      ]
                        .filter(Boolean)
                        .join(' · ') || 'Pago en línea'}
                    </dd>
                  </div>
                )}
              </dl>
            </section>

            {/* Reseñas con flujo autenticado 1 por compra completada */}
            <ProductReviewsSection
              productId={product.id}
              productName={product.name}
              reviewsSummary={reviewsSummary}
              onReviewsChanged={refreshReviews}
            />
          </div>

          {/* Columna Derecha Sticky: Buy Box + Tienda */}
          <aside className="lg:col-span-5 lg:sticky lg:top-24 space-y-4 min-w-0">
            <ProductBuyBox
              store={store}
              product={product}
              variants={variants}
              selectedVariant={selected}
              onSelectVariant={handleSelectVariant}
              quantity={quantity}
              onChangeQuantity={setQuantity}
              inCartQty={inCartQty}
              onAddToCart={addToCart}
              pending={pending}
              reviewsSummary={reviewsSummary}
              liveAnnouncement={liveAnnouncement}
            />

            <StoreTrustCard store={store} storeSettings={storeSettings} />
          </aside>
        </div>

        {/* Productos relacionados de la misma tienda */}
        <RelatedStoreProducts store={store} products={relatedProducts} />
      </Container>

      {/* Barra fija inferior móvil */}
      <div className="lg:hidden fixed bottom-0 inset-x-0 z-30 border-t border-[#1e2c31] bg-[#02090a]/95 px-4 py-3 text-white backdrop-blur-md shadow-2xl">
        <div className="mx-auto flex max-w-lg items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="text-[11px] text-[#99b3ad] truncate">
              {selected ? selected.presentationName : product.name}
            </p>
            <p className="font-display text-lg font-black text-white tabular-nums">
              {mobileDisplayPrice}
            </p>
          </div>
          <button
            type="button"
            onClick={addToCart}
            disabled={pending || isOutOfStock || (variants.length > 0 && !selected)}
            className="min-h-[44px] inline-flex items-center justify-center gap-2 rounded-full bg-white px-5 py-2.5 text-xs font-bold text-[#02090a] transition hover:bg-stone-100 active:scale-[0.99] disabled:opacity-50 cursor-pointer shrink-0"
          >
            <ShoppingCart className="h-4 w-4 text-[#005141]" />
            <span>{isOutOfStock ? 'Agotado' : 'Añadir'}</span>
          </button>
        </div>
      </div>
    </div>
  );
}

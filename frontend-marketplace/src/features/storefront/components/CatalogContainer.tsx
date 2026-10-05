'use client';

import React, { useState, useEffect } from 'react';
import { Clock } from 'lucide-react';
import { CapacitySlotPicker, CapacitySlot } from './CapacitySlotPicker';
import { StorefrontNav } from './StorefrontNav';
import {
  ProductGrid,
  CatalogFilterBar,
  CatalogEmptyState,
} from './catalog';
import { ProductItem, ProductVariantItem, CartItem, PublicStore } from '../types/storefront.types';
import { StorefrontBadges } from './StorefrontBadges';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';

export type { ProductItem, ProductVariantItem, CartItem };

interface CatalogContainerProps {
  storeSlug?: string;
  storeName: string;
  store?: PublicStore;
  products?: ProductItem[] | null;
  slots?: CapacitySlot[] | null;
  catalogError?: boolean;
  capacityError?: boolean;
  capacitySlot?: React.ReactNode;
}

export function CatalogContainer({
  storeName,
  store,
  products,
  slots,
  catalogError = false,
  capacityError = false,
  capacitySlot,
}: CatalogContainerProps) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const { items: cart, count, store: cartStore, changeQuantity, selectStore, pending, loading } = useStorefrontCart();
  const totalCartCount = cartStore?.id === store?.id ? count : 0;
  useEffect(() => {
    if (store) selectStore({ id: store.id, name: store.name, slug: store.slug });
  }, [store, selectStore]);
  const [selectedSlot, setSelectedSlot] = useState<CapacitySlot | null>(null);
  const [fulfillment, setFulfillment] = useState<'DELIVERY' | 'PICKUP'>('DELIVERY');

  const items = products ?? [];
  const contactNumber = (store?.whatsappNumber || store?.phone)?.replace(/\D/g, '');
  const contactUrl = contactNumber ? `https://wa.me/${contactNumber}?text=${encodeURIComponent(`Hola ${storeName}, quisiera consultar sus productos.`)}` : null;
  const categories = [...new Map(items.filter((product) => product.category).map((product) => [product.categoryId || product.category, {
    id: product.categoryId || product.category,
    name: product.category,
    slug: product.categoryId || product.category,
    itemCount: items.filter((item) => (item.categoryId || item.category) === (product.categoryId || product.category)).length,
  }])).values()];
  const isFiltering = query.trim().length > 0 || category !== 'all';

  const filtered = items.filter(
    (product) =>
      (category === 'all' || (product.categoryId || product.category) === category) &&
      (product.name + ' ' + (product.description || ''))
        .toLocaleLowerCase()
        .includes(query.toLocaleLowerCase())
  );

  const capacityTitle = capacityError
    ? 'No pudimos consultar los horarios'
    : slots == null
    ? 'Horarios de pedido no disponibles'
    : 'No hay horarios disponibles';

  const capacityMessage = capacityError
    ? 'Recarga la página para volver a consultar la disponibilidad. No se ha reservado ningún cupo.'
    : slots == null
    ? `Aún no podemos consultar los cupos de ${storeName}. No es posible reservar un horario desde esta página.`
    : 'La tienda no tiene horarios disponibles para seleccionar. Vuelve a consultar más adelante.';

  return (
    <div className="space-y-8 font-sans">
      <section id="catalogo" className="scroll-mt-40">
        {/* Barra Superior con Título, Contador, Búsqueda y Botón de Carrito */}
        <CatalogFilterBar
          storeName={storeName}
          query={query}
          onQueryChange={setQuery}
          totalProductsCount={items.length}
        />

        {catalogError ? (
          <CatalogEmptyState
            type="error"
            storeName={storeName}
            onReset={() => window.location.reload()}
          />
        ) : products == null ? (
          <CatalogEmptyState type="unavailable" storeName={storeName} />
        ) : items.length === 0 ? (
          <CatalogEmptyState type="no-products" storeName={storeName} />
        ) : (
          <>
            {/* Navegación por Categorías de la Tienda */}
            {categories.length > 0 && (
              <StorefrontNav
                categories={[
                  {
                    id: 'all',
                    name: 'Todos los productos',
                    slug: 'todos',
                    itemCount: items.length,
                  },
                  ...categories,
                ]}
                activeCategoryId={category}
                onSelectCategory={setCategory}
              />
            )}
            {items.some((product) => product.categoryId && !product.category) && <p role="status" className="mt-3 text-sm text-stone-600">Algunas categorías no están disponibles. Puedes explorar todos los productos.</p>}

            {/* Cuadrícula de Productos a Ancho Completo */}
            <div className="mt-6">
              <ProductGrid
                products={filtered}
                onAdd={(p) => changeQuantity(p, 1)}
                cart={cart}
                storeName={storeName}
                onResetFilters={() => {
                  setQuery('');
                  setCategory('all');
                }}
                isFiltering={isFiltering}
                pending={pending || loading || cartStore?.id !== store?.id}
              />
            </div>

          </>
        )}
      </section>

      {(catalogError || items.length === 0) && contactUrl && <div className="-mt-4 text-center">
        <a href={contactUrl} target="_blank" rel="noopener noreferrer" className="inline-flex rounded-2xl bg-[#005141] px-5 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-[#00382d]">Consultar productos por WhatsApp</a>
      </div>}

      {/* Selector de Horarios y Capacidad Operativa */}
      {totalCartCount > 0 && <div id="entrega" className="space-y-4 scroll-mt-28">
        <h2 className="font-display text-2xl font-bold text-stone-900">Entrega y horarios</h2>
        {store && <StorefrontBadges store={store} />}
        {store?.pickupEnabled && store.address && <p className="text-sm text-stone-600">Recojo en {store.address}. <a href="#ubicacion" className="font-semibold text-[#005141] hover:underline">Ver cómo llegar</a></p>}
        {capacitySlot ??
        (slots && slots.length > 0 && !capacityError ? (
          <CapacitySlotPicker
            slots={slots}
            selectedSlotId={selectedSlot?.id}
            onSelectSlot={setSelectedSlot}
            fulfillmentType={fulfillment}
            onFulfillmentTypeChange={setFulfillment}
          />
        ) : (
          <section className="rounded-3xl border border-stone-200/90 bg-white p-6 sm:p-8 shadow-xs">
            <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-stone-900">
              <Clock aria-hidden="true" className="h-5 w-5 text-[#005141]" />
              {capacityTitle}
            </h2>
            <p className="mt-2.5 max-w-2xl text-sm leading-relaxed text-stone-600">
              {capacityMessage}
            </p>
          </section>
        ))}
      </div>}
    </div>
  );
}

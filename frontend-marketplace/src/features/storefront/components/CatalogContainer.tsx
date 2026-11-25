'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import Link from 'next/link';
import { ArrowRight, ShoppingBag, Store } from 'lucide-react';
import { CapacitySlot } from './CapacitySlotPicker';
import { StorefrontNav } from './StorefrontNav';
import {
  ProductGrid,
  CatalogFilterBar,
  CatalogEmptyState,
} from './catalog';
import { PaginationBar } from '@/shared/components/ui/PaginationBar';
import { ProductItem, ProductVariantItem, CartItem, PublicStore, PublicStoreSettings } from '../types/storefront.types';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';
import { useCustomerFavorites } from '@/features/customer/hooks/useCustomerFavorites';
import { useCustomerProfile } from '@/features/customer/hooks/useCustomerProfile';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';

export type { ProductItem, ProductVariantItem, CartItem };

interface CatalogContainerProps {
  storeSlug?: string;
  storeName: string;
  store?: PublicStore;
  storeSettings?: PublicStoreSettings | null;
  products?: ProductItem[] | null;
  slots?: CapacitySlot[] | null;
  catalogError?: boolean;
  capacityError?: boolean;
  capacitySlot?: React.ReactNode;
}

export const PRODUCTS_PAGE_SIZE = 12;

export function CatalogContainer({
  storeSlug,
  storeName,
  store,
  products,
  catalogError = false,
}: CatalogContainerProps) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const catalogSectionRef = useRef<HTMLElement>(null);

  const handleQueryChange = useCallback((nextQuery: string) => {
    setQuery(nextQuery);
    setCurrentPage(1);
  }, []);

  const handleCategoryChange = useCallback((nextCategory: string) => {
    setCategory(nextCategory);
    setCurrentPage(1);
  }, []);

  const handlePageChange = useCallback((nextPage: number) => {
    setCurrentPage(nextPage);
    if (typeof catalogSectionRef.current?.scrollIntoView === 'function') {
      catalogSectionRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, []);
  const {
    items: cart,
    count,
    store: cartStore,
    changeQuantity,
    selectStore,
    pending,
    loading,
    subtotal,
    currency,
    authenticated,
  } = useStorefrontCart();

  const totalCartCount = cartStore?.id === store?.id ? count : 0;
  useEffect(() => {
    if (store?.slug) {
      selectStore({ id: store.id, name: store.name, slug: store.slug });
    }
  }, [store, selectStore]);

  const { isFavorite, toggleFavorite } = useCustomerFavorites();
  const { data: customerProfile } = useCustomerProfile(authenticated);
  const isOwnStore = Boolean(
    store?.id && customerProfile?.ownedStoreId && customerProfile.ownedStoreId === store.id
  );

  const handleToggleFavorite = useCallback(
    async (productId: string) => {
      if (!authenticated) {
        toast.error('Inicia sesión para guardar productos en tus favoritos');
        return;
      }
      try {
        const added = await toggleFavorite(productId);
        if (added) {
          toast.success('Producto añadido a tus favoritos');
        } else {
          toast.info('Producto eliminado de tus favoritos');
        }
      } catch {
        toast.error('No se pudo actualizar favoritos');
      }
    },
    [authenticated, toggleFavorite]
  );

  const items = products ?? [];
  const contactNumber = (store?.whatsappNumber || store?.phone)?.replace(/\D/g, '');
  const contactUrl = contactNumber
    ? `https://wa.me/${contactNumber}?text=${encodeURIComponent(
        `Hola ${storeName}, quisiera consultar sus productos.`
      )}`
    : null;

  const categories = [
    ...new Map(
      items
        .filter((product) => product.category)
        .map((product) => [
          product.categoryId || product.category,
          {
            id: product.categoryId || product.category,
            name: product.category,
            slug: product.categoryId || product.category,
            itemCount: items.filter(
              (item) =>
                (item.categoryId || item.category) ===
                (product.categoryId || product.category)
            ).length,
          },
        ])
    ).values(),
  ];

  const isFiltering = query.trim().length > 0 || category !== 'all';

  const filtered = items.filter(
    (product) =>
      (category === 'all' || (product.categoryId || product.category) === category) &&
      (product.name + ' ' + (product.description || ''))
        .toLocaleLowerCase()
        .includes(query.toLocaleLowerCase())
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / PRODUCTS_PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const startIndex = (safePage - 1) * PRODUCTS_PAGE_SIZE;
  const paginatedProducts = filtered.slice(startIndex, startIndex + PRODUCTS_PAGE_SIZE);

  const checkoutHref = store?.slug
    ? `/checkout?store=${encodeURIComponent(store.slug)}`
    : '/carrito';

  return (
    <div className="space-y-8 font-sans">
      <section id="catalogo" ref={catalogSectionRef} className="scroll-mt-40">
        {/* Barra Superior con Título, Contador, Búsqueda y Botón de Carrito */}
        <CatalogFilterBar
          storeName={storeName}
          query={query}
          onQueryChange={handleQueryChange}
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
                onSelectCategory={handleCategoryChange}
              />
            )}
            {items.some((product) => product.categoryId && !product.category) && (
              <p role="status" className="mt-3 text-sm text-stone-600">
                Algunas categorías no están disponibles. Puedes explorar todos los productos.
              </p>
            )}

            {/* Cuadrícula de Productos a Ancho Completo */}
            <div className="mt-6 space-y-4">
              {isOwnStore && (
                <div
                  role="status"
                  className="rounded-[12px] border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-900 flex items-center gap-2.5"
                >
                  <Store className="w-4 h-4 text-amber-700 shrink-0" aria-hidden="true" />
                  <span>
                    Estás viendo tu propia tienda. No puedes agregar ni comprar tus propios productos.
                  </span>
                </div>
              )}

              <ProductGrid
                storeSlug={storeSlug ?? store?.slug}
                products={paginatedProducts}
                onAdd={(p) => {
                  if (isOwnStore) {
                    toast.error('No puedes agregar productos de tu propia tienda al carrito.');
                    return;
                  }
                  void changeQuantity(p, 1);
                }}
                cart={cart}
                storeName={storeName}
                onResetFilters={() => {
                  handleQueryChange('');
                  handleCategoryChange('all');
                }}
                isFiltering={isFiltering}
                pending={pending || loading || cartStore?.id !== store?.id || isOwnStore}
                isFavorite={(productId) => isFavorite(productId)}
                onToggleFavorite={handleToggleFavorite}
              />

              <PaginationBar
                currentPage={safePage}
                totalPages={totalPages}
                totalItems={filtered.length}
                pageSize={PRODUCTS_PAGE_SIZE}
                itemLabel="productos"
                onPageChange={handlePageChange}
                ariaLabel="Paginación de productos de la tienda"
              />
            </div>
          </>
        )}
      </section>

      {(catalogError || items.length === 0) && contactUrl && (
        <div className="-mt-4 text-center">
          <a
            href={contactUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex rounded-full bg-[#005141] px-6 py-3 text-sm font-bold text-white shadow-xs transition hover:bg-[#00382d] active:scale-95"
          >
            Consultar productos por WhatsApp
          </a>
        </div>
      )}

      {/* Sticky Mobile Cart Bar */}
      {totalCartCount > 0 && (
        <aside
          aria-label="Resumen móvil del pedido"
          className="fixed bottom-0 left-0 right-0 z-40 sm:hidden bg-white/95 backdrop-blur-md border-t border-stone-200 px-4 py-3 shadow-lg flex items-center justify-between gap-3"
        >
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-emerald-50 text-[#005141] flex items-center justify-center font-bold text-xs border border-emerald-200">
              <ShoppingBag className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[10px] font-bold text-stone-500 uppercase tracking-wider block">
                {totalCartCount} {totalCartCount === 1 ? 'producto' : 'productos'}
              </span>
              <span className="font-mono text-sm font-black text-[#005141] tabular-nums">
                {formatCurrency(subtotal, currency)}
              </span>
            </div>
          </div>
          <Link
            href={checkoutHref}
            className="px-4 py-2 rounded-full bg-[#005141] hover:bg-[#00382d] text-white font-bold text-xs shadow-md shadow-[#005141]/20 flex items-center gap-1.5 active:scale-95 transition-all"
          >
            <span>Ir al checkout</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </aside>
      )}
    </div>
  );
}

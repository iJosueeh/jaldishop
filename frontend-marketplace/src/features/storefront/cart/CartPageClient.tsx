'use client';

import React, { useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQueries, useQuery } from '@tanstack/react-query';
import {
  Store,
  ShoppingBag,
  ArrowLeft,
  ArrowRight,
  Plus,
  Minus,
  Trash2,
  AlertTriangle,
  Check,
  Search,
  Clock,
  CreditCard,
  ShieldCheck,
} from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { PaginationBar } from '@/shared/components/ui/PaginationBar';
import { useStorefrontCart, ActiveStore } from './StorefrontCartProvider';
import { cartService, CartResponse } from './services/cartService';
import { storeService } from '../services/storeService';
import { estimateServiceFee } from '@/features/checkout/utils/pricing';
import { formatCurrency } from '@/shared/utils/formatters';

export const STORES_PAGE_SIZE = 3;

interface StoreCartEntry {
  store: ActiveStore;
  cart: CartResponse | undefined;
  isLoading: boolean;
}

export function CartPageClient() {
  const router = useRouter();
  const [currentPage, setCurrentPage] = useState(1);
  const [storeQuery, setStoreQuery] = useState('');

  const {
    store: activeStore,
    recentStores,
    selectStore,
    authenticated,
    loading,
    pending,
    error,
    retry,
    clear,
    changeQuantity,
    removeItem,
  } = useStorefrontCart();

  // Fetch cart contents for every store in recentStores
  const storeCartQueries = useQueries({
    queries: recentStores.map((entry) => ({
      queryKey: ['customer-cart', entry.id],
      queryFn: () => cartService.get(entry.id),
      enabled: authenticated,
      retry: false,
      staleTime: 0,
    })),
  });

  const storeEntries: StoreCartEntry[] = useMemo(
    () =>
      recentStores.map((entry, idx) => ({
        store: entry,
        cart: storeCartQueries[idx]?.data,
        isLoading: Boolean(storeCartQueries[idx]?.isLoading),
      })),
    [recentStores, storeCartQueries]
  );

  const populatedStores = useMemo(
    () => storeEntries.filter((entry) => (entry.cart?.totalItems ?? 0) > 0),
    [storeEntries]
  );

  const filteredStores = useMemo(() => {
    const q = storeQuery.trim().toLowerCase();
    if (!q) return populatedStores;
    return populatedStores.filter((entry) => entry.store.name.toLowerCase().includes(q));
  }, [populatedStores, storeQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredStores.length / STORES_PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const paginatedStores = useMemo(() => {
    const start = (safePage - 1) * STORES_PAGE_SIZE;
    return filteredStores.slice(start, start + STORES_PAGE_SIZE);
  }, [filteredStores, safePage]);

  // Selected store for checkout (persists across pagination)
  const selectedEntry =
    populatedStores.find((entry) => entry.store.id === activeStore?.id) ?? null;

  const selectedStoreDetailsQuery = useQuery({
    queryKey: ['cart-selected-store', selectedEntry?.store.slug],
    queryFn: () => storeService.getStoreBySlugClient(selectedEntry!.store.slug),
    enabled: Boolean(selectedEntry?.store.slug),
    staleTime: 60_000,
    retry: false,
  });

  const selectedStoreSettingsQuery = useQuery({
    queryKey: ['cart-selected-store-settings', selectedEntry?.store.id],
    queryFn: () => storeService.getPublicStoreSettingsClient(selectedEntry!.store.id),
    enabled: Boolean(selectedEntry?.store.id),
    staleTime: 60_000,
    retry: false,
  });

  const selectedStoreInfo = selectedStoreDetailsQuery.data;
  const selectedStoreSettings = selectedStoreSettingsQuery.data;

  const selectedCart = selectedEntry?.cart;
  const selectedCount = selectedCart?.totalItems ?? 0;
  const selectedSubtotal = Number(selectedCart?.totalAmount ?? 0);
  const selectedCurrency = selectedCart?.currency || 'PEN';
  const selectedHasUnavailable = Boolean(
    selectedCart?.items.some((item) => item.available === false)
  );

  const platformSettingsQuery = useQuery({
    queryKey: ['public-platform-settings'],
    queryFn: async () => {
      const res = await fetch('/api/platform-settings', { cache: 'no-store' });
      if (!res.ok) return { customerServiceFeeRate: 0, customerServiceFeeCap: null };
      return (await res.json()) as { customerServiceFeeRate: number; customerServiceFeeCap?: number | null };
    },
    staleTime: 0,
    retry: false,
  });

  const serviceFeeRate = platformSettingsQuery.data?.customerServiceFeeRate ?? 0;
  const serviceFeeCap = platformSettingsQuery.data?.customerServiceFeeCap ?? null;

  const estimatedDeliveryFee =
    selectedCount > 0 && selectedStoreInfo?.deliveryEnabled
      ? Number(selectedStoreInfo.deliveryFeeAmount ?? 0)
      : 0;
  const platformCommission =
    selectedCount > 0 ? estimateServiceFee(selectedSubtotal, serviceFeeRate, serviceFeeCap) : 0;
  const estimatedTotal = selectedSubtotal + estimatedDeliveryFee + platformCommission;

  const minDeliveryAmount = selectedStoreSettings?.minDeliveryAmount ?? null;
  const missingForMinDelivery =
    selectedCount > 0 &&
    selectedStoreInfo?.deliveryEnabled &&
    minDeliveryAmount !== null &&
    minDeliveryAmount > 0 &&
    selectedSubtotal < minDeliveryAmount
      ? minDeliveryAmount - selectedSubtotal
      : 0;

  const handleToggleStoreCheckbox = (entry: StoreCartEntry) => {
    selectStore(entry.store);
  };

  const handleUpdateQuantity = async (
    targetStore: ActiveStore,
    item: CartResponse['items'][number],
    delta: number
  ) => {
    if (activeStore?.id !== targetStore.id) {
      selectStore(targetStore);
    }
    await changeQuantity(
      {
        id: item.productId,
        variantId: item.variantId,
        name: item.productName + (item.presentationName ? ' — ' + item.presentationName : ''),
        description: '',
        category: '',
        price: Number(item.unitPriceAmount),
        priceCurrency: item.unitPriceCurrency,
        imageUrl: item.imageUrl,
      },
      delta
    );
  };

  const handleRemoveItem = async (targetStore: ActiveStore, variantId: string) => {
    if (activeStore?.id !== targetStore.id) {
      selectStore(targetStore);
    }
    await removeItem(variantId);
  };

  const handleClearStore = async (targetStore: ActiveStore) => {
    if (activeStore?.id !== targetStore.id) {
      selectStore(targetStore);
    }
    await clear();
  };

  return (
    <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-16">
      <Container size="lg" className="space-y-8">
        {/* Editorial Page Header */}
        <header className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#e7e0d6] pb-6">
          <div className="space-y-1.5">
            <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#005141] block">
              Carrito aislado por tienda
            </span>
            <h1 className="font-display text-3xl sm:text-4xl font-semibold tracking-tight text-[#1c1917]">
              Tu carrito de compras
            </h1>
          </div>
          <Link
            href="/tiendas"
            className="inline-flex items-center gap-2 text-sm font-medium text-[#005141] hover:underline"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            <span>Explorar tiendas</span>
          </Link>
        </header>

        {error && (
          <div
            role="alert"
            className="rounded-[12px] border border-rose-300 bg-rose-50 p-4 text-sm text-rose-800 flex items-center justify-between gap-4"
          >
            <span>{error}</span>
            <button
              type="button"
              onClick={retry}
              className="rounded-full border border-rose-300 bg-white px-4 py-1.5 text-xs font-semibold text-rose-800 hover:bg-rose-100 cursor-pointer"
            >
              Reintentar
            </button>
          </div>
        )}

        {loading && !authenticated ? (
          <div
            role="status"
            className="max-w-md mx-auto rounded-[12px] border border-[#e7e0d6] bg-white p-8 text-center text-sm text-[#57534e]"
          >
            Verificando sesión...
          </div>
        ) : !authenticated ? (
          <div className="max-w-md mx-auto rounded-[12px] border border-[#e7e0d6] bg-white p-7 sm:p-9 text-center space-y-4">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#f0fdfa] border border-[#ccfbf1]">
              <ShoppingBag className="h-6 w-6 text-[#005141]" aria-hidden="true" />
            </div>
            <h2 className="font-display text-xl font-semibold text-[#1c1917]">
              Inicia sesión para ver tus carritos
            </h2>
            <p className="text-sm text-[#57534e] leading-relaxed">
              Necesitas una sesión activa de cliente para visualizar los carritos de cada tienda, seleccionar tu comercio y continuar al checkout.
            </p>
            <div className="pt-2 flex flex-col gap-2.5">
              <Link
                href="/login?redirect=/carrito"
                className="w-full rounded-full bg-[#005141] px-6 py-3 text-sm font-semibold text-white hover:bg-[#003d31] transition-colors"
              >
                Ingresar como cliente
              </Link>
              <Link
                href="/registro?redirect=/carrito"
                className="w-full rounded-full border border-[#e7e0d6] px-6 py-2.5 text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] transition-colors"
              >
                ¿No tienes cuenta? Regístrate aquí
              </Link>
            </div>
          </div>
        ) : (
          /* Two-Column Workspace: Left = Paginated Store Carts + Items, Right = Essential Checkout Summary */
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column (8 cols): Store Carts & Items */}
            <section
              aria-label="Productos por tienda"
              className="lg:col-span-8 rounded-[12px] border border-[#e7e0d6] bg-white overflow-hidden"
            >
              <div className="px-5 sm:px-6 py-4 border-b border-[#e7e0d6] bg-[#f7f3ec]/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <h2 className="text-xs font-semibold uppercase tracking-[0.06em] text-[#57534e] flex items-center gap-2">
                  <Store className="h-4 w-4 text-[#005141]" aria-hidden="true" />
                  <span>
                    Productos por tienda ({populatedStores.length})
                  </span>
                </h2>

                {populatedStores.length > STORES_PAGE_SIZE ? (
                  <div className="relative w-full sm:w-56">
                    <Search className="h-3.5 w-3.5 text-[#78716c] absolute left-3 top-2.5 pointer-events-none" aria-hidden="true" />
                    <input
                      type="search"
                      value={storeQuery}
                      onChange={(e) => {
                        setStoreQuery(e.target.value);
                        setCurrentPage(1);
                      }}
                      placeholder="Buscar tienda..."
                      aria-label="Filtrar carritos por tienda"
                      className="w-full rounded-full border border-[#e7e0d6] bg-white pl-8 pr-3 py-1.5 text-xs text-[#1c1917] placeholder-[#78716c] focus:border-[#005141] focus:outline-none"
                    />
                  </div>
                ) : (
                  <span className="text-xs text-[#78716c]">
                    Marca la tienda que deseas pagar
                  </span>
                )}
              </div>

              {storeEntries.some((e) => e.isLoading) && populatedStores.length === 0 ? (
                <div role="status" className="p-10 text-center text-sm text-[#57534e]">
                  Consultando tus carritos...
                </div>
              ) : populatedStores.length === 0 ? (
                <div className="p-10 sm:p-14 text-center space-y-3">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#faf7f2] border border-[#e7e0d6]">
                    <ShoppingBag className="h-5 w-5 text-[#78716c]" aria-hidden="true" />
                  </div>
                  <h3 className="font-display text-lg font-semibold text-[#1c1917]">
                    No hay ningún elemento en tu carrito
                  </h3>
                  <p className="text-sm text-[#57534e] max-w-sm mx-auto">
                    Cuando agregues productos desde una tienda, aparecerán aquí organizados por comercio.
                  </p>
                  <div className="pt-2">
                    <Link
                      href="/tiendas"
                      className="inline-flex items-center gap-2 rounded-full bg-[#005141] px-5 py-2.5 text-xs font-semibold text-white hover:bg-[#003d31]"
                    >
                      <span>Explorar tiendas</span>
                      <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                    </Link>
                  </div>
                </div>
              ) : filteredStores.length === 0 ? (
                <div className="p-10 text-center space-y-2">
                  <p className="text-sm font-medium text-[#1c1917]">
                    No encontramos carritos para &ldquo;{storeQuery}&rdquo;
                  </p>
                  <button
                    type="button"
                    onClick={() => setStoreQuery('')}
                    className="text-xs font-semibold text-[#005141] underline cursor-pointer"
                  >
                    Ver todas las tiendas
                  </button>
                </div>
              ) : (
                <>
                  <div className="divide-y divide-[#e7e0d6]">
                    {paginatedStores.map((entry) => {
                      const storeCart = entry.cart!;
                      const isChecked = selectedEntry?.store.id === entry.store.id;
                      const hasUnavailable = storeCart.items.some((i) => i.available === false);

                      return (
                        <article
                          key={entry.store.id}
                          aria-label={`Carrito de ${entry.store.name}`}
                          className={`p-5 sm:p-6 transition-colors ${
                            isChecked ? 'bg-[#f0fdfa]/50' : 'bg-white'
                          }`}
                        >
                          {/* Store Row Header + Corner Checkbox */}
                          <div className="flex items-start justify-between gap-4 pb-4 border-b border-[#e7e0d6]/70">
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="h-9 w-9 shrink-0 rounded-[8px] bg-[#072720] text-[#36f4a4] flex items-center justify-center">
                                <Store className="h-4 w-4" aria-hidden="true" />
                              </div>
                              <div className="min-w-0">
                                <div className="flex items-center gap-2 flex-wrap">
                                  <Link
                                    href={`/tienda/${entry.store.slug}`}
                                    className="font-display text-base font-semibold text-[#1c1917] hover:text-[#005141] hover:underline truncate"
                                  >
                                    {entry.store.name}
                                  </Link>
                                  <span className="rounded-full bg-[#faf7f2] border border-[#e7e0d6] px-2.5 py-0.5 text-[11px] font-medium text-[#57534e]">
                                    {storeCart.totalItems}{' '}
                                    {storeCart.totalItems === 1 ? 'producto' : 'productos'}
                                  </span>
                                </div>
                                <button
                                  type="button"
                                  onClick={() => void handleClearStore(entry.store)}
                                  disabled={pending}
                                  className="mt-0.5 text-xs text-[#78716c] hover:text-rose-600 underline cursor-pointer disabled:opacity-40"
                                >
                                  Vaciar tienda
                                </button>
                              </div>
                            </div>

                            {/* Corner Checkout Selection Checkbox */}
                            <label className="inline-flex items-center gap-2 cursor-pointer select-none shrink-0 rounded-full border border-[#e7e0d6] bg-white px-3 py-1.5 hover:border-[#005141]/50">
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={() => handleToggleStoreCheckbox(entry)}
                                aria-label={`Seleccionar ${entry.store.name} para checkout`}
                                className="sr-only"
                              />
                              <span
                                aria-hidden="true"
                                className={`flex h-4 w-4 items-center justify-center rounded-[4px] border transition-colors ${
                                  isChecked
                                    ? 'border-[#005141] bg-[#005141] text-white'
                                    : 'border-[#78716c]/50 bg-white'
                                }`}
                              >
                                {isChecked && <Check className="h-3 w-3 stroke-[3]" />}
                              </span>
                              <span className="text-xs font-semibold text-[#1c1917]">
                                {isChecked ? 'Seleccionada' : 'Seleccionar'}
                              </span>
                            </label>
                          </div>

                          {hasUnavailable && (
                            <div
                              role="alert"
                              className="mt-4 rounded-[8px] border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900 flex items-center gap-2"
                            >
                              <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" aria-hidden="true" />
                              <span>
                                Hay productos sin stock en {entry.store.name}. Retíralos para habilitar el checkout.
                              </span>
                            </div>
                          )}

                          {/* Store Items List */}
                          <ul className="mt-4 divide-y divide-[#e7e0d6]/60">
                            {storeCart.items.map((item) => {
                              const displayName =
                                item.productName +
                                (item.presentationName ? ` — ${item.presentationName}` : '');
                              const isUnavailable = item.available === false;

                              return (
                                <li
                                  key={item.variantId}
                                  className="py-3.5 first:pt-0 last:pb-0 flex items-start gap-3.5"
                                >
                                  <div className="h-14 w-14 shrink-0 rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] overflow-hidden flex items-center justify-center">
                                    {item.imageUrl ? (
                                      <img
                                        src={item.imageUrl}
                                        alt={displayName}
                                        className="h-full w-full object-cover"
                                      />
                                    ) : (
                                      <ShoppingBag
                                        className="h-4 w-4 text-[#78716c]"
                                        aria-hidden="true"
                                      />
                                    )}
                                  </div>

                                  <div className="min-w-0 flex-1 space-y-1.5">
                                    <div className="flex items-start justify-between gap-3">
                                      <p className="text-sm font-semibold text-[#1c1917] leading-snug">
                                        {displayName}
                                      </p>
                                      <span className="text-sm font-mono font-bold text-[#1c1917] shrink-0 tabular-nums">
                                        {formatCurrency(
                                          Number(item.unitPriceAmount) * item.quantity,
                                          item.unitPriceCurrency || storeCart.currency
                                        )}
                                      </span>
                                    </div>

                                    {isUnavailable && (
                                      <p className="inline-flex items-center rounded-full bg-rose-50 border border-rose-200 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700">
                                        No disponible — retira este ítem
                                      </p>
                                    )}

                                    <div className="flex items-center justify-between gap-3 pt-0.5">
                                      <span className="text-xs text-[#57534e]">
                                        {formatCurrency(
                                          Number(item.unitPriceAmount),
                                          item.unitPriceCurrency || storeCart.currency
                                        )}{' '}
                                        c/u
                                      </span>

                                      <div className="flex items-center gap-2">
                                        <div className="flex items-center gap-1 bg-[#faf7f2] rounded-full px-1.5 py-0.5 border border-[#e7e0d6]">
                                          <button
                                            type="button"
                                            aria-label={`Quitar una unidad de ${displayName}`}
                                            disabled={pending}
                                            onClick={() =>
                                              void handleUpdateQuantity(entry.store, item, -1)
                                            }
                                            className="p-1.5 rounded-full hover:bg-[#e7e0d6]/60 text-[#1c1917] transition-colors cursor-pointer disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                                          >
                                            <Minus className="h-3 w-3" />
                                          </button>
                                          <span className="text-xs font-bold w-5 text-center text-[#1c1917] tabular-nums">
                                            {item.quantity}
                                          </span>
                                          <button
                                            type="button"
                                            aria-label={`Añadir una unidad de ${displayName}`}
                                            disabled={pending || isUnavailable}
                                            onClick={() =>
                                              void handleUpdateQuantity(entry.store, item, 1)
                                            }
                                            className="p-1.5 rounded-full hover:bg-[#e7e0d6]/60 text-[#1c1917] transition-colors cursor-pointer disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                                          >
                                            <Plus className="h-3 w-3" />
                                          </button>
                                        </div>

                                        <button
                                          type="button"
                                          aria-label={`Eliminar ${displayName}`}
                                          disabled={pending}
                                          onClick={() =>
                                            void handleRemoveItem(entry.store, item.variantId)
                                          }
                                          className="p-1.5 rounded-full text-[#78716c] hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
                                        >
                                          <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                      </div>
                                    </div>
                                  </div>
                                </li>
                              );
                            })}
                          </ul>
                        </article>
                      );
                    })}
                  </div>

                  <PaginationBar
                    currentPage={safePage}
                    totalPages={totalPages}
                    totalItems={filteredStores.length}
                    pageSize={STORES_PAGE_SIZE}
                    itemLabel="tiendas"
                    onPageChange={setCurrentPage}
                    ariaLabel="Paginación de carritos por tienda"
                    className="border-t border-[#e7e0d6] py-5 mt-0 bg-[#f7f3ec]/30"
                  />
                </>
              )}
            </section>

            {/* Right Column (4 cols): Always-visible Essential Summary */}
            <aside
              aria-label="Resumen de compra"
              aria-live="polite"
              className="lg:col-span-4 rounded-[12px] border border-[#e7e0d6] bg-white p-5 sm:p-6 space-y-5 lg:sticky lg:top-28"
            >
              <div className="border-b border-[#e7e0d6] pb-3.5">
                <span className="text-[11px] font-semibold uppercase tracking-[0.06em] text-[#005141] block">
                  Resumen de compra
                </span>
                <h2 className="font-display text-lg font-semibold text-[#1c1917] mt-0.5">
                  {selectedEntry ? selectedEntry.store.name : 'Sin tienda seleccionada'}
                </h2>
              </div>

              {/* Economic Breakdown */}
              <div className="space-y-2.5 text-sm">
                <div className="flex items-center justify-between text-[#57534e]">
                  <span>Productos ({selectedCount})</span>
                  <span className="font-mono font-semibold text-[#1c1917] tabular-nums">
                    {formatCurrency(selectedSubtotal, selectedCurrency)}
                  </span>
                </div>

                <div className="flex items-center justify-between text-[#57534e]">
                  <span>Delivery referencial</span>
                  <span className="font-mono font-semibold text-[#1c1917] tabular-nums">
                    {selectedCount === 0
                      ? formatCurrency(0, selectedCurrency)
                      : selectedStoreDetailsQuery.isLoading
                      ? 'Calculando...'
                      : selectedStoreInfo?.deliveryEnabled
                      ? formatCurrency(estimatedDeliveryFee, selectedCurrency)
                      : 'Solo recojo'}
                  </span>
                </div>

                <div className="flex items-center justify-between text-[#57534e]">
                  <span>Tarifa de servicio</span>
                  <span className="font-mono font-semibold text-[#005141] tabular-nums">
                    {formatCurrency(platformCommission, selectedCurrency)}
                  </span>
                </div>

                <div className="pt-3 border-t border-[#e7e0d6] flex items-center justify-between text-base font-semibold">
                  <span className="text-[#1c1917]">Total estimado</span>
                  <span className="font-display text-xl font-bold text-[#005141] tabular-nums">
                    {formatCurrency(estimatedTotal, selectedCurrency)}
                  </span>
                </div>
              </div>

              {/* Essential Store Operational Info */}
              {selectedEntry && (selectedStoreInfo || selectedStoreSettings) && (
                <div className="rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] p-3.5 space-y-2 text-xs text-[#57534e]">
                  {selectedStoreSettings?.estimatedPrepTimeMinutes != null && (
                    <div className="flex items-center gap-2">
                      <Clock className="h-3.5 w-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                      <span>
                        Preparación estimada:{' '}
                        <strong className="text-[#1c1917]">
                          {selectedStoreSettings.estimatedPrepTimeMinutes} min
                        </strong>
                      </span>
                    </div>
                  )}

                  <div className="flex items-center gap-2">
                    <CreditCard className="h-3.5 w-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                    <span>
                      Pagos:{' '}
                      <strong className="text-[#1c1917]">
                        {[
                          selectedStoreSettings?.onlinePaymentsEnabled !== false && 'Mercado Pago',
                          selectedStoreSettings?.cashOnDeliveryEnabled && 'Contra entrega',
                          selectedStoreSettings?.acceptsPosOnDelivery && 'POS',
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </strong>
                    </span>
                  </div>

                  {selectedStoreSettings?.requireCustomerDni && (
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="h-3.5 w-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                      <span>El comercio solicita DNI/RUC en checkout.</span>
                    </div>
                  )}

                  {missingForMinDelivery > 0 && minDeliveryAmount !== null && (
                    <p className="text-amber-800 font-medium pt-1 border-t border-[#e7e0d6]">
                      Mínimo para delivery: {formatCurrency(minDeliveryAmount, selectedCurrency)} (te faltan{' '}
                      {formatCurrency(missingForMinDelivery, selectedCurrency)}; recojo en tienda sigue disponible).
                    </p>
                  )}
                </div>
              )}

              {selectedHasUnavailable && (
                <p role="alert" className="text-xs font-medium text-rose-700">
                  Retira los productos no disponibles para continuar al checkout.
                </p>
              )}

              <button
                type="button"
                disabled={
                  !selectedEntry ||
                  selectedCount === 0 ||
                  pending ||
                  selectedHasUnavailable
                }
                onClick={() => {
                  if (!selectedEntry) return;
                  router.push(`/checkout?store=${encodeURIComponent(selectedEntry.store.slug)}`);
                }}
                className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-[#005141] hover:bg-[#003d31] text-white font-semibold text-sm px-6 py-3 transition-all active:scale-[0.98] disabled:bg-stone-200 disabled:text-stone-500 disabled:cursor-not-allowed cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
              >
                <span>Continuar al checkout</span>
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </button>

              <p className="text-[11px] text-[#78716c] leading-relaxed">
                Sin cargos ocultos ni comisiones de plataforma. El detalle final (horario y entrega) se confirma antes de pagar.
              </p>
            </aside>
          </div>
        )}
      </Container>
    </div>
  );
}

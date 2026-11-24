'use client';

import React, { useMemo, useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  Store,
  X,
  SlidersHorizontal,
  LayoutGrid,
  List,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { PublicStore, StoreCategory } from '../types/storefront.types';
import { MarketplaceStoreCard } from './MarketplaceStoreCard';
import {
  DeliveryFilter,
  StoresFilterSidebar,
  StoresFilterState,
} from './StoresFilterSidebar';
import { cn } from '@/shared/utils/cn';

type SortOption = 'name' | 'deliveryFee' | 'freeFirst';
type ViewLayout = 'grid' | 'list';

export const STORES_PAGE_SIZE = 9;

function getVisiblePageNumbers(current: number, total: number): (number | 'ellipsis')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }
  const pages: (number | 'ellipsis')[] = [1];
  if (current > 3) pages.push('ellipsis');
  const start = Math.max(2, current - 1);
  const end = Math.min(total - 1, current + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (current < total - 2) pages.push('ellipsis');
  pages.push(total);
  return pages;
}

export interface StoresCatalogViewProps {
  stores: PublicStore[];
  categories: StoreCategory[];
}

export function StoresCatalogView({ stores, categories }: StoresCatalogViewProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const shouldReduceMotion = useReducedMotion();
  const resultsTopRef = useRef<HTMLDivElement>(null);

  const [filters, setFilters] = useState<StoresFilterState>({
    query: searchParams.get('q') ?? '',
    category: searchParams.get('categoria') ?? 'all',
    deliveryMode:
      (searchParams.get('entrega') as DeliveryFilter) === 'delivery' ||
      (searchParams.get('entrega') as DeliveryFilter) === 'pickup'
        ? (searchParams.get('entrega') as DeliveryFilter)
        : 'all',
    maxDeliveryFee: searchParams.get('tarifaMax')
      ? Number(searchParams.get('tarifaMax'))
      : null,
    onlyWithWhatsapp: searchParams.get('whatsapp') === '1',
    onlyWithAddress: searchParams.get('direccion') === '1',
  });

  const [currentPage, setCurrentPage] = useState<number>(() => {
    const p = Number.parseInt(searchParams.get('pagina') ?? '1', 10);
    return Number.isNaN(p) || p < 1 ? 1 : p;
  });

  const [sortBy, setSortBy] = useState<SortOption>('name');
  const [viewLayout, setViewLayout] = useState<ViewLayout>('grid');
  const [isMobileFiltersOpen, setIsMobileFiltersOpen] = useState(false);

  useEffect(() => {
    const entrega = searchParams.get('entrega');
    const tarifaMax = searchParams.get('tarifaMax');
    const pagina = Number.parseInt(searchParams.get('pagina') ?? '1', 10);

    setFilters({
      query: searchParams.get('q') ?? '',
      category: searchParams.get('categoria') ?? 'all',
      deliveryMode:
        entrega === 'delivery' || entrega === 'pickup' ? entrega : 'all',
      maxDeliveryFee:
        tarifaMax !== null && !Number.isNaN(Number(tarifaMax))
          ? Number(tarifaMax)
          : null,
      onlyWithWhatsapp: searchParams.get('whatsapp') === '1',
      onlyWithAddress: searchParams.get('direccion') === '1',
    });
    setCurrentPage(Number.isNaN(pagina) || pagina < 1 ? 1 : pagina);
  }, [searchParams]);

  const syncUrl = (nextFilters: StoresFilterState, nextPage: number) => {
    const params = new URLSearchParams();
    if (nextFilters.query.trim()) params.set('q', nextFilters.query.trim());
    if (nextFilters.category !== 'all') params.set('categoria', nextFilters.category);
    if (nextFilters.deliveryMode !== 'all') params.set('entrega', nextFilters.deliveryMode);
    if (nextFilters.maxDeliveryFee !== null) {
      params.set('tarifaMax', String(nextFilters.maxDeliveryFee));
    }
    if (nextFilters.onlyWithWhatsapp) params.set('whatsapp', '1');
    if (nextFilters.onlyWithAddress) params.set('direccion', '1');
    if (nextPage > 1) params.set('pagina', String(nextPage));

    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };

  const handleFiltersChange = (next: StoresFilterState) => {
    setFilters(next);
    setCurrentPage(1);
    syncUrl(next, 1);
  };

  const handleReset = () => {
    const resetState: StoresFilterState = {
      query: '',
      category: 'all',
      deliveryMode: 'all',
      maxDeliveryFee: null,
      onlyWithWhatsapp: false,
      onlyWithAddress: false,
    };
    setFilters(resetState);
    setCurrentPage(1);
    syncUrl(resetState, 1);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    syncUrl(filters, page);
    if (typeof resultsTopRef.current?.scrollIntoView === 'function') {
      resultsTopRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const activeCategoryObj = useMemo(
    () =>
      categories.find(
        (c) => c.slug === filters.category || c.id === filters.category
      ) ?? null,
    [categories, filters.category]
  );

  const filteredStores = useMemo(() => {
    const normalizedQuery = filters.query.trim().toLowerCase();

    return stores
      .filter((store) => {
        if (normalizedQuery) {
          const text = `${store.name} ${store.description || ''} ${store.address || ''} ${store.category || ''}`.toLowerCase();
          if (!text.includes(normalizedQuery)) return false;
        }

        if (filters.category !== 'all') {
          if (activeCategoryObj) {
            const matchesId = store.categoryIds?.includes(activeCategoryObj.id);
            const matchesText = `${store.category || ''} ${store.description || ''} ${store.name}`
              .toLowerCase()
              .includes(activeCategoryObj.name.toLowerCase());
            if (!matchesId && !matchesText) return false;
          } else {
            const matchesSlug = `${store.category || ''} ${store.description || ''} ${store.name}`
              .toLowerCase()
              .includes(filters.category.toLowerCase());
            if (!matchesSlug) return false;
          }
        }

        if (filters.deliveryMode === 'delivery' && !store.deliveryEnabled) return false;
        if (filters.deliveryMode === 'pickup' && !store.pickupEnabled) return false;

        if (filters.maxDeliveryFee !== null) {
          if (!store.deliveryEnabled) return false;
          const fee = store.deliveryFeeAmount ?? store.deliveryFee ?? 0;
          if (fee > filters.maxDeliveryFee) return false;
        }

        if (
          filters.onlyWithWhatsapp &&
          !(store.whatsappNumber || store.contactPhone)
        ) {
          return false;
        }

        if (filters.onlyWithAddress && !store.address) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'deliveryFee') {
          const feeA = a.deliveryEnabled
            ? (a.deliveryFeeAmount ?? a.deliveryFee ?? Number.MAX_SAFE_INTEGER)
            : Number.MAX_SAFE_INTEGER;
          const feeB = b.deliveryEnabled
            ? (b.deliveryFeeAmount ?? b.deliveryFee ?? Number.MAX_SAFE_INTEGER)
            : Number.MAX_SAFE_INTEGER;
          return feeA - feeB;
        }
        if (sortBy === 'freeFirst') {
          const freeA = a.deliveryEnabled && (a.deliveryFeeAmount ?? a.deliveryFee ?? 0) === 0 ? 0 : 1;
          const freeB = b.deliveryEnabled && (b.deliveryFeeAmount ?? b.deliveryFee ?? 0) === 0 ? 0 : 1;
          if (freeA !== freeB) return freeA - freeB;
        }
        return a.name.localeCompare(b.name, 'es');
      });
  }, [stores, filters, activeCategoryObj, sortBy]);

  const totalPages = Math.max(1, Math.ceil(filteredStores.length / STORES_PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const startIndex = (safePage - 1) * STORES_PAGE_SIZE;
  const paginatedStores = filteredStores.slice(startIndex, startIndex + STORES_PAGE_SIZE);

  const activeChips: { key: string; label: string; onRemove: () => void }[] = [];
  if (filters.query.trim()) {
    activeChips.push({
      key: 'q',
      label: `Búsqueda: "${filters.query.trim()}"`,
      onRemove: () => handleFiltersChange({ ...filters, query: '' }),
    });
  }
  if (filters.category !== 'all') {
    activeChips.push({
      key: 'category',
      label: activeCategoryObj?.name || filters.category,
      onRemove: () => handleFiltersChange({ ...filters, category: 'all' }),
    });
  }
  if (filters.deliveryMode !== 'all') {
    activeChips.push({
      key: 'delivery',
      label: filters.deliveryMode === 'delivery' ? 'Delivery' : 'Retiro en tienda',
      onRemove: () => handleFiltersChange({ ...filters, deliveryMode: 'all' }),
    });
  }
  if (filters.maxDeliveryFee !== null) {
    activeChips.push({
      key: 'fee',
      label:
        filters.maxDeliveryFee === 0
          ? 'Envío gratis'
          : `Envío ≤ S/ ${filters.maxDeliveryFee}`,
      onRemove: () => handleFiltersChange({ ...filters, maxDeliveryFee: null }),
    });
  }
  if (filters.onlyWithWhatsapp) {
    activeChips.push({
      key: 'whatsapp',
      label: 'Con WhatsApp',
      onRemove: () => handleFiltersChange({ ...filters, onlyWithWhatsapp: false }),
    });
  }
  if (filters.onlyWithAddress) {
    activeChips.push({
      key: 'address',
      label: 'Con dirección física',
      onRemove: () => handleFiltersChange({ ...filters, onlyWithAddress: false }),
    });
  }

  return (
    <div className="w-full lg:grid lg:grid-cols-[280px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)] lg:gap-8 items-start">
      {/* Left Column: Filter Sidebar (Sticky on Desktop, Slide-Over Drawer on Mobile) */}
      <StoresFilterSidebar
        filters={filters}
        categories={categories}
        onChange={handleFiltersChange}
        onReset={handleReset}
        activeFilterCount={activeChips.length}
        isMobileOpen={isMobileFiltersOpen}
        onCloseMobile={() => setIsMobileFiltersOpen(false)}
      />

      {/* Right Column: Results, Toolbar, Paginated Cards */}
      <div ref={resultsTopRef} className="space-y-5 min-w-0">
        {/* Top Controls Bar */}
        <div className="rounded-2xl bg-white border border-stone-200/90 p-4 shadow-2xs flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsMobileFiltersOpen(true)}
              className="lg:hidden min-h-[44px] inline-flex items-center gap-2 px-4 rounded-xl border border-stone-200 bg-stone-50 hover:bg-stone-100 text-xs font-bold text-stone-800 cursor-pointer"
            >
              <SlidersHorizontal className="w-4 h-4 text-[#005141]" />
              <span>Filtros</span>
              {activeChips.length > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-[#072720] text-[#36f4a4] text-[10px] font-bold tabular-nums">
                  {activeChips.length}
                </span>
              )}
            </button>

            <div>
              <p aria-live="polite" className="text-sm font-bold text-stone-900">
                {filteredStores.length}{' '}
                {filteredStores.length === 1 ? 'comercio encontrado' : 'comercios encontrados'}
              </p>
              {filteredStores.length > 0 && (
                <p className="text-[11px] text-stone-500">
                  Mostrando {startIndex + 1}–{Math.min(startIndex + STORES_PAGE_SIZE, filteredStores.length)} de {filteredStores.length}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2.5 ml-auto">
            <label htmlFor="stores-sort" className="sr-only">
              Ordenar comercios
            </label>
            <select
              id="stores-sort"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              className="min-h-[44px] px-3 py-2 rounded-xl border border-stone-300 bg-white text-xs font-semibold text-stone-800 focus:outline-none focus:border-[#005141]"
            >
              <option value="name">Nombre (A–Z)</option>
              <option value="deliveryFee">Menor costo de envío</option>
              <option value="freeFirst">Envío gratis primero</option>
            </select>

            <div className="hidden sm:flex items-center rounded-xl border border-stone-200 bg-stone-50 p-1">
              <button
                type="button"
                aria-label="Vista en cuadrícula"
                aria-pressed={viewLayout === 'grid'}
                onClick={() => setViewLayout('grid')}
                className={cn(
                  'min-h-[36px] min-w-[36px] inline-flex items-center justify-center rounded-lg transition-colors cursor-pointer',
                  viewLayout === 'grid'
                    ? 'bg-white text-[#005141] shadow-2xs'
                    : 'text-stone-500 hover:text-stone-800'
                )}
              >
                <LayoutGrid className="w-4 h-4" />
              </button>
              <button
                type="button"
                aria-label="Vista en lista"
                aria-pressed={viewLayout === 'list'}
                onClick={() => setViewLayout('list')}
                className={cn(
                  'min-h-[36px] min-w-[36px] inline-flex items-center justify-center rounded-lg transition-colors cursor-pointer',
                  viewLayout === 'list'
                    ? 'bg-white text-[#005141] shadow-2xs'
                    : 'text-stone-500 hover:text-stone-800'
                )}
              >
                <List className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Removable Active Filter Chips */}
        <AnimatePresence>
          {activeChips.length > 0 && (
            <motion.div
              initial={shouldReduceMotion ? false : { opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.16 }}
              className="flex flex-wrap items-center gap-2"
            >
              {activeChips.map((chip) => (
                <button
                  key={chip.key}
                  type="button"
                  onClick={chip.onRemove}
                  className="min-h-[36px] inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-semibold text-[#005141] hover:bg-emerald-100 transition-colors cursor-pointer"
                >
                  <span>{chip.label}</span>
                  <X className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Grid / List or Empty State */}
        {paginatedStores.length === 0 ? (
          <div
            role="status"
            className="rounded-2xl border border-dashed border-stone-300 bg-white p-12 text-center space-y-3"
          >
            <div className="w-12 h-12 rounded-2xl bg-stone-100 text-stone-500 flex items-center justify-center mx-auto">
              <Store className="w-6 h-6" />
            </div>
            <h3 className="font-display text-lg font-bold text-stone-900">
              No encontramos comercios con esos filtros
            </h3>
            <p className="text-sm text-stone-600 max-w-md mx-auto">
              Prueba quitando algún filtro o buscando otro término para ver todos los comercios disponibles.
            </p>
            {activeChips.length > 0 && (
              <button
                type="button"
                onClick={handleReset}
                className="min-h-[44px] inline-flex items-center gap-2 px-5 rounded-full bg-[#072720] text-white text-xs font-semibold hover:bg-[#005141] transition-colors cursor-pointer"
              >
                Ver todos los comercios
              </button>
            )}
          </div>
        ) : (
          <motion.div
            key={`${safePage}-${viewLayout}`}
            initial={shouldReduceMotion ? false : { opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className={cn(
              viewLayout === 'grid'
                ? 'grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-4 sm:gap-5'
                : 'grid grid-cols-1 gap-4'
            )}
          >
            {paginatedStores.map((store) => (
              <MarketplaceStoreCard
                key={store.id}
                rawStore={store}
                layout={viewLayout}
              />
            ))}
          </motion.div>
        )}

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <nav
            aria-label="Paginación de tiendas"
            className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-stone-200/80"
          >
            <button
              type="button"
              disabled={safePage <= 1}
              onClick={() => handlePageChange(safePage - 1)}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-xl border border-stone-200 bg-white hover:bg-stone-50 text-xs font-semibold text-stone-800 disabled:opacity-40 disabled:pointer-events-none cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
              <span>Anterior</span>
            </button>

            <div className="flex items-center gap-1.5">
              {getVisiblePageNumbers(safePage, totalPages).map((entry, idx) => {
                if (entry === 'ellipsis') {
                  return (
                    <span
                      key={`ellipsis-${idx}`}
                      className="px-2 text-xs font-bold text-stone-400 select-none"
                      aria-hidden="true"
                    >
                      …
                    </span>
                  );
                }
                const isCurrent = entry === safePage;
                return (
                  <button
                    key={entry}
                    type="button"
                    aria-label={`Página ${entry}`}
                    aria-current={isCurrent ? 'page' : undefined}
                    onClick={() => handlePageChange(entry)}
                    className={cn(
                      'min-h-[44px] min-w-[44px] rounded-xl text-xs font-bold tabular-nums transition-colors cursor-pointer',
                      isCurrent
                        ? 'bg-[#072720] text-white'
                        : 'bg-white border border-stone-200 text-stone-700 hover:bg-stone-50'
                    )}
                  >
                    {entry}
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              disabled={safePage >= totalPages}
              onClick={() => handlePageChange(safePage + 1)}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-xl border border-stone-200 bg-white hover:bg-stone-50 text-xs font-semibold text-stone-800 disabled:opacity-40 disabled:pointer-events-none cursor-pointer"
            >
              <span>Siguiente</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </nav>
        )}
      </div>
    </div>
  );
}

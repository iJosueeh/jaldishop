'use client';

import React, { useMemo, useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { motion, useReducedMotion } from 'framer-motion';
import {
  Package,
  X,
  SlidersHorizontal,
  LayoutGrid,
  List,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { MarketplaceProductItem } from '../types/storefront.types';
import { MarketplaceProductCard } from './MarketplaceProductCard';
import {
  ProductDeliveryFilter,
  ProductsFilterSidebar,
  ProductsFilterState,
  StoreOption,
} from './ProductsFilterSidebar';
import { cn } from '@/shared/utils/cn';

type ProductSortOption = 'relevancia' | 'price_asc' | 'price_desc' | 'name';
type ViewLayout = 'grid' | 'list';

export const PRODUCTS_PAGE_SIZE = 12;

function parseDeliveryParam(raw: string | null): ProductDeliveryFilter {
  return raw === 'delivery' || raw === 'pickup' ? raw : 'all';
}

function parseSortParam(raw: string | null): ProductSortOption {
  if (raw === 'price_asc' || raw === 'price_desc' || raw === 'name') {
    return raw;
  }
  return 'relevancia';
}

function parseMaxPriceParam(raw: string | null): number | null {
  if (raw === null || raw.trim() === '') return null;
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed < 0) return null;
  return parsed;
}

function parsePageParam(raw: string | null): number {
  const parsed = Number.parseInt(raw ?? '1', 10);
  return Number.isNaN(parsed) || parsed < 1 ? 1 : parsed;
}

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

export interface ProductsCatalogViewProps {
  products: MarketplaceProductItem[];
}

export function ProductsCatalogView({ products }: ProductsCatalogViewProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const shouldReduceMotion = useReducedMotion();
  const resultsTopRef = useRef<HTMLDivElement>(null);
  const mobileFilterTriggerRef = useRef<HTMLButtonElement>(null);

  const [filters, setFilters] = useState<ProductsFilterState>(() => ({
    query: searchParams.get('q') ?? '',
    category: searchParams.get('categoria') ?? 'all',
    deliveryMode: parseDeliveryParam(searchParams.get('entrega')),
    storeSlug: searchParams.get('tienda') ?? 'all',
    maxPrice: parseMaxPriceParam(searchParams.get('precioMax')),
  }));

  const [sortBy, setSortBy] = useState<ProductSortOption>(() =>
    parseSortParam(searchParams.get('orden'))
  );
  const [currentPage, setCurrentPage] = useState<number>(() =>
    parsePageParam(searchParams.get('page'))
  );
  const [viewLayout, setViewLayout] = useState<ViewLayout>('grid');
  const [isMobileFiltersOpen, setIsMobileFiltersOpen] = useState(false);

  useEffect(() => {
    setFilters({
      query: searchParams.get('q') ?? '',
      category: searchParams.get('categoria') ?? 'all',
      deliveryMode: parseDeliveryParam(searchParams.get('entrega')),
      storeSlug: searchParams.get('tienda') ?? 'all',
      maxPrice: parseMaxPriceParam(searchParams.get('precioMax')),
    });
    setSortBy(parseSortParam(searchParams.get('orden')));
    setCurrentPage(parsePageParam(searchParams.get('page')));
  }, [searchParams]);

  const syncUrl = (
    nextFilters: ProductsFilterState,
    nextSort: ProductSortOption,
    nextPage: number
  ) => {
    const params = new URLSearchParams();
    if (nextFilters.query.trim()) params.set('q', nextFilters.query.trim());
    if (nextFilters.category !== 'all') params.set('categoria', nextFilters.category);
    if (nextFilters.deliveryMode !== 'all') params.set('entrega', nextFilters.deliveryMode);
    if (nextFilters.storeSlug !== 'all') params.set('tienda', nextFilters.storeSlug);
    if (nextFilters.maxPrice !== null) params.set('precioMax', String(nextFilters.maxPrice));
    if (nextSort !== 'relevancia') params.set('orden', nextSort);
    if (nextPage > 1) params.set('page', String(nextPage));

    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };

  const handleFiltersChange = (next: ProductsFilterState) => {
    setFilters(next);
    setCurrentPage(1);
    syncUrl(next, sortBy, 1);
  };

  const handleSortChange = (nextSort: ProductSortOption) => {
    setSortBy(nextSort);
    setCurrentPage(1);
    syncUrl(filters, nextSort, 1);
  };

  const handleReset = () => {
    const resetState: ProductsFilterState = {
      query: '',
      category: 'all',
      deliveryMode: 'all',
      storeSlug: 'all',
      maxPrice: null,
    };
    setFilters(resetState);
    setSortBy('relevancia');
    setCurrentPage(1);
    syncUrl(resetState, 'relevancia', 1);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    syncUrl(filters, sortBy, page);
    if (typeof resultsTopRef.current?.scrollIntoView === 'function') {
      resultsTopRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const p of products) {
      if (p.category && p.category.trim()) set.add(p.category.trim());
    }
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'es'));
  }, [products]);

  const stores = useMemo<StoreOption[]>(() => {
    const map = new Map<string, string>();
    for (const p of products) {
      if (p.storeSlug && p.storeName && !map.has(p.storeSlug)) {
        map.set(p.storeSlug, p.storeName);
      }
    }
    return Array.from(map.entries())
      .map(([slug, name]) => ({ slug, name }))
      .sort((a, b) => a.name.localeCompare(b.name, 'es'));
  }, [products]);

  const maxCatalogPrice = useMemo(() => {
    let max = 0;
    for (const p of products) {
      if (typeof p.price === 'number' && Number.isFinite(p.price) && p.price > max) {
        max = p.price;
      }
    }
    return max;
  }, [products]);

  const filteredProducts = useMemo(() => {
    const normalizedQuery = filters.query.trim().toLowerCase();

    const matched = products.filter((p) => {
      if (normalizedQuery) {
        const text = `${p.name} ${p.description || ''} ${p.storeName} ${p.category || ''}`.toLowerCase();
        if (!text.includes(normalizedQuery)) return false;
      }
      if (filters.category !== 'all' && p.category !== filters.category) {
        return false;
      }
      if (filters.deliveryMode === 'delivery' && !p.deliveryEnabled) return false;
      if (filters.deliveryMode === 'pickup' && !p.pickupEnabled) return false;
      if (filters.storeSlug !== 'all' && p.storeSlug !== filters.storeSlug) {
        return false;
      }
      if (filters.maxPrice !== null) {
        const hasValidPrice =
          typeof p.price === 'number' && Number.isFinite(p.price) && p.price >= 0;
        if (!hasValidPrice || p.price > filters.maxPrice) return false;
      }
      return true;
    });

    if (sortBy === 'relevancia') return matched;

    return [...matched].sort((a, b) => {
      const validA = typeof a.price === 'number' && Number.isFinite(a.price) && a.price >= 0;
      const validB = typeof b.price === 'number' && Number.isFinite(b.price) && b.price >= 0;

      if (sortBy === 'price_asc') {
        if (!validA && !validB) return a.name.localeCompare(b.name, 'es');
        if (!validA) return 1;
        if (!validB) return -1;
        return a.price - b.price || a.name.localeCompare(b.name, 'es');
      }
      if (sortBy === 'price_desc') {
        if (!validA && !validB) return a.name.localeCompare(b.name, 'es');
        if (!validA) return 1;
        if (!validB) return -1;
        return b.price - a.price || a.name.localeCompare(b.name, 'es');
      }
      return a.name.localeCompare(b.name, 'es');
    });
  }, [products, filters, sortBy]);

  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PRODUCTS_PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const startIndex = (safePage - 1) * PRODUCTS_PAGE_SIZE;
  const paginatedProducts = filteredProducts.slice(
    startIndex,
    startIndex + PRODUCTS_PAGE_SIZE
  );

  const selectedStoreName =
    stores.find((s) => s.slug === filters.storeSlug)?.name ?? filters.storeSlug;

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
      label: `Categoría: ${filters.category}`,
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
  if (filters.storeSlug !== 'all') {
    activeChips.push({
      key: 'store',
      label: `Tienda: ${selectedStoreName}`,
      onRemove: () => handleFiltersChange({ ...filters, storeSlug: 'all' }),
    });
  }
  if (filters.maxPrice !== null) {
    activeChips.push({
      key: 'price',
      label: `Precio ≤ S/ ${filters.maxPrice}`,
      onRemove: () => handleFiltersChange({ ...filters, maxPrice: null }),
    });
  }

  return (
    <div className="w-full lg:grid lg:grid-cols-[280px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)] lg:gap-8 items-start">
      <ProductsFilterSidebar
        filters={filters}
        categories={categories}
        stores={stores}
        maxCatalogPrice={maxCatalogPrice}
        onChange={handleFiltersChange}
        onReset={handleReset}
        activeFilterCount={activeChips.length}
        isMobileOpen={isMobileFiltersOpen}
        onCloseMobile={() => setIsMobileFiltersOpen(false)}
        returnFocusRef={mobileFilterTriggerRef}
      />

      <div ref={resultsTopRef} className="space-y-5 min-w-0">
        {/* Top Controls Bar */}
        <div className="rounded-2xl bg-white border border-[#e7e0d6] p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              ref={mobileFilterTriggerRef}
              type="button"
              onClick={() => setIsMobileFiltersOpen(true)}
              className="lg:hidden min-h-[44px] inline-flex items-center gap-2 px-4 rounded-xl border border-[#e7e0d6] bg-[#faf7f2] hover:bg-stone-100 text-xs font-bold text-stone-800 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
            >
              <SlidersHorizontal className="w-4 h-4 text-[#005141]" aria-hidden="true" />
              <span>Filtros</span>
              {activeChips.length > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-[#072720] text-[#36f4a4] text-[10px] font-bold tabular-nums">
                  {activeChips.length}
                </span>
              )}
            </button>

            <div>
              <p aria-live="polite" className="text-sm font-bold text-stone-900">
                {filteredProducts.length}{' '}
                {filteredProducts.length === 1 ? 'producto disponible' : 'productos disponibles'}
              </p>
              {filteredProducts.length > 0 && (
                <p className="text-xs text-stone-600 tabular-nums">
                  Mostrando {startIndex + 1}–
                  {Math.min(startIndex + PRODUCTS_PAGE_SIZE, filteredProducts.length)} de{' '}
                  {filteredProducts.length}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2.5 ml-auto">
            <label htmlFor="products-sort" className="sr-only">
              Ordenar productos
            </label>
            <select
              id="products-sort"
              value={sortBy}
              onChange={(e) => handleSortChange(e.target.value as ProductSortOption)}
              className="min-h-[44px] px-3 py-2 rounded-xl border border-stone-300 bg-white text-xs font-semibold text-stone-800 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            >
              <option value="relevancia">Relevancia</option>
              <option value="price_asc">Menor precio</option>
              <option value="price_desc">Mayor precio</option>
              <option value="name">Nombre (A–Z)</option>
            </select>

            <div className="hidden sm:flex items-center rounded-xl border border-[#e7e0d6] bg-[#faf7f2] p-0.5">
              <button
                type="button"
                aria-label="Vista en cuadrícula"
                aria-pressed={viewLayout === 'grid'}
                onClick={() => setViewLayout('grid')}
                className={cn(
                  'min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-lg cursor-pointer focus-visible:outline-2 focus-visible:outline-[#005141]',
                  viewLayout === 'grid'
                    ? 'bg-white text-[#005141]'
                    : 'text-stone-600 hover:text-stone-900'
                )}
              >
                <LayoutGrid className="w-4 h-4" aria-hidden="true" />
              </button>
              <button
                type="button"
                aria-label="Vista en lista"
                aria-pressed={viewLayout === 'list'}
                onClick={() => setViewLayout('list')}
                className={cn(
                  'min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-lg cursor-pointer focus-visible:outline-2 focus-visible:outline-[#005141]',
                  viewLayout === 'list'
                    ? 'bg-white text-[#005141]'
                    : 'text-stone-600 hover:text-stone-900'
                )}
              >
                <List className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>

        {/* Removable Active Filter Chips */}
        {activeChips.length > 0 && (
          <div
            role="region"
            aria-label="Filtros activos"
            className="flex flex-wrap items-center gap-2"
          >
            {activeChips.map((chip) => (
              <button
                key={chip.key}
                type="button"
                onClick={chip.onRemove}
                aria-label={`Quitar filtro ${chip.label}`}
                className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-semibold text-[#005141] [@media(hover:hover)]:hover:bg-emerald-100 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
              >
                <span>{chip.label}</span>
                <X className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />
              </button>
            ))}

            <button
              type="button"
              onClick={handleReset}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-semibold text-stone-700 hover:text-[#005141] hover:bg-stone-100 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
            >
              Limpiar todo
            </button>
          </div>
        )}

        {/* Grid / List or Empty State */}
        {paginatedProducts.length === 0 ? (
          <div
            role="status"
            className="rounded-2xl border border-dashed border-stone-300 bg-white p-10 sm:p-12 text-center space-y-4"
          >
            <div className="w-12 h-12 rounded-2xl bg-[#faf7f2] border border-[#e7e0d6] text-stone-600 flex items-center justify-center mx-auto">
              <Package className="w-6 h-6" aria-hidden="true" />
            </div>
            <div className="space-y-1.5">
              <h3 className="font-display text-lg font-bold text-stone-900">
                No encontramos productos con esos filtros
              </h3>
              <p className="text-sm text-stone-600 max-w-md mx-auto">
                {activeChips.length > 0
                  ? `Ningún producto coincide con ${activeChips.map((c) => c.label).join(' + ')}.`
                  : 'Cuando los comercios publiquen productos activos, aparecerán aquí.'}
              </p>
            </div>
            {activeChips.length > 0 && (
              <button
                type="button"
                onClick={handleReset}
                className="min-h-[44px] inline-flex items-center gap-2 px-5 rounded-full bg-[#072720] text-white text-xs font-semibold hover:bg-[#005141] transition-opacity duration-150 ease-out motion-reduce:transition-none cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
              >
                Limpiar filtros
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
            {paginatedProducts.map((product) => (
              <MarketplaceProductCard
                key={`${product.storeId}-${product.id}`}
                product={product}
                layout={viewLayout}
              />
            ))}
          </motion.div>
        )}

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <nav
            aria-label="Paginación de productos"
            className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-[#e7e0d6]"
          >
            <button
              type="button"
              disabled={safePage <= 1}
              onClick={() => handlePageChange(safePage - 1)}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-xl border border-[#e7e0d6] bg-white hover:bg-stone-50 text-xs font-semibold text-stone-800 disabled:opacity-40 disabled:pointer-events-none cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
            >
              <ChevronLeft className="w-4 h-4" aria-hidden="true" />
              <span>Anterior</span>
            </button>

            <div className="flex items-center gap-1.5">
              {getVisiblePageNumbers(safePage, totalPages).map((entry, idx) => {
                if (entry === 'ellipsis') {
                  return (
                    <span
                      key={`ellipsis-${idx}`}
                      className="px-2 text-xs font-bold text-stone-500 select-none"
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
                      'min-h-[44px] min-w-[44px] rounded-xl text-xs font-bold tabular-nums cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                      isCurrent
                        ? 'bg-[#072720] text-white'
                        : 'bg-white border border-[#e7e0d6] text-stone-800 hover:bg-stone-50'
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
              className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-xl border border-[#e7e0d6] bg-white hover:bg-stone-50 text-xs font-semibold text-stone-800 disabled:opacity-40 disabled:pointer-events-none cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
            >
              <span>Siguiente</span>
              <ChevronRight className="w-4 h-4" aria-hidden="true" />
            </button>
          </nav>
        )}
      </div>
    </div>
  );
}

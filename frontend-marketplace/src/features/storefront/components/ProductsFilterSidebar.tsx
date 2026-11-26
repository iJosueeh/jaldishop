'use client';

import React, { useState, useEffect, useRef } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  Search,
  ChevronDown,
  X,
  SlidersHorizontal,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export type ProductDeliveryFilter = 'all' | 'delivery' | 'pickup';

export interface StoreOption {
  slug: string;
  name: string;
}

export interface ProductsFilterState {
  query: string;
  category: string;
  deliveryMode: ProductDeliveryFilter;
  storeSlug: string;
  maxPrice: number | null;
}

export interface ProductsFilterSidebarProps {
  filters: ProductsFilterState;
  categories: string[];
  stores: StoreOption[];
  maxCatalogPrice: number;
  onChange: (next: ProductsFilterState) => void;
  onReset: () => void;
  activeFilterCount: number;
  isMobileOpen: boolean;
  onCloseMobile: () => void;
  returnFocusRef?: React.RefObject<HTMLButtonElement | null>;
}

interface FilterSectionProps {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}

function FilterSection({ title, defaultOpen = true, children }: FilterSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-b border-[#e7e0d6] pb-4 last:border-b-0 last:pb-0">
      <button
        type="button"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((prev) => !prev)}
        className="w-full min-h-[44px] flex items-center justify-between text-left text-xs font-bold uppercase tracking-wider text-stone-800 hover:text-[#005141] transition-opacity duration-150 ease-out motion-reduce:transition-none cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141] rounded-lg"
      >
        <span>{title}</span>
        <ChevronDown
          aria-hidden="true"
          className={cn(
            'w-4 h-4 text-stone-600 transition-transform duration-150 ease-out motion-reduce:transition-none',
            isOpen ? 'rotate-180' : ''
          )}
        />
      </button>

      {isOpen && <div className="pt-2">{children}</div>}
    </div>
  );
}

const PRICE_PRESETS: { label: string; value: number | null }[] = [
  { label: 'Cualquiera', value: null },
  { label: 'Hasta S/ 15', value: 15 },
  { label: 'Hasta S/ 30', value: 30 },
  { label: 'Hasta S/ 50', value: 50 },
];

export function ProductsFilterSidebar({
  filters,
  categories,
  stores,
  maxCatalogPrice,
  onChange,
  onReset,
  activeFilterCount,
  isMobileOpen,
  onCloseMobile,
  returnFocusRef,
}: ProductsFilterSidebarProps) {
  const shouldReduceMotion = useReducedMotion();
  const closeRef = useRef<HTMLButtonElement>(null);
  const wasOpenRef = useRef(false);

  useEffect(() => {
    if (isMobileOpen) {
      wasOpenRef.current = true;
      const prevOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      closeRef.current?.focus();

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Escape') onCloseMobile();
      };
      document.addEventListener('keydown', handleKeyDown);
      return () => {
        document.body.style.overflow = prevOverflow;
        document.removeEventListener('keydown', handleKeyDown);
      };
    } else if (wasOpenRef.current) {
      wasOpenRef.current = false;
      returnFocusRef?.current?.focus();
    }
  }, [isMobileOpen, onCloseMobile, returnFocusRef]);

  const sliderMax = Math.max(50, Math.ceil(maxCatalogPrice / 10) * 10);
  const sliderValue =
    filters.maxPrice === null ? sliderMax : Math.min(filters.maxPrice, sliderMax);

  const filterFormContent = (
    <div className="space-y-4">
      <div className="flex items-center justify-between pb-3 border-b border-[#e7e0d6]">
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="w-4 h-4 text-[#005141]" aria-hidden="true" />
          <h2 className="font-display text-sm font-bold text-stone-900">Filtros</h2>
          {activeFilterCount > 0 && (
            <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#072720] text-[#36f4a4] tabular-nums">
              {activeFilterCount}
            </span>
          )}
        </div>

        {activeFilterCount > 0 && (
          <button
            type="button"
            onClick={onReset}
            className="min-h-[44px] px-2 text-xs font-semibold text-[#005141] hover:underline cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141] rounded-lg"
          >
            Limpiar todo
          </button>
        )}
      </div>

      {/* 1. Search */}
      <FilterSection title="Buscar producto">
        <div className="relative">
          <Search
            className="w-4 h-4 text-stone-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
            aria-hidden="true"
          />
          <input
            id="products-search"
            aria-label="Buscar producto o tienda"
            type="search"
            value={filters.query}
            onChange={(e) => onChange({ ...filters, query: e.target.value })}
            placeholder="Nombre, tienda, sabor..."
            className="w-full min-h-[44px] pl-9 pr-11 py-2 rounded-xl border border-stone-300 bg-white text-sm text-stone-900 placeholder-stone-500 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
          />
          {filters.query && (
            <button
              type="button"
              onClick={() => onChange({ ...filters, query: '' })}
              aria-label="Limpiar búsqueda"
              className="absolute right-0.5 top-1/2 -translate-y-1/2 min-h-[44px] min-w-[44px] inline-flex items-center justify-center text-stone-600 hover:text-stone-900 cursor-pointer rounded-xl focus-visible:outline-2 focus-visible:outline-[#005141]"
            >
              <X className="w-4 h-4" aria-hidden="true" />
            </button>
          )}
        </div>
      </FilterSection>

      {/* 2. Categories */}
      {categories.length > 0 && (
        <FilterSection title="Categorías">
          <div
            className="flex flex-wrap gap-2 max-h-48 overflow-y-auto pr-1"
            role="group"
            aria-label="Filtrar por categoría de producto"
          >
            <button
              type="button"
              aria-pressed={filters.category === 'all'}
              onClick={() => onChange({ ...filters, category: 'all' })}
              className={cn(
                'min-h-[44px] px-3.5 py-2 rounded-full text-xs font-semibold border cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                filters.category === 'all'
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-[#faf7f2] text-stone-800 border-[#e7e0d6] [@media(hover:hover)]:hover:bg-stone-100'
              )}
            >
              Todas
            </button>
            {categories.map((cat) => {
              const isSelected = filters.category === cat;
              return (
                <button
                  key={cat}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => onChange({ ...filters, category: cat })}
                  className={cn(
                    'min-h-[44px] px-3.5 py-2 rounded-full text-xs font-semibold border cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isSelected
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-stone-800 border-[#e7e0d6] [@media(hover:hover)]:hover:bg-stone-100'
                  )}
                >
                  {cat}
                </button>
              );
            })}
          </div>
        </FilterSection>
      )}

      {/* 3. Delivery Mode */}
      <FilterSection title="Modalidad de entrega">
        <div
          className="grid grid-cols-3 gap-1.5"
          role="group"
          aria-label="Modalidad de entrega"
        >
          {(
            [
              { id: 'all', label: 'Todas' },
              { id: 'delivery', label: 'Delivery' },
              { id: 'pickup', label: 'Retiro' },
            ] as const
          ).map((opt) => (
            <button
              key={opt.id}
              type="button"
              aria-pressed={filters.deliveryMode === opt.id}
              onClick={() => onChange({ ...filters, deliveryMode: opt.id })}
              className={cn(
                'min-h-[44px] px-2 py-2 rounded-xl text-center text-xs font-semibold border cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                filters.deliveryMode === opt.id
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-[#faf7f2] text-stone-800 border-[#e7e0d6] [@media(hover:hover)]:hover:bg-stone-100'
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </FilterSection>

      {/* 4. Store */}
      {stores.length > 0 && (
        <FilterSection title="Tienda" defaultOpen={filters.storeSlug !== 'all'}>
          <label htmlFor="products-store-select" className="sr-only">
            Filtrar por tienda
          </label>
          <select
            id="products-store-select"
            value={filters.storeSlug}
            onChange={(e) => onChange({ ...filters, storeSlug: e.target.value })}
            className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-stone-300 bg-white text-xs font-semibold text-stone-800 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
          >
            <option value="all">Todas las tiendas</option>
            {stores.map((store) => (
              <option key={store.slug} value={store.slug}>
                {store.name}
              </option>
            ))}
          </select>
        </FilterSection>
      )}

      {/* 5. Max Price */}
      <FilterSection title="Precio máximo" defaultOpen={filters.maxPrice !== null}>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-1.5">
            {PRICE_PRESETS.map((preset) => {
              const isSelected = filters.maxPrice === preset.value;
              return (
                <button
                  key={preset.label}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => onChange({ ...filters, maxPrice: preset.value })}
                  className={cn(
                    'min-h-[44px] px-2.5 rounded-xl text-xs font-semibold border cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isSelected
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-white text-stone-800 border-[#e7e0d6] [@media(hover:hover)]:hover:bg-stone-50'
                  )}
                >
                  {preset.label}
                </button>
              );
            })}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs text-stone-700">
              <label htmlFor="max-price-slider" className="font-medium">
                Tope personalizado
              </label>
              <span className="font-bold text-[#005141] tabular-nums">
                {filters.maxPrice === null ? 'Sin límite' : `Hasta S/ ${filters.maxPrice}`}
              </span>
            </div>
            <input
              id="max-price-slider"
              type="range"
              min={5}
              max={sliderMax}
              step={5}
              value={sliderValue}
              onChange={(e) => {
                const val = Number(e.target.value);
                onChange({
                  ...filters,
                  maxPrice: val >= sliderMax ? null : val,
                });
              }}
              className="w-full min-h-[44px] accent-[#005141] cursor-pointer"
            />
          </div>
        </div>
      </FilterSection>
    </div>
  );

  return (
    <>
      {/* Desktop Sticky Sidebar */}
      <aside
        aria-label="Filtros de productos"
        className="hidden lg:block sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto rounded-2xl bg-white border border-[#e7e0d6] p-5"
      >
        {filterFormContent}
      </aside>

      {/* Mobile Slide-Over Filter Drawer */}
      <AnimatePresence>
        {isMobileOpen && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Filtros de productos"
            className="fixed inset-0 z-50 lg:hidden flex"
          >
            <motion.div
              initial={shouldReduceMotion ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.18, ease: 'easeOut' }}
              onClick={onCloseMobile}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs"
              aria-hidden="true"
            />

            <motion.div
              initial={shouldReduceMotion ? false : { x: '-100%' }}
              animate={{ x: 0 }}
              exit={shouldReduceMotion ? { opacity: 0 } : { x: '-100%' }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="relative z-10 w-[86vw] max-w-[320px] h-full bg-white border-r border-[#e7e0d6] p-5 overflow-y-auto flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between pb-3 mb-3 border-b border-[#e7e0d6]">
                  <span className="font-display text-base font-bold text-stone-900">
                    Filtrar productos
                  </span>
                  <button
                    ref={closeRef}
                    type="button"
                    onClick={onCloseMobile}
                    aria-label="Cerrar filtros"
                    className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-xl bg-stone-100 hover:bg-stone-200 text-stone-800 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                  >
                    <X className="w-5 h-5" aria-hidden="true" />
                  </button>
                </div>
                {filterFormContent}
              </div>

              <div className="pt-4 mt-4 border-t border-[#e7e0d6]">
                <button
                  type="button"
                  onClick={onCloseMobile}
                  className="w-full min-h-[44px] rounded-full bg-[#072720] text-white text-sm font-semibold hover:bg-[#005141] transition-opacity duration-150 ease-out motion-reduce:transition-none cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                >
                  Ver resultados
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}

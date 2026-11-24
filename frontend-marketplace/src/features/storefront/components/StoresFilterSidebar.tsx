'use client';

import React, { useState, useEffect, useRef } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  Search,
  ChevronDown,
  X,
  SlidersHorizontal,
} from 'lucide-react';
import { StoreCategory } from '../types/storefront.types';
import { cn } from '@/shared/utils/cn';

export type DeliveryFilter = 'all' | 'delivery' | 'pickup';

export interface StoresFilterState {
  query: string;
  category: string;
  deliveryMode: DeliveryFilter;
  maxDeliveryFee: number | null; // null = any, 0 = free
  onlyWithWhatsapp: boolean;
  onlyWithAddress: boolean;
}

export interface StoresFilterSidebarProps {
  filters: StoresFilterState;
  categories: StoreCategory[];
  onChange: (next: StoresFilterState) => void;
  onReset: () => void;
  activeFilterCount: number;
  isMobileOpen: boolean;
  onCloseMobile: () => void;
}

interface FilterSectionProps {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}

function FilterSection({ title, defaultOpen = true, children }: FilterSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const shouldReduceMotion = useReducedMotion();

  return (
    <div className="border-b border-stone-200/80 pb-4 last:border-b-0 last:pb-0">
      <button
        type="button"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((prev) => !prev)}
        className="w-full min-h-[44px] flex items-center justify-between text-left text-xs font-bold uppercase tracking-wider text-stone-800 hover:text-[#005141] transition-colors cursor-pointer"
      >
        <span>{title}</span>
        <ChevronDown
          className={cn(
            'w-4 h-4 text-stone-500 transition-transform duration-200',
            isOpen ? 'rotate-180' : ''
          )}
        />
      </button>

      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            initial={shouldReduceMotion ? false : { opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, height: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="overflow-hidden pt-2"
          >
            {children}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function StoresFilterSidebar({
  filters,
  categories,
  onChange,
  onReset,
  activeFilterCount,
  isMobileOpen,
  onCloseMobile,
}: StoresFilterSidebarProps) {
  const shouldReduceMotion = useReducedMotion();
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!isMobileOpen) return;

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
  }, [isMobileOpen, onCloseMobile]);

  const sliderValue = filters.maxDeliveryFee === null ? 20 : filters.maxDeliveryFee;

  const filterFormContent = (
    <div className="space-y-4">
      <div className="flex items-center justify-between pb-3 border-b border-stone-200/80">
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
            className="text-xs font-semibold text-[#005141] hover:underline cursor-pointer"
          >
            Limpiar todo
          </button>
        )}
      </div>

      {/* 1. Search */}
      <FilterSection title="Buscar comercio">
        <div className="relative">
          <Search
            className="w-4 h-4 text-stone-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
            aria-hidden="true"
          />
          <input
            aria-label="Buscar tienda o rubro"
            type="search"
            value={filters.query}
            onChange={(e) => onChange({ ...filters, query: e.target.value })}
            placeholder="Nombre, distrito, rubro..."
            className="w-full min-h-[44px] pl-9 pr-8 py-2 rounded-xl border border-stone-300 bg-white text-sm text-stone-900 placeholder-stone-400 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
          />
          {filters.query && (
            <button
              type="button"
              onClick={() => onChange({ ...filters, query: '' })}
              aria-label="Limpiar búsqueda"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 text-stone-400 hover:text-stone-700 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </FilterSection>

      {/* 2. Categories */}
      {categories.length > 0 && (
        <FilterSection title="Categorías">
          <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto pr-1">
            <button
              type="button"
              aria-pressed={filters.category === 'all'}
              onClick={() => onChange({ ...filters, category: 'all' })}
              className={cn(
                'min-h-[36px] px-3 py-1 rounded-full text-xs font-semibold border transition-colors cursor-pointer',
                filters.category === 'all'
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
              )}
            >
              Todas
            </button>
            {categories.map((cat) => {
              const isSelected =
                filters.category === cat.slug || filters.category === cat.id;
              return (
                <button
                  key={cat.id}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => onChange({ ...filters, category: cat.slug })}
                  className={cn(
                    'min-h-[36px] px-3 py-1 rounded-full text-xs font-semibold border transition-colors cursor-pointer',
                    isSelected
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
                  )}
                >
                  {cat.name}
                </button>
              );
            })}
          </div>
        </FilterSection>
      )}

      {/* 3. Delivery Mode */}
      <FilterSection title="Modalidad de entrega">
        <div className="grid grid-cols-3 gap-1.5" role="group" aria-label="Modalidad de entrega">
          {(
            [
              { id: 'all', label: 'Todas' },
              { id: 'delivery', label: 'Delivery' },
              { id: 'pickup', label: 'Pickup' },
            ] as const
          ).map((opt) => (
            <button
              key={opt.id}
              type="button"
              aria-pressed={filters.deliveryMode === opt.id}
              onClick={() => onChange({ ...filters, deliveryMode: opt.id })}
              className={cn(
                'min-h-[38px] px-2 py-1.5 rounded-xl text-center text-xs font-semibold border transition-colors cursor-pointer',
                filters.deliveryMode === opt.id
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </FilterSection>

      {/* 4. Max Delivery Fee */}
      <FilterSection
        title="Tarifa máxima de envío"
        defaultOpen={filters.maxDeliveryFee !== null}
      >
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-1.5">
            {[
              { label: 'Cualquiera', value: null },
              { label: 'Envío gratis', value: 0 },
              { label: 'Hasta S/ 5', value: 5 },
              { label: 'Hasta S/ 10', value: 10 },
            ].map((preset) => {
              const isSelected = filters.maxDeliveryFee === preset.value;
              return (
                <button
                  key={preset.label}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => onChange({ ...filters, maxDeliveryFee: preset.value })}
                  className={cn(
                    'min-h-[38px] px-2.5 rounded-xl text-[11px] font-semibold border transition-colors cursor-pointer',
                    isSelected
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-white text-stone-700 border-stone-200 hover:bg-stone-50'
                  )}
                >
                  {preset.label}
                </button>
              );
            })}
          </div>

          <div className="space-y-1">
            <div className="flex items-center justify-between text-[11px] text-stone-600">
              <label htmlFor="max-delivery-slider" className="font-medium">
                Tope personalizado
              </label>
              <span className="font-bold text-[#005141] tabular-nums">
                {filters.maxDeliveryFee === null
                  ? 'Sin límite'
                  : filters.maxDeliveryFee === 0
                  ? 'Gratis'
                  : `Hasta S/ ${filters.maxDeliveryFee}`}
              </span>
            </div>
            <input
              id="max-delivery-slider"
              type="range"
              min={0}
              max={20}
              step={1}
              value={sliderValue}
              onChange={(e) => {
                const val = Number(e.target.value);
                onChange({
                  ...filters,
                  maxDeliveryFee: val >= 20 ? null : val,
                });
              }}
              className="w-full accent-[#005141] cursor-pointer"
            />
          </div>
        </div>
      </FilterSection>

      {/* 5. Additional Channels */}
      <FilterSection
        title="Atención y ubicación"
        defaultOpen={filters.onlyWithWhatsapp || filters.onlyWithAddress}
      >
        <div className="space-y-2.5">
          <label className="flex items-center gap-2.5 text-xs font-medium text-stone-700 cursor-pointer min-h-[36px]">
            <input
              type="checkbox"
              checked={filters.onlyWithWhatsapp}
              onChange={(e) =>
                onChange({ ...filters, onlyWithWhatsapp: e.target.checked })
              }
              className="w-4 h-4 rounded border-stone-300 text-[#005141] focus:ring-[#005141]"
            />
            <span>Con WhatsApp directo</span>
          </label>

          <label className="flex items-center gap-2.5 text-xs font-medium text-stone-700 cursor-pointer min-h-[36px]">
            <input
              type="checkbox"
              checked={filters.onlyWithAddress}
              onChange={(e) =>
                onChange({ ...filters, onlyWithAddress: e.target.checked })
              }
              className="w-4 h-4 rounded border-stone-300 text-[#005141] focus:ring-[#005141]"
            />
            <span>Con dirección física registrada</span>
          </label>
        </div>
      </FilterSection>
    </div>
  );

  return (
    <>
      {/* Desktop Sticky Sidebar (bounded to viewport height so user never loses scroll) */}
      <aside
        aria-label="Filtros de tiendas"
        className="hidden lg:block sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto rounded-2xl bg-white border border-stone-200/90 p-5 shadow-xs"
      >
        {filterFormContent}
      </aside>

      {/* Mobile Slide-Over Filter Drawer */}
      <AnimatePresence>
        {isMobileOpen && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Filtros de tiendas"
            className="fixed inset-0 z-50 lg:hidden flex"
          >
            <motion.div
              initial={shouldReduceMotion ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.18 }}
              onClick={onCloseMobile}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs"
              aria-hidden="true"
            />

            <motion.div
              initial={shouldReduceMotion ? false : { x: '-100%' }}
              animate={{ x: 0 }}
              exit={shouldReduceMotion ? { opacity: 0 } : { x: '-100%' }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
              className="relative z-10 w-[86vw] max-w-[320px] h-full bg-white border-r border-stone-200 p-5 overflow-y-auto shadow-2xl flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between pb-3 mb-3 border-b border-stone-200">
                  <span className="font-display text-base font-bold text-stone-900">
                    Filtrar comercios
                  </span>
                  <button
                    ref={closeRef}
                    type="button"
                    onClick={onCloseMobile}
                    aria-label="Cerrar filtros"
                    className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-xl bg-stone-100 hover:bg-stone-200 text-stone-700 cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                {filterFormContent}
              </div>

              <div className="pt-4 mt-4 border-t border-stone-200">
                <button
                  type="button"
                  onClick={onCloseMobile}
                  className="w-full min-h-[44px] rounded-full bg-[#072720] text-white text-sm font-semibold hover:bg-[#005141] transition-colors cursor-pointer"
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

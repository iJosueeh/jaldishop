'use client';

import React, { useMemo, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Search, Package, Store, ArrowRight, X } from 'lucide-react';
import { MarketplaceProductItem } from '../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';
import { cn } from '@/shared/utils/cn';

export interface ProductsCatalogViewProps {
  products: MarketplaceProductItem[];
}

export function ProductsCatalogView({ products }: ProductsCatalogViewProps) {
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');

  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const p of products) {
      if (p.category && p.category.trim()) set.add(p.category.trim());
    }
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'es'));
  }, [products]);

  const filteredProducts = useMemo(() => {
    const q = query.trim().toLowerCase();
    return products.filter((p) => {
      if (q) {
        const text = `${p.name} ${p.description} ${p.storeName} ${p.category}`.toLowerCase();
        if (!text.includes(q)) return false;
      }
      if (selectedCategory !== 'all' && p.category !== selectedCategory) {
        return false;
      }
      return true;
    });
  }, [products, query, selectedCategory]);

  const hasFilters = query.trim() !== '' || selectedCategory !== 'all';

  return (
    <div className="space-y-8">
      {/* Search & Category Filters */}
      <div className="rounded-2xl bg-white border border-stone-200/90 p-4 sm:p-5 shadow-2xs space-y-4">
        <div>
          <label
            htmlFor="products-search"
            className="block text-[11px] font-bold uppercase tracking-wider text-stone-600 mb-1"
          >
            Buscar producto o tienda
          </label>
          <div className="relative">
            <Search
              className="w-4 h-4 text-stone-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none"
              aria-hidden="true"
            />
            <input
              id="products-search"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Ej. torta, pan, café..."
              className="w-full min-h-[44px] pl-10 pr-4 py-2 rounded-xl border border-stone-300 bg-white text-sm text-stone-900 placeholder-stone-400 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>
        </div>

        {categories.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar" role="group" aria-label="Filtrar por categoría de producto">
            <button
              type="button"
              aria-pressed={selectedCategory === 'all'}
              onClick={() => setSelectedCategory('all')}
              className={cn(
                'min-h-[44px] px-4 rounded-full text-xs font-semibold border transition-colors shrink-0 cursor-pointer',
                selectedCategory === 'all'
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
              )}
            >
              Todos
            </button>
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                aria-pressed={selectedCategory === cat}
                onClick={() => setSelectedCategory(cat)}
                className={cn(
                  'min-h-[44px] px-4 rounded-full text-xs font-semibold border transition-colors shrink-0 cursor-pointer',
                  selectedCategory === cat
                    ? 'bg-[#072720] text-white border-[#072720]'
                    : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
                )}
              >
                {cat}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Results Count */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p aria-live="polite" className="text-sm font-semibold text-stone-700">
          {filteredProducts.length}{' '}
          {filteredProducts.length === 1 ? 'producto disponible' : 'productos disponibles'}
        </p>

        {hasFilters && (
          <button
            type="button"
            onClick={() => {
              setQuery('');
              setSelectedCategory('all');
            }}
            className="min-h-[44px] inline-flex items-center gap-1.5 px-3 rounded-xl text-xs font-semibold text-[#005141] hover:bg-emerald-50 transition-colors cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
            <span>Limpiar filtros</span>
          </button>
        )}
      </div>

      {/* Grid or Empty State */}
      {filteredProducts.length === 0 ? (
        <div
          role="status"
          className="rounded-2xl border border-dashed border-stone-300 bg-white p-12 text-center space-y-3"
        >
          <div className="w-12 h-12 rounded-2xl bg-stone-100 text-stone-500 flex items-center justify-center mx-auto">
            <Package className="w-6 h-6" />
          </div>
          <h3 className="font-display text-lg font-bold text-stone-900">
            No encontramos productos disponibles
          </h3>
          <p className="text-sm text-stone-600 max-w-md mx-auto">
            {hasFilters
              ? 'Prueba ajustando tu búsqueda o categoría.'
              : 'Cuando los comercios publiquen productos activos, aparecerán aquí.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {filteredProducts.map((product) => {
            const href = product.slug
              ? `/tienda/${product.storeSlug}/producto/${product.slug}`
              : `/tienda/${product.storeSlug}`;

            return (
              <Link
                key={`${product.storeId}-${product.id}`}
                href={href}
                className="group flex flex-col justify-between rounded-2xl bg-white border border-stone-200/90 shadow-xs hover:shadow-lg hover:border-stone-300 transition-all duration-200 overflow-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
              >
                <div>
                  <div className="aspect-[4/3] w-full relative overflow-hidden bg-stone-100 flex items-center justify-center">
                    {product.imageUrl ? (
                      <Image
                        src={product.imageUrl}
                        alt={product.name}
                        fill
                        sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
                        className="object-cover group-hover:scale-105 transition-transform duration-500"
                      />
                    ) : (
                      <Package className="w-10 h-10 text-stone-400" aria-hidden="true" />
                    )}
                  </div>

                  <div className="p-4 space-y-1.5">
                    <div className="flex items-center justify-between gap-2 text-[11px] font-semibold text-stone-500">
                      <span className="inline-flex items-center gap-1 truncate">
                        <Store className="w-3 h-3 text-[#005141] shrink-0" />
                        <span className="truncate">{product.storeName}</span>
                      </span>
                      <span className="shrink-0 text-[#005141]">{product.category}</span>
                    </div>

                    <h3 className="font-display text-base font-bold text-stone-900 group-hover:text-[#005141] transition-colors line-clamp-2">
                      {product.name}
                    </h3>
                  </div>
                </div>

                <div className="px-4 pb-4 pt-3 border-t border-stone-100 flex items-center justify-between">
                  <span className="text-base font-bold text-[#005141] tabular-nums">
                    {formatCurrency(product.price, product.priceCurrency || 'PEN')}
                  </span>
                  <span className="inline-flex items-center gap-1 text-xs font-semibold text-stone-700 group-hover:text-[#005141]">
                    <span>Ver</span>
                    <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}

'use client';

import React from 'react';
import { Search, X } from 'lucide-react';

interface CatalogFilterBarProps {
  storeName: string;
  query: string;
  onQueryChange: (query: string) => void;
  totalProductsCount: number;
}

export function CatalogFilterBar({
  storeName,
  query,
  onQueryChange,
  totalProductsCount,
}: CatalogFilterBarProps) {
  return (
    <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <div className="flex items-center gap-2">
          <h2 className="font-display text-2xl font-bold text-stone-900">
            Productos de {storeName}
          </h2>
          <span className="px-2.5 py-0.5 rounded-full bg-emerald-50 text-[#005141] font-bold text-xs border border-emerald-200/60">
            {totalProductsCount} {totalProductsCount === 1 ? 'producto' : 'productos'}
          </span>
        </div>
        <p className="mt-1 text-sm text-stone-600">
            Encuentra lo que buscas y añade productos a tu selección.
        </p>
      </div>

      <div className="flex items-center gap-3 w-full sm:w-auto">
        {totalProductsCount > 0 && (
          <label className="relative block flex-1 sm:w-72">
            <span className="sr-only">Buscar productos en el catálogo</span>
            <Search aria-hidden="true" className="absolute left-3.5 top-3 h-4 w-4 text-stone-400" />
            <input
              type="text"
              value={query}
              onChange={(e) => onQueryChange(e.target.value)}
              placeholder="Buscar en el catálogo..."
              className="w-full rounded-2xl border border-stone-200 bg-white py-2.5 pl-10 pr-9 text-xs sm:text-sm text-stone-900 placeholder:text-stone-400 focus:outline-none focus:ring-2 focus:ring-[#005141]/30 focus:border-[#005141] shadow-2xs transition-all"
            />
            {query && (
              <button
                type="button"
                onClick={() => onQueryChange('')}
                className="absolute right-3 top-3 text-stone-400 hover:text-stone-600 cursor-pointer"
                title="Limpiar búsqueda"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </label>
        )}

      </div>
    </div>
  );
}

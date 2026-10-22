'use client';

import React from 'react';
import { PackageOpen, SearchX } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';

interface CatalogEmptyStateProps {
  type: 'no-products' | 'no-results' | 'error' | 'unavailable';
  storeName: string;
  onReset?: () => void;
}

export function CatalogEmptyState({ type, storeName, onReset }: CatalogEmptyStateProps) {
  if (type === 'no-results') {
    return (
      <div className="col-span-full rounded-[12px] bg-white p-8 sm:p-12 text-center border border-stone-200/90 shadow-2xs">
        <SearchX className="mx-auto mb-4 h-10 w-10 text-stone-400" />
        <h3 className="font-display text-lg font-bold text-stone-900">
          No hay productos que coincidan con tu búsqueda
        </h3>
        <p className="mt-2 text-sm text-stone-600 max-w-md mx-auto">
          Prueba escribiendo otra palabra o selecciona otra categoría de {storeName}.
        </p>
        {onReset && (
          <Button
            variant="outline"
            className="mt-5 rounded-full border-stone-300 hover:bg-stone-50 text-xs font-bold px-6 py-2.5 transition-all active:scale-95"
            onClick={onReset}
          >
            Ver todos los productos
          </Button>
        )}
      </div>
    );
  }

  if (type === 'error') {
    return (
      <div className="col-span-full rounded-[12px] bg-white p-8 sm:p-12 text-center border border-red-200/90 shadow-2xs">
        <PackageOpen className="mx-auto mb-4 h-10 w-10 text-red-500" />
        <h3 className="font-display text-lg font-bold text-stone-900">
          No pudimos cargar los productos
        </h3>
        <p className="mt-2 text-sm text-stone-600 max-w-md mx-auto">
          No pudimos consultar el catálogo de {storeName}. Recarga la página para volver a intentarlo.
        </p>
        {onReset && (
          <Button className="mt-5 rounded-full bg-[#005141] hover:bg-[#00382d] text-xs font-bold px-6 py-2.5 text-white shadow-xs transition-all active:scale-95" onClick={onReset}>
            Volver a intentar
          </Button>
        )}
      </div>
    );
  }

  if (type === 'unavailable') {
    return (
      <div className="col-span-full rounded-[12px] bg-white p-8 sm:p-12 text-center border border-stone-200/90 shadow-2xs">
        <PackageOpen className="mx-auto mb-4 h-10 w-10 text-[#005141]" />
        <h3 className="font-display text-xl font-bold text-stone-900">
          El catálogo todavía no está disponible
        </h3>
        <p className="mt-2 text-sm text-stone-600 max-w-md mx-auto">
          No podemos mostrar los productos ni los precios de {storeName} en este momento. Cuando su catálogo esté disponible, aparecerá aquí.
        </p>
      </div>
    );
  }

  return (
    <div className="col-span-full rounded-[12px] bg-white p-8 sm:p-12 text-center border border-stone-200/90 shadow-2xs">
      <PackageOpen className="mx-auto mb-4 h-10 w-10 text-[#005141]" />
      <h3 className="font-display text-xl font-bold text-stone-900">
        Esta tienda aún no tiene productos publicados
      </h3>
      <p className="mt-2 text-sm text-stone-600 max-w-md mx-auto">
        {storeName} todavía no ha publicado productos para comprar desde esta página.
      </p>
    </div>
  );
}

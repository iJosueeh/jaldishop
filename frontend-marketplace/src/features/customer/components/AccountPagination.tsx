'use client';

import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface AccountPaginationProps {
  page: number;
  pageSize: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  itemLabel?: string;
  className?: string;
}

/**
 * Shared client-side pagination bar for mi-jaldishop lists and tables.
 * Enforces 44px touch targets and tabular-nums alignment.
 */
export function AccountPagination({
  page,
  pageSize,
  totalItems,
  onPageChange,
  itemLabel = 'resultados',
  className,
}: AccountPaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  if (totalItems <= pageSize) return null;

  const safePage = Math.min(Math.max(1, page), totalPages);
  const startItem = (safePage - 1) * pageSize + 1;
  const endItem = Math.min(safePage * pageSize, totalItems);

  return (
    <nav
      aria-label="Paginación"
      className={cn(
        'rounded-xl border border-[#e7e0d6] bg-white px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3',
        className
      )}
    >
      <p className="text-xs text-[#57534e] tabular-nums">
        Mostrando <strong className="text-[#1c1917]">{startItem}–{endItem}</strong> de{' '}
        <strong className="text-[#1c1917]">{totalItems}</strong> {itemLabel}
      </p>

      <div className="flex items-center justify-between sm:justify-end gap-2">
        <button
          type="button"
          disabled={safePage <= 1}
          onClick={() => onPageChange(safePage - 1)}
          className="min-h-[44px] inline-flex items-center gap-1 px-3.5 rounded-lg border border-[#e7e0d6] bg-white text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] disabled:opacity-45 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Anterior</span>
        </button>

        <span className="px-2 text-xs font-semibold text-[#57534e] tabular-nums">
          Página {safePage} de {totalPages}
        </span>

        <button
          type="button"
          disabled={safePage >= totalPages}
          onClick={() => onPageChange(safePage + 1)}
          className="min-h-[44px] inline-flex items-center gap-1 px-3.5 rounded-lg border border-[#e7e0d6] bg-white text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] disabled:opacity-45 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <span>Siguiente</span>
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </nav>
  );
}

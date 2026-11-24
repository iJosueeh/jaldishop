'use client';

import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export function getVisiblePageNumbers(current: number, total: number): (number | 'ellipsis')[] {
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

interface PaginationBarProps {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  itemLabel?: string;
  onPageChange: (page: number) => void;
  ariaLabel?: string;
  className?: string;
}

export function PaginationBar({
  currentPage,
  totalPages,
  totalItems,
  pageSize,
  itemLabel = 'elementos',
  onPageChange,
  ariaLabel = 'Paginación',
  className,
}: PaginationBarProps) {
  if (totalPages <= 1) return null;

  const safePage = Math.min(Math.max(1, currentPage), totalPages);
  const startIndex = (safePage - 1) * pageSize;
  const startItem = startIndex + 1;
  const endItem = Math.min(startIndex + pageSize, totalItems);

  return (
    <div className={cn('mt-8 flex flex-col items-center gap-3', className)}>
      <p className="text-xs font-medium text-stone-500">
        Mostrando <span className="font-semibold text-stone-800">{startItem}–{endItem}</span> de{' '}
        <span className="font-semibold text-stone-800">{totalItems}</span> {itemLabel}
      </p>

      <nav
        aria-label={ariaLabel}
        className="flex flex-wrap items-center justify-center gap-1.5"
      >
        <button
          type="button"
          onClick={() => onPageChange(safePage - 1)}
          disabled={safePage <= 1}
          aria-label="Página anterior"
          className="inline-flex h-9 items-center gap-1 rounded-xl border border-stone-200 bg-white px-3 text-xs font-semibold text-stone-700 transition-colors hover:border-stone-300 hover:bg-stone-50 disabled:cursor-not-allowed disabled:opacity-40 cursor-pointer"
        >
          <ChevronLeft className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Anterior</span>
        </button>

        {getVisiblePageNumbers(safePage, totalPages).map((entry, idx) =>
          entry === 'ellipsis' ? (
            <span
              key={`ellipsis-${idx}`}
              aria-hidden="true"
              className="px-1.5 text-xs font-semibold text-stone-400"
            >
              …
            </span>
          ) : (
            <button
              key={entry}
              type="button"
              onClick={() => onPageChange(entry)}
              aria-label={`Página ${entry}`}
              aria-current={entry === safePage ? 'page' : undefined}
              className={cn(
                'inline-flex h-9 min-w-9 items-center justify-center rounded-xl px-3 text-xs font-bold transition-all cursor-pointer',
                entry === safePage
                  ? 'bg-[#005141] text-white shadow-sm'
                  : 'border border-stone-200 bg-white text-stone-700 hover:border-stone-300 hover:bg-stone-50'
              )}
            >
              {entry}
            </button>
          )
        )}

        <button
          type="button"
          onClick={() => onPageChange(safePage + 1)}
          disabled={safePage >= totalPages}
          aria-label="Página siguiente"
          className="inline-flex h-9 items-center gap-1 rounded-xl border border-stone-200 bg-white px-3 text-xs font-semibold text-stone-700 transition-colors hover:border-stone-300 hover:bg-stone-50 disabled:cursor-not-allowed disabled:opacity-40 cursor-pointer"
        >
          <span className="hidden sm:inline">Siguiente</span>
          <ChevronRight className="h-3.5 w-3.5" />
        </button>
      </nav>
    </div>
  );
}

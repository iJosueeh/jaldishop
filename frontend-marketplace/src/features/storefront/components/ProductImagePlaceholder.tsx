'use client';

import {
  CakeSlice,
  Coffee,
  Croissant,
  PackageOpen,
  ShoppingBag,
  UtensilsCrossed,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export type ProductPlaceholderVariant = 'card' | 'detail' | 'thumb';

interface ProductImagePlaceholderProps {
  variant?: ProductPlaceholderVariant;
  category?: string;
  className?: string;
}

function getCategoryIcon(category?: string) {
  const normalized = (category ?? '').toLowerCase();
  if (/pan|horne|boller|masa|bakery/.test(normalized)) return Croissant;
  if (/torta|pastel|postre|dulce|cake|reposter/.test(normalized)) return CakeSlice;
  if (/bebida|caf[eé]|jugo|t[eé]|barista|drink/.test(normalized)) return Coffee;
  if (/comida|plato|almuerzo|cena|salado|restaur|men[uú]/.test(normalized)) return UtensilsCrossed;
  if (/abarrote|mercado|tienda|despensa|bodega/.test(normalized)) return ShoppingBag;
  return PackageOpen;
}

/**
 * Unified editorial placeholder for products without a valid photograph.
 * Aligned to DESIGN.md palette (#faf7f2, #072720, #005141, #e7e0d6, #36f4a4).
 * Purely decorative (aria-hidden="true") so screen readers rely on the product heading/link.
 */
export function ProductImagePlaceholder({
  variant = 'card',
  category,
  className,
}: ProductImagePlaceholderProps) {
  const Icon = getCategoryIcon(category);

  if (variant === 'thumb') {
    return (
      <div
        aria-hidden="true"
        className={cn(
          'relative flex h-full w-full items-center justify-center overflow-hidden bg-[#faf7f2] text-[#005141]',
          className
        )}
      >
        <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-[#e7e0d6] bg-white/90">
          <Icon className="h-4 w-4 text-[#005141]" strokeWidth={1.5} />
        </div>
      </div>
    );
  }

  return (
    <div
      aria-hidden="true"
      className={cn(
        'relative flex h-full w-full flex-col items-center justify-center overflow-hidden bg-[#faf7f2] px-4 text-center select-none',
        className
      )}
    >
      {/* Subtle Midnight Greenhouse ambient glow */}
      <div
        className="pointer-events-none absolute inset-0 opacity-70"
        style={{
          background:
            'radial-gradient(circle at 50% 42%, rgba(0, 81, 65, 0.08) 0%, rgba(250, 247, 242, 0) 68%)',
        }}
      />

      <div
        className={cn(
          'relative flex items-center justify-center rounded-2xl border border-[#e7e0d6] bg-white shadow-2xs transition-transform duration-200 ease-out motion-reduce:transition-none [@media(hover:hover)]:group-hover:scale-105',
          variant === 'detail' ? 'h-16 w-16' : 'h-12 w-12'
        )}
      >
        <Icon
          className={cn('text-[#005141]', variant === 'detail' ? 'h-7 w-7' : 'h-5 w-5')}
          strokeWidth={1.5}
        />
        <span className="absolute -top-1 -right-1 h-2.5 w-2.5 rounded-full border-2 border-[#faf7f2] bg-[#36f4a4]" />
      </div>

      {variant === 'detail' && (
        <p className="relative mt-3 text-xs font-medium tracking-wide text-[#072720]/75">
          Sin fotografía publicada
        </p>
      )}
    </div>
  );
}

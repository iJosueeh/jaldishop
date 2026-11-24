'use client';

import Link from 'next/link';
import { ShoppingCart } from 'lucide-react';
import { useStorefrontCart } from './StorefrontCartProvider';

export function StorefrontCart() {
  const { count } = useStorefrontCart();

  return (
    <Link
      href="/carrito"
      aria-label={`Carrito, ${count} ${count === 1 ? 'producto' : 'productos'}`}
      className="relative flex h-10 w-10 items-center justify-center rounded-full border border-white/20 text-white transition hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
    >
      <ShoppingCart className="h-5 w-5" aria-hidden="true" />
      {count > 0 && (
        <span
          aria-hidden="true"
          className="absolute -right-1 -top-1 flex min-w-5 h-5 items-center justify-center rounded-full bg-[#36f4a4] px-1 text-[10px] font-bold text-[#02090a] shadow-xs"
        >
          {count}
        </span>
      )}
    </Link>
  );
}

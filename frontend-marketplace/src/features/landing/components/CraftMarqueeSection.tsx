'use client';

import React, { useState, useEffect } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowUpRight, Package, Store } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

export interface MarqueeItem {
  id: string;
  name: string;
  category: string;
  store: string;
  storeSlug: string;
  productSlug?: string;
  price: string;
  badge?: string;
  badgeVariant?: 'jade' | 'terracotta' | 'amber';
  imageUrl?: string | null;
}

export const DEFAULT_ROTATING_PHRASES: string[] = [
  'Capacidad operativa en tiempo real',
  'Pedidos y despachos bajo demanda',
  'Talleres, gastronomía y creadores locales',
  'Disponibilidad de slots al instante',
  'Creaciones por encargo sin sobreventa',
];

export interface CraftMarqueeSectionProps {
  title?: string;
  phrases?: string[];
  subtitle?: string;
  items?: MarqueeItem[];
}

export function CraftMarqueeSection({
  title,
  phrases = DEFAULT_ROTATING_PHRASES,
  subtitle = 'Productos reales publicados por comercios locales en JaldiShop',
  items = [],
}: CraftMarqueeSectionProps) {
  const [phraseIndex, setPhraseIndex] = useState(0);
  const [isFading, setIsFading] = useState(false);

  useEffect(() => {
    if (title || !phrases.length) return;

    const interval = setInterval(() => {
      setIsFading(true);
      const timeout = setTimeout(() => {
        setPhraseIndex((prev) => (prev + 1) % phrases.length);
        setIsFading(false);
      }, 250);

      return () => clearTimeout(timeout);
    }, 3500);

    return () => clearInterval(interval);
  }, [title, phrases]);

  const activeTitle = title || phrases[phraseIndex] || DEFAULT_ROTATING_PHRASES[0];
  const duplicatedItems = items.length > 0 ? [...items, ...items] : [];

  return (
    <section className="py-10 bg-[#090b0c] relative">
      {/* Subtle Section Title Ribbon */}
      <div className="max-w-7xl mx-auto px-6 mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5 text-xs font-semibold text-white uppercase tracking-wider">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#161c1e] border border-[#232b2f] text-emerald-400 text-[10px] font-semibold tracking-wide">
            <span className="relative flex h-1.5 w-1.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-emerald-500" />
            </span>
            <span>EN VIVO</span>
          </span>

          <span
            className={`transition-all duration-300 text-xs font-semibold text-white uppercase tracking-wider ${
              isFading ? 'opacity-0 translate-y-0.5' : 'opacity-100 translate-y-0'
            }`}
          >
            {activeTitle}
          </span>

          <span className="text-stone-700 hidden sm:inline">•</span>
          <span className="text-[11px] font-normal text-stone-400 normal-case hidden sm:inline">
            {subtitle}
          </span>
        </div>

        <Link
          href="/tiendas"
          className="text-xs font-semibold text-[#feae2c] hover:text-[#ea580c] inline-flex items-center gap-1 transition-all"
        >
          <span>Ver todas las tiendas</span>
          <ArrowUpRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {duplicatedItems.length === 0 ? (
        <div className="max-w-7xl mx-auto px-6">
          <div className="rounded-[12px] border border-[#1e2426] bg-[#121618] px-6 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3 text-sm text-stone-300">
              <Store className="w-5 h-5 text-[#36f4a4] shrink-0" />
              <span>Explora el directorio de tiendas locales y descubre sus productos disponibles.</span>
            </div>
            <Link
              href="/productos"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-[#36f4a4] hover:underline shrink-0"
            >
              <span>Ir al catálogo de productos</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      ) : (
        <div className="relative w-full overflow-hidden before:pointer-events-none before:absolute before:inset-y-0 before:left-0 before:w-16 sm:before:w-28 before:bg-gradient-to-r before:from-[#090b0c] before:to-transparent before:z-10 after:pointer-events-none after:absolute after:inset-y-0 after:right-0 after:w-16 sm:after:w-28 after:bg-gradient-to-l after:from-[#090b0c] after:to-transparent after:z-10">
          <div className="animate-marquee gap-5 flex items-center py-2 px-4">
            {duplicatedItems.map((item, index) => {
              const targetHref = item.productSlug
                ? `/tienda/${item.storeSlug}/producto/${item.productSlug}`
                : `/tienda/${item.storeSlug}`;

              return (
                <Link
                  key={`${item.id}-${index}`}
                  href={targetHref}
                  className="group shrink-0 block"
                >
                  <div className="w-72 sm:w-80 bg-[#121618] rounded-[12px] p-3.5 border border-[#1e2426] shadow-sm transition-all duration-300 hover:border-stone-500 hover:-translate-y-1 cursor-pointer relative overflow-hidden">
                    <div className="relative h-40 w-full rounded-[10px] overflow-hidden bg-stone-900 mb-3 flex items-center justify-center">
                      {item.imageUrl ? (
                        <Image
                          src={item.imageUrl}
                          alt={item.name}
                          fill
                          sizes="320px"
                          className="object-cover transition-transform duration-700 ease-out group-hover:scale-105"
                        />
                      ) : (
                        <Package className="w-9 h-9 text-stone-600" aria-hidden="true" />
                      )}
                      <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-black/20" />

                      {item.badge && (
                        <div className="absolute top-2.5 left-2.5 z-10">
                          <Badge
                            variant={item.badgeVariant || 'jade'}
                            size="sm"
                            className="shadow-xs font-semibold text-[10px]"
                          >
                            {item.badge}
                          </Badge>
                        </div>
                      )}
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center justify-between text-[11px] text-stone-400 font-medium gap-2">
                        <span className="truncate">{item.category}</span>
                        <span className="text-stone-300 font-medium group-hover:text-white transition-colors truncate">
                          {item.store}
                        </span>
                      </div>

                      <h4 className="text-sm font-semibold text-white group-hover:text-[#feae2c] transition-colors truncate">
                        {item.name}
                      </h4>

                      <div className="flex items-center justify-between pt-1 border-t border-[#1e2426] mt-2">
                        <span className="font-mono font-semibold text-sm text-white tabular-nums">
                          {item.price}
                        </span>
                        <span className="text-[11px] font-bold text-[#ea580c] flex items-center gap-0.5 group-hover:translate-x-0.5 transition-transform">
                          <span>Ver producto</span>
                          <ArrowUpRight className="w-3 h-3" />
                        </span>
                      </div>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </section>
  );
}

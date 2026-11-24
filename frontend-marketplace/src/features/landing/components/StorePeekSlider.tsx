'use client';

import React, { useRef, useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  Star,
  Clock,
  Bike,
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  Store,
} from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import { PublicStore } from '@/features/storefront/types/storefront.types';

export interface StorePeekSliderProps {
  stores: PublicStore[];
}

export type StoreCategoryFilter = 'ALL' | 'PANADERIA' | 'REPOSTERIA' | 'CAFE' | 'SALADOS';

interface FilterOption {
  id: StoreCategoryFilter;
  label: string;
  emoji: string;
  keywords: string[];
}

export const FILTER_OPTIONS: FilterOption[] = [
  { id: 'ALL', label: 'Todos', emoji: '✨', keywords: [] },
  {
    id: 'PANADERIA',
    label: 'Panadería & Masas',
    emoji: '🥐',
    keywords: ['panaderia', 'panadería', 'pan', 'masa', 'bagel', 'croissant', 'hogaza', 'horno'],
  },
  {
    id: 'REPOSTERIA',
    label: 'Repostería & Postres',
    emoji: '🍰',
    keywords: ['reposteria', 'repostería', 'pasteleria', 'pastelería', 'dulce', 'torta', 'postre', 'cake'],
  },
  {
    id: 'CAFE',
    label: 'Café de Especialidad',
    emoji: '☕',
    keywords: ['cafe', 'café', 'barista', 'cafeteria', 'cafetería', 'espresso', 'grano'],
  },
  {
    id: 'SALADOS',
    label: 'Cocinas & Pizzas',
    emoji: '🍕',
    keywords: ['focaccia', 'pizza', 'salado', 'brunch', 'empanada', 'cocina'],
  },
];

export interface InferredCraftProfile {
  category: string;
  iconEmoji: string;
  logoBg: string;
  badge: string;
  badgeVariant: 'jade' | 'terracotta' | 'amber';
}

const CRAFT_CATEGORY_PRESETS: InferredCraftProfile[] = [
  {
    category: 'Panadería & Masa Madre',
    iconEmoji: '🥐',
    logoBg: 'bg-amber-900',
    badge: 'Horno Diario',
    badgeVariant: 'amber',
  },
  {
    category: 'Pastelería & Postres',
    iconEmoji: '🍰',
    logoBg: 'bg-[#9f1239]',
    badge: 'Repostería de Autor',
    badgeVariant: 'terracotta',
  },
  {
    category: 'Café de Especialidad',
    iconEmoji: '☕',
    logoBg: 'bg-[#451a03]',
    badge: 'Grano de Origen',
    badgeVariant: 'jade',
  },
  {
    category: 'Cocinas & Pizzas',
    iconEmoji: '🍕',
    logoBg: 'bg-[#c2410c]',
    badge: 'Masa Fermentada',
    badgeVariant: 'amber',
  },
];

export function inferStoreCraftProfile(
  store: Partial<PublicStore>,
  seed: string
): InferredCraftProfile {
  const combinedText = `${store.name || ''} ${store.slug || ''} ${store.description || ''} ${store.tagline || ''} ${store.category || ''}`.toLowerCase();

  if (/cafe|café|cafeteria|cafetería|coffee|barista|espresso|villa rica|grano|tueste|latte|cold brew|brew|filtrado/.test(combinedText)) {
    return CRAFT_CATEGORY_PRESETS[2];
  }
  if (/pizza|pizzeria|pizzería|focaccia|focacceria|salado|empanada|brunch|sandwich|pasta|italiana|comida|lunch|cena/.test(combinedText)) {
    return CRAFT_CATEGORY_PRESETS[3];
  }
  if (/postre|dulce|torta|cake|pasteleria|pastelería|reposteria|repostería|cheesecake|alfajor|chocolate|galleta|cookie|tartaleta|brownie|muffin/.test(combinedText)) {
    return CRAFT_CATEGORY_PRESETS[1];
  }
  if (/pan|panaderia|panadería|masa|bagel|croissant|hogaza|sourdough|ciabatta|horno|boulangerie|panes/.test(combinedText)) {
    return CRAFT_CATEGORY_PRESETS[0];
  }

  if (
    store.category &&
    store.category.trim() !== '' &&
    store.category !== 'Gastronomía Artesanal' &&
    store.category !== 'General'
  ) {
    return {
      category: store.category,
      iconEmoji: '✨',
      logoBg: 'bg-[#005141]',
      badge: 'Verificado',
      badgeVariant: 'jade',
    };
  }

  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  return CRAFT_CATEGORY_PRESETS[Math.abs(hash) % CRAFT_CATEGORY_PRESETS.length];
}

export function formatStoreData(store: Partial<PublicStore>) {
  const name = store.name?.trim() || 'Comercio Local';
  const slug = store.slug || 'tienda-local';
  const seed = `${slug}-${name}`;

  const craftProfile = inferStoreCraftProfile(store, seed);

  const bannerUrl = store.bannerUrl || null;
  const logoUrl = store.logoUrl || null;
  const iconEmoji = store.iconEmoji || craftProfile.iconEmoji;
  const logoBg = store.logoBg || craftProfile.logoBg;

  const category =
    store.category &&
    store.category.trim() !== '' &&
    store.category !== 'Gastronomía Artesanal' &&
    store.category !== 'General'
      ? store.category
      : craftProfile.category;

  const description =
    store.tagline ||
    store.description ||
    'Comercio local con reserva de horarios y atención directa.';

  const rating =
    typeof store.rating === 'number' && store.rating > 0 ? store.rating : null;

  const reviewsCount =
    typeof store.reviewsCount === 'number' && store.reviewsCount > 0
      ? store.reviewsCount
      : null;

  const prepTime =
    typeof store.preparationTimeMinutes === 'number' && store.preparationTimeMinutes > 0
      ? `${store.preparationTimeMinutes} min de preparación`
      : 'Turnos programados';

  const deliveryFee =
    typeof store.deliveryFee === 'number'
      ? store.deliveryFee
      : typeof (store as { deliveryFeeAmount?: number }).deliveryFeeAmount === 'number'
      ? (store as { deliveryFeeAmount?: number }).deliveryFeeAmount
      : undefined;

  let deliveryText = 'Envío disponible';
  if (deliveryFee === 0) {
    deliveryText = 'Envío Gratis';
  } else if (typeof deliveryFee === 'number' && deliveryFee > 0) {
    deliveryText = `Envío S/ ${deliveryFee.toFixed(2)}`;
  } else if (store.pickupEnabled && !store.deliveryEnabled) {
    deliveryText = 'Solo retiro en local';
  }

  const badge = store.badge || craftProfile.badge;
  const badgeVariant = store.badgeVariant || craftProfile.badgeVariant;

  return {
    id: store.id || slug,
    name,
    slug,
    bannerUrl,
    logoUrl,
    iconEmoji,
    logoBg,
    category,
    description,
    rating,
    reviewsCount,
    prepTime,
    deliveryText,
    badge,
    badgeVariant,
  };
}

function StoreCardItem({ rawStore }: { rawStore: Partial<PublicStore> }) {
  const store = formatStoreData(rawStore);
  const hasReviews = store.rating !== null && store.reviewsCount !== null;
  const [bannerError, setBannerError] = useState(false);
  const [logoError, setLogoError] = useState(false);

  return (
    <div className="w-[84vw] sm:w-[350px] lg:w-[380px] shrink-0 snap-start flex flex-col group">
      <Link
        href={`/tienda/${store.slug}`}
        aria-label={`Ver tienda: ${store.name}`}
        className="block h-full rounded-[12px] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#072720]"
      >
        <div className="h-full flex flex-col justify-between bg-white rounded-[12px] border border-stone-200/90 shadow-md group-hover:shadow-xl group-hover:border-stone-400 transition-all duration-300 overflow-hidden relative">
          {/* Top Cover Banner */}
          <div className="aspect-[16/9] w-full relative overflow-hidden bg-gradient-to-br from-[#072720] via-[#061a1c] to-[#02090a]">
            {store.bannerUrl && !bannerError ? (
              <Image
                src={store.bannerUrl}
                alt={store.name}
                fill
                sizes="(max-width: 768px) 85vw, 380px"
                onError={() => setBannerError(true)}
                className="object-cover object-center transition-transform duration-700 ease-out group-hover:scale-110 filter brightness-[0.92] group-hover:brightness-100"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-[#36f4a4]/30">
                <Store className="w-12 h-12" />
              </div>
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/20" />

            {/* Top Badges */}
            <div className="absolute top-3.5 left-3.5 right-3.5 sm:top-4 sm:left-4 sm:right-4 flex flex-wrap items-center justify-between gap-2 z-10">
              <Badge
                variant={store.badgeVariant}
                size="sm"
                className="bg-white/95 backdrop-blur-md shadow-md border-0 font-bold"
              >
                Negocio local
              </Badge>

              {hasReviews && (
                <div className="text-xs font-bold text-[#1c1917] flex items-center gap-1.5 bg-white/95 backdrop-blur-md px-2.5 sm:px-3 py-1 rounded-full shadow-md">
                  <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                  <span>{store.rating!.toFixed(1)}</span>
                  <span className="text-[#78716c] text-[10px] font-medium">
                    ({store.reviewsCount})
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Body Content */}
          <div className="p-4 sm:p-6 pt-0 relative flex-1 flex flex-col justify-between">
            {/* Floating Store Logo / Avatar */}
            <div className="flex items-end justify-between gap-3 -mt-7 sm:-mt-8 mb-3 sm:mb-4 relative z-10">
              <div
                className={`w-13 h-13 sm:w-14 sm:h-14 shrink-0 rounded-2xl ${store.logoBg} text-white flex items-center justify-center shadow-lg font-black text-2xl border-2 border-white group-hover:scale-105 transition-transform duration-300 overflow-hidden`}
              >
                {store.logoUrl && !logoError ? (
                  <Image
                    src={store.logoUrl}
                    alt={store.name}
                    width={56}
                    height={56}
                    onError={() => setLogoError(true)}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <span aria-hidden="true">{store.iconEmoji}</span>
                )}
              </div>

              <span className="text-[11px] sm:text-xs font-bold text-[#57534e] bg-[#faf7f2] border border-stone-200/80 px-2.5 sm:px-3 py-1 rounded-xl shadow-2xs">
                {store.category}
              </span>
            </div>

            {/* Title & Description */}
            <div className="space-y-1.5 sm:space-y-2">
              <div className="flex items-center gap-1.5">
                <h3 className="min-h-14 line-clamp-2 text-lg sm:text-xl font-bold text-[#1c1917] group-hover:text-[#005141] transition-colors leading-snug">
                  {store.name}
                </h3>
              </div>
              <p className="min-h-10 text-sm text-[#57534e] line-clamp-2 leading-relaxed">
                {store.description}
              </p>
            </div>

            {/* Bottom Operational Metadata */}
            <div className="mt-6 pt-4 border-t border-stone-100 flex flex-wrap items-center justify-between gap-2 text-xs text-[#57534e]">
              <div className="flex items-center gap-1.5 font-medium">
                <Clock className="w-3.5 h-3.5 text-[#a8a29e]" />
                <span>{store.prepTime}</span>
              </div>

              <div className="flex items-center gap-1.5 font-semibold text-[#1c1917]">
                <Bike className="w-3.5 h-3.5 text-[#ea580c]" />
                <span>{store.deliveryText}</span>
              </div>
            </div>

            {/* Call-to-action bar */}
            <div className="mt-4 rounded-full bg-[#02090a] px-5 py-2.5 flex items-center justify-between text-sm font-medium text-white group-hover:bg-[#072720] transition-colors shadow-xs">
              <span>Ver tienda</span>
              <ArrowRight aria-hidden="true" className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        </div>
      </Link>
    </div>
  );
}

export function StorePeekSlider({ stores }: StorePeekSliderProps) {
  const sliderRef = useRef<HTMLDivElement>(null);
  const [selectedFilter, setSelectedFilter] = useState<StoreCategoryFilter>('ALL');
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);
  const [scrollProgress, setScrollProgress] = useState(0);

  const filteredStores = stores.filter((s) => {
    if (selectedFilter === 'ALL') return true;
    const opt = FILTER_OPTIONS.find((f) => f.id === selectedFilter);
    if (!opt) return true;
    const formatted = formatStoreData(s);
    const text = `${formatted.name} ${formatted.category} ${formatted.description} ${(s.keywords || []).join(' ')}`.toLowerCase();
    return opt.keywords.some((kw) => text.includes(kw));
  });

  const checkScroll = useCallback(() => {
    const el = sliderRef.current;
    if (!el) return;

    const { scrollLeft, scrollWidth, clientWidth } = el;
    setCanScrollLeft(scrollLeft > 10);
    setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 10);

    const maxScroll = scrollWidth - clientWidth;
    if (maxScroll > 0) {
      setScrollProgress(Math.min(100, Math.max(0, (scrollLeft / maxScroll) * 100)));
    }
  }, []);

  const handleFilterChange = (filterId: StoreCategoryFilter) => {
    setSelectedFilter(filterId);
    if (sliderRef.current) {
      if (typeof sliderRef.current.scrollTo === 'function') {
        sliderRef.current.scrollTo({ left: 0, behavior: 'smooth' });
      } else {
        sliderRef.current.scrollLeft = 0;
      }
    }
  };

  useEffect(() => {
    checkScroll();
    window.addEventListener('resize', checkScroll);
    return () => window.removeEventListener('resize', checkScroll);
  }, [checkScroll, filteredStores.length]);

  const scrollByAmount = (amount: number) => {
    if (sliderRef.current) {
      if (typeof sliderRef.current.scrollBy === 'function') {
        sliderRef.current.scrollBy({ left: amount, behavior: 'smooth' });
      } else {
        sliderRef.current.scrollLeft += amount;
      }
    }
  };

  if (!stores || stores.length === 0) {
    return null;
  }

  return (
    <div className="relative space-y-5">
      {/* Category Filter Pills & Slider Prev/Next Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-1 border-b border-stone-200/80">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1 pt-1">
          {FILTER_OPTIONS.map((opt) => {
            const isActive = selectedFilter === opt.id;
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => handleFilterChange(opt.id)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-black transition-all cursor-pointer flex items-center gap-1.5 shrink-0 border ${
                  isActive
                    ? 'bg-[#005141] text-white border-[#005141] shadow-md shadow-[#005141]/20 scale-[1.02]'
                    : 'bg-white text-stone-600 border-stone-200 hover:border-stone-300 hover:bg-stone-50'
                }`}
              >
                <span aria-hidden="true">{opt.emoji}</span>
                <span>{opt.label}</span>
              </button>
            );
          })}
        </div>

        <div className="flex items-center justify-between sm:justify-end gap-3">
          <span className="text-[11px] font-bold text-[#78716c] font-mono">
            {filteredStores.length} {filteredStores.length === 1 ? 'comercio' : 'comercios'}
          </span>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => scrollByAmount(-380)}
              disabled={!canScrollLeft}
              aria-label="Ver comercios anteriores"
              className="w-10 h-10 rounded-full bg-white border border-stone-200 shadow-sm flex items-center justify-center text-stone-700 hover:text-[#02090a] hover:border-stone-400 hover:bg-stone-50 disabled:opacity-30 disabled:pointer-events-none transition-all duration-200 cursor-pointer group"
            >
              <ChevronLeft className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" />
            </button>

            <button
              type="button"
              onClick={() => scrollByAmount(380)}
              disabled={!canScrollRight}
              aria-label="Ver siguientes comercios"
              className="w-10 h-10 rounded-full bg-[#02090a] text-white shadow-sm flex items-center justify-center hover:bg-[#072720] disabled:opacity-30 disabled:pointer-events-none transition-all duration-200 cursor-pointer group"
            >
              <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
            </button>
          </div>
        </div>
      </div>

      {/* Peek Slider Track or Filter Empty State */}
      {filteredStores.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-3xl border border-stone-200 p-8 space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto text-xl">
            <Store className="w-6 h-6" />
          </div>
          <h4 className="text-base font-bold text-[#1c1917]">No hay comercios en esta categoría</h4>
          <p className="text-xs text-[#57534e]">Prueba seleccionando otra categoría o ver todos los rubros.</p>
          <button
            type="button"
            onClick={() => handleFilterChange('ALL')}
            className="text-xs font-bold text-[#005141] hover:underline cursor-pointer"
          >
            Ver todos los comercios
          </button>
        </div>
      ) : (
        <div
          ref={sliderRef}
          onScroll={checkScroll}
          className="flex gap-5 sm:gap-6 overflow-x-auto scroll-smooth snap-x snap-mandatory no-scrollbar pb-6 pt-2 -mx-4 px-4 sm:-mx-6 sm:px-6"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          {filteredStores.map((rawStore, idx) => (
            <StoreCardItem
              key={rawStore.id || rawStore.slug || `store-${idx}`}
              rawStore={rawStore}
            />
          ))}
        </div>
      )}

      {filteredStores.length > 1 && (
        <div className="w-full h-1 bg-stone-200/70 rounded-full overflow-hidden max-w-xs mx-auto">
          <div
            className="h-full bg-[#005141] rounded-full transition-all duration-150"
            style={{ width: `${Math.max(15, scrollProgress)}%` }}
          />
        </div>
      )}
    </div>
  );
}

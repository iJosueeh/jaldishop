'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  Store,
  MapPin,
  MessageCircle,
  Bike,
  ShoppingBag,
  ArrowRight,
  Star,
  Share2,
  Globe,
} from 'lucide-react';
import { PublicStore } from '../types/storefront.types';
import { formatStoreData } from '@/features/landing/components/StorePeekSlider';
import { cn } from '@/shared/utils/cn';

export interface MarketplaceStoreCardProps {
  rawStore: PublicStore;
  layout?: 'grid' | 'list';
}

export function MarketplaceStoreCard({
  rawStore,
  layout = 'grid',
}: MarketplaceStoreCardProps) {
  const store = formatStoreData(rawStore);
  const [bannerError, setBannerError] = useState(false);
  const [logoError, setLogoError] = useState(false);

  const isActive = rawStore.status === 'ACTIVE';
  const hasReviews = store.rating !== null && store.reviewsCount !== null;
  const whatsappDisplay = rawStore.whatsappNumber || rawStore.contactPhone;
  const hasSocials = Boolean(rawStore.instagramUrl || rawStore.facebookUrl);
  const isList = layout === 'list';

  return (
    <Link
      href={`/tienda/${store.slug}`}
      className={cn(
        'group rounded-2xl bg-white border border-stone-200/90 shadow-xs hover:shadow-lg hover:border-stone-300 transition-all duration-200 overflow-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
        isList
          ? 'flex flex-col md:flex-row'
          : 'flex flex-col justify-between h-full'
      )}
    >
      {/* Top / Left Banner */}
      <div
        className={cn(
          'relative overflow-hidden bg-gradient-to-br from-[#072720] via-[#061a1c] to-[#02090a] flex items-center justify-center shrink-0',
          isList ? 'aspect-[16/9] md:aspect-auto md:w-64' : 'aspect-[16/9] w-full'
        )}
      >
        {store.bannerUrl && !bannerError ? (
          <Image
            src={store.bannerUrl}
            alt={store.name}
            fill
            loading="lazy"
            sizes={
              isList
                ? '(max-width: 768px) 100vw, 256px'
                : '(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw'
            }
            onError={() => setBannerError(true)}
            className="object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <Store className="w-10 h-10 text-[#36f4a4]/30" aria-hidden="true" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-black/15" />

        {/* Category pill */}
        <span className="absolute top-3 left-3 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-white/95 text-[#072720] shadow-xs">
          {store.category}
        </span>

        {hasReviews && (
          <div className="absolute top-3 right-3 bg-white/95 backdrop-blur-sm px-2.5 py-1 rounded-full text-xs font-bold text-stone-900 flex items-center gap-1 shadow-xs">
            <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
            <span>{store.rating!.toFixed(1)}</span>
            <span className="text-stone-500 text-[11px]">({store.reviewsCount})</span>
          </div>
        )}
      </div>

      {/* Main Content Body (aligned with merchant store-preview-card) */}
      <div className="flex-1 flex flex-col justify-between p-5 gap-4">
        <div className="space-y-3">
          {/* Logo/Initial + Name + Live Status */}
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl overflow-hidden shrink-0 border border-stone-200/80 bg-[#072720] text-[#36f4a4] flex items-center justify-center font-bold text-base shadow-xs">
              {store.logoUrl && !logoError ? (
                <Image
                  src={store.logoUrl}
                  alt={`Logo de ${store.name}`}
                  width={48}
                  height={48}
                  loading="lazy"
                  onError={() => setLogoError(true)}
                  className="w-full h-full object-cover"
                />
              ) : (
                <span>{store.name.charAt(0).toUpperCase()}</span>
              )}
            </div>

            <div className="min-w-0 flex-1">
              <h3 className="font-display text-base sm:text-lg font-bold text-stone-900 group-hover:text-[#005141] transition-colors truncate">
                {store.name}
              </h3>
              <span className="text-xs text-stone-600 inline-flex items-center gap-1.5 font-medium">
                <span
                  className={cn(
                    'w-2 h-2 rounded-full',
                    isActive ? 'bg-emerald-500' : 'bg-stone-400'
                  )}
                  aria-hidden="true"
                />
                {isActive ? 'Abierto ahora' : 'En pausa'}
              </span>
            </div>
          </div>

          {/* Description */}
          <p className="text-sm text-stone-600 line-clamp-2 leading-relaxed">
            {store.description}
          </p>

          {/* Address, WhatsApp & Social Indicators */}
          {(rawStore.address || whatsappDisplay || hasSocials) && (
            <div className="pt-2 border-t border-stone-100 space-y-1.5 text-xs text-stone-600">
              {rawStore.address && (
                <div className="flex items-start gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-[#005141] shrink-0 mt-0.5" aria-hidden="true" />
                  <span className="line-clamp-1 font-medium text-stone-800">
                    {rawStore.address}
                    {rawStore.addressReference && (
                      <span className="text-stone-500 font-normal"> ({rawStore.addressReference})</span>
                    )}
                  </span>
                </div>
              )}

              {whatsappDisplay && (
                <div className="flex items-center gap-1.5">
                  <MessageCircle className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                  <span className="truncate">
                    WhatsApp: <strong className="text-stone-800">{whatsappDisplay}</strong>
                  </span>
                </div>
              )}

              {hasSocials && (
                <div className="flex items-center gap-3 text-[11px] text-stone-500 pt-0.5">
                  {rawStore.instagramUrl && (
                    <span className="inline-flex items-center gap-1">
                      <Share2 className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                      Instagram
                    </span>
                  )}
                  {rawStore.facebookUrl && (
                    <span className="inline-flex items-center gap-1">
                      <Globe className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                      Facebook
                    </span>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Dispatch Chips + CTA */}
        <div className="space-y-3 pt-1">
          <div className="flex items-center gap-1.5 flex-wrap">
            {rawStore.pickupEnabled && (
              <span className="px-2.5 py-1 rounded-lg bg-stone-100 text-stone-800 text-[11px] font-bold uppercase inline-flex items-center gap-1">
                <ShoppingBag className="w-3 h-3 text-[#005141]" aria-hidden="true" />
                Pickup
              </span>
            )}
            {rawStore.deliveryEnabled && (
              <span className="px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200/70 text-[#005141] text-[11px] font-bold uppercase inline-flex items-center gap-1">
                <Bike className="w-3 h-3 text-[#ea580c]" aria-hidden="true" />
                {store.deliveryText}
              </span>
            )}
          </div>

          <span className="w-full min-h-[44px] rounded-full bg-[#02090a] group-hover:bg-[#072720] text-white text-xs font-semibold px-4 flex items-center justify-between transition-colors">
            <span>Ver catálogo</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
          </span>
        </div>
      </div>
    </Link>
  );
}

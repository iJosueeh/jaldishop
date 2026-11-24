'use client';

import React from 'react';
import { PublicStore } from '../types/storefront.types';
import { StoreBanner } from './hero/StoreBanner';
import { StoreLogo } from './hero/StoreLogo';
import { StoreInfoCard } from './hero/StoreInfoCard';

interface StorefrontHeroProps {
  store: PublicStore;
}

export function StorefrontHero({ store }: StorefrontHeroProps) {
  return (
    <header className="relative w-full pb-4">
      {/* 1. Lienzo Fotográfico de Portada y Navegación Superior */}
      <StoreBanner bannerUrl={store.bannerUrl} storeName={store.name} />

      {/* 2. Tarjeta Isla de Identidad del Comercio full-width responsive */}
      <div className="w-full px-4 sm:px-6 lg:px-10 xl:px-12">
        <div className="bg-white rounded-[16px] shadow-xl shadow-stone-900/5 border border-stone-200/90 p-4 sm:p-6 lg:p-7 -mt-8 sm:-mt-10 relative z-20 transition-all">
          <div className="flex flex-col md:flex-row items-start md:items-center gap-4 sm:gap-6">
            {/* Logo squircle adaptativo con 12px radius */}
            <StoreLogo
              logoUrl={store.logoUrl}
              storeName={store.name}
              iconEmoji={store.iconEmoji}
            />

            {/* Ficha de Información, Ratings, Tags y CTA */}
            <StoreInfoCard store={store} />
          </div>
        </div>
      </div>
    </header>
  );
}

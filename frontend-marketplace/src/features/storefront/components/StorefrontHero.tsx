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

      {/* 2. Tarjeta Isla de Identidad del Comercio */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-3xl shadow-xl shadow-stone-900/5 border border-stone-200/80 p-5 sm:p-7 md:p-8 -mt-10 sm:-mt-12 md:-mt-14 relative z-20 transition-shadow">
          <div className="flex flex-col md:flex-row items-start md:items-center gap-5 sm:gap-7">
            {/* Logo squircle adaptativo */}
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

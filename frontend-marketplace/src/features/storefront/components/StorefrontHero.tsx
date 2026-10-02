import React from 'react';
import Link from 'next/link';
import { ArrowLeft, Star, MapPin, Clock } from 'lucide-react';
import { PublicStore } from '../types/storefront.types';
import { Badge } from '@/shared/components/ui/Badge';

interface StorefrontHeroProps {
  store: PublicStore;
}

export function StorefrontHero({ store }: StorefrontHeroProps) {
  return (
    <div className="relative bg-[#005141] text-white pt-24 pb-12 overflow-hidden border-b border-[#003d31]">
      {/* Background ambient warm light */}
      <div className="absolute top-0 right-1/4 w-[400px] h-[300px] bg-[#feae2c]/12 blur-[100px] pointer-events-none" />
      <div className="absolute bottom-0 left-10 w-[300px] h-[300px] bg-[#ea580c]/10 blur-[100px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between pb-6">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-bold text-[#ccfbf1] hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" /> Volver al Marketplace
          </Link>
          <Badge variant="amber" size="sm" className="bg-[#feae2c] text-[#1c1917] border-none font-bold">
            <span className="w-1.5 h-1.5 rounded-full bg-[#1c1917] animate-pulse mr-1.5" />
            Capacidad Sincronizada en Vivo
          </Badge>
        </div>

        {/* Store Header Info */}
        <div className="flex flex-col md:flex-row md:items-center gap-6">
          {/* Logo / Initials */}
          <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-gradient-to-tr from-[#ea580c] to-[#feae2c] flex items-center justify-center text-white font-extrabold text-2xl sm:text-3xl shadow-xl border-4 border-white/20 shrink-0">
            {store.name.substring(0, 2).toUpperCase()}
          </div>

          {/* Details */}
          <div className="space-y-2 flex-1">
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white tracking-tight">
                {store.name}
              </h1>
              {store.status === 'ACTIVE' && (
                <Badge variant="jade" size="sm" className="bg-white/15 text-white border-white/20">
                  Abierto hoy
                </Badge>
              )}
            </div>

            {store.description && (
              <p className="text-xs sm:text-sm text-[#ccfbf1] max-w-2xl leading-relaxed">
                {store.description}
              </p>
            )}

            {/* Quick stats & tags */}
            <div className="flex flex-wrap items-center gap-4 text-xs text-white/80 pt-1">
              {store.category && (
                <span className="text-[#feae2c] font-bold">{store.category}</span>
              )}
              {store.rating && (
                <span className="flex items-center gap-1 font-bold text-white">
                  <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                  {store.rating} ({store.reviewsCount || 0} opiniones)
                </span>
              )}
              {store.preparationTimeMinutes && (
                <span className="flex items-center gap-1 font-medium">
                  <Clock className="w-3.5 h-3.5 text-[#ccfbf1]" />
                  Preparación ~{store.preparationTimeMinutes} min
                </span>
              )}
              {store.address && (
                <span className="flex items-center gap-1 line-clamp-1 text-white/70">
                  <MapPin className="w-3.5 h-3.5 text-[#ccfbf1] shrink-0" />
                  {store.address}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

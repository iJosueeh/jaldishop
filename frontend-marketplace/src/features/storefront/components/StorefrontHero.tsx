'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowLeft, ArrowRight, Star, MapPin, Clock, Share2, ShieldCheck } from 'lucide-react';
import { PublicStore } from '../types/storefront.types';
import { Badge } from '@/shared/components/ui/Badge';
import { toast } from 'sonner';

interface StorefrontHeroProps {
  store: PublicStore;
}

export function StorefrontHero({ store }: StorefrontHeroProps) {
  const [failedBanner, setFailedBanner] = useState<string | null>(null);
  const [failedLogo, setFailedLogo] = useState<string | null>(null);
  const handleShare = () => {
    if (typeof window !== 'undefined') {
      navigator.clipboard.writeText(window.location.href);
      toast.success('¡Enlace de la tienda copiado al portapapeles!', {
        description: 'Compártelo con tus clientes o en tus redes sociales.',
      });
    }
  };

  return (
    <div className="relative bg-gradient-to-br from-[#005141] via-[#00382d] to-[#002820] text-white pt-28 sm:pt-32 pb-12 sm:pb-16 overflow-hidden border-b border-[#003d31]">
      {store.bannerUrl && failedBanner !== store.bannerUrl && (
        <div className="absolute inset-0">
          <Image src={store.bannerUrl} alt="" fill priority sizes="100vw" className="object-cover object-center" onError={() => setFailedBanner(store.bannerUrl!)} />
          <div className="absolute inset-0 bg-gradient-to-r from-[#002820]/95 via-[#002820]/80 to-[#002820]/40" />
        </div>
      )}
      {/* Background ambient warm lights */}
      <div className="absolute top-0 right-1/4 w-[450px] h-[350px] bg-[#feae2c]/15 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-0 left-10 w-[350px] h-[350px] bg-[#ea580c]/12 blur-[120px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Navigation Breadcrumb & Live Capacity Tag */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-6 border-b border-white/10">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-bold text-[#ccfbf1] hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" /> Volver al Marketplace
          </Link>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 border border-white/15 text-xs font-bold text-[#feae2c] backdrop-blur-md">
              <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-pulse" />
              Comparte esta tienda
            </span>
            <button
              onClick={handleShare}
              className="p-1.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/15 text-stone-200 hover:text-white transition-all cursor-pointer"
              title="Compartir tienda"
              aria-label="Compartir tienda"
            >
              <Share2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Store Header Info */}
        <div className="flex flex-col md:flex-row md:items-center gap-6 pt-8 sm:pt-10 motion-safe:animate-[storefront-enter_600ms_ease-out_both]">
          {/* Logo / Initials */}
          <div className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-gradient-to-tr from-[#ea580c] to-[#feae2c] flex items-center justify-center text-white font-black text-2xl sm:text-3xl shadow-2xl border-4 border-white/20 shrink-0 overflow-hidden">
            {store.logoUrl && failedLogo !== store.logoUrl
              ? <Image src={store.logoUrl} alt={`Logo de ${store.name}`} fill sizes="96px" className="object-contain bg-white p-1" onError={() => setFailedLogo(store.logoUrl!)} />
              : store.iconEmoji || store.name.substring(0, 2).toUpperCase()}
          </div>

          {/* Details */}
          <div className="space-y-2 flex-1">
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight drop-shadow-md">
                {store.name}
              </h1>
              {store.status === 'ACTIVE' && (
                <Badge variant="jade" size="sm" className="bg-emerald-500/20 text-emerald-300 border-emerald-400/30 font-bold">
                  <ShieldCheck className="w-3 h-3 inline mr-1" /> Tienda activa
                </Badge>
              )}
            </div>

            {(store.tagline || store.description) && (
              <p className="text-sm sm:text-base text-[#ccfbf1] max-w-2xl leading-relaxed">
                {store.tagline || store.description}
              </p>
            )}

            {/* Quick stats & tags */}
            <div className="flex flex-wrap items-center gap-4 text-xs text-white/90 pt-1.5">
              {store.category && (
                <span className="text-[#feae2c] font-bold bg-black/20 px-2.5 py-0.5 rounded-lg border border-white/10">
                  {store.category}
                </span>
              )}
              {store.rating && (
                <span className="flex items-center gap-1 font-bold text-white bg-black/20 px-2.5 py-0.5 rounded-lg border border-white/10">
                  <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                  {store.rating} ({store.reviewsCount || 0} opiniones)
                </span>
              )}
              {store.preparationTimeMinutes && (
                <span className="flex items-center gap-1 font-medium text-stone-200">
                  <Clock className="w-3.5 h-3.5 text-[#ccfbf1]" />
                  Preparación ~{store.preparationTimeMinutes} min
                </span>
              )}
              {store.address && (
                <span className="flex items-center gap-1 line-clamp-1 text-white/80">
                  <MapPin className="w-3.5 h-3.5 text-[#ccfbf1] shrink-0" />
                  {store.address}
                </span>
              )}
            </div>
          </div>
        </div>
        <Link href="#catalogo" className="mt-7 inline-flex items-center justify-center gap-2 rounded-2xl bg-[#ea580c] hover:bg-[#c2410c] px-6 py-3 text-sm font-bold shadow-lg shadow-black/20 transition-all hover:-translate-y-0.5 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white">
          Ver productos <ArrowRight aria-hidden="true" className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}

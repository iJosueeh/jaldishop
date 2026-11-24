'use client';

import React from 'react';
import {
  ShieldCheck,
  Star,
  MessageCircle,
  Share2,
  Globe,
} from 'lucide-react';
import { PublicStore } from '../../types/storefront.types';
import { Badge } from '@/shared/components/ui/Badge';
import { StoreKpisBar } from './StoreKpisBar';

interface StoreInfoCardProps {
  store: PublicStore;
}

export function StoreInfoCard({ store }: StoreInfoCardProps) {
  const contactNumber = (store.whatsappNumber || store.phone || store.contactPhone)?.replace(/\D/g, '');
  const whatsappUrl = contactNumber
    ? `https://wa.me/${contactNumber}?text=${encodeURIComponent(
        `¡Hola ${store.name}! Vi su tienda en Jaldishop y me gustaría hacer una consulta.`
      )}`
    : null;

  return (
    <div className="min-w-0 flex-1 space-y-4">
      {/* Cabecera con Nombre, Badges y CTAs */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
        <div className="space-y-2">
          {/* Fila de Título y Badges de Confianza */}
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="font-display text-2xl sm:text-3xl lg:text-4xl font-black text-stone-900 tracking-tight">
              {store.name}
            </h1>
            {store.status === 'ACTIVE' && (
              <Badge
                variant="jade"
                size="sm"
                className="bg-emerald-50 text-[#005141] border-emerald-200/80 font-bold px-3 py-1 rounded-full shadow-2xs"
              >
                <ShieldCheck className="w-3.5 h-3.5 inline mr-1 text-[#005141]" />
                Tienda activa
              </Badge>
            )}
            {store.category && (
              <span className="text-xs font-semibold text-stone-700 bg-stone-100 px-3 py-1 rounded-full border border-stone-200/80">
                {store.category}
              </span>
            )}
            {store.rating && (
              <span className="inline-flex items-center gap-1 text-xs font-bold text-stone-800 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-200/80 shadow-2xs">
                <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                <span>{store.rating}</span>
                {store.reviewsCount ? (
                  <span className="text-stone-500 font-normal">({store.reviewsCount})</span>
                ) : null}
              </span>
            )}
          </div>

          {/* Descripción / Eslogan */}
          {(store.tagline || store.description) && (
            <p className="text-xs sm:text-sm text-stone-600 max-w-3xl leading-relaxed">
              {store.tagline || store.description}
            </p>
          )}
        </div>

        {/* Acciones Rápidas: WhatsApp y Redes Sociales */}
        <div className="flex flex-wrap items-center gap-2 shrink-0">
          {store.instagramUrl && (
            <a
              href={store.instagramUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`Instagram de ${store.name}`}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full border border-stone-200 bg-white hover:bg-stone-50 text-xs font-semibold text-stone-700 hover:text-[#005141] transition-colors"
            >
              <Share2 className="w-3.5 h-3.5 text-[#ea580c]" />
              <span>Instagram</span>
            </a>
          )}

          {store.facebookUrl && (
            <a
              href={store.facebookUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`Facebook de ${store.name}`}
              className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full border border-stone-200 bg-white hover:bg-stone-50 text-xs font-semibold text-stone-700 hover:text-[#005141] transition-colors"
            >
              <Globe className="w-3.5 h-3.5 text-[#005141]" />
              <span>Facebook</span>
            </a>
          )}

          {whatsappUrl && (
            <a
              href={whatsappUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="min-h-[44px] inline-flex items-center gap-2 px-4 py-2 rounded-full bg-[#072720] hover:bg-[#005141] text-white text-xs sm:text-sm font-bold shadow-xs transition-all hover:-translate-y-0.5 active:scale-95"
              title="Escribir por WhatsApp"
            >
              <MessageCircle className="w-4 h-4 text-[#36f4a4]" />
              <span>WhatsApp</span>
            </a>
          )}
        </div>
      </div>

      {/* Grid Modular de KPIs Operativos (Modalidad, Envío, Horario, Ubicación) */}
      <StoreKpisBar store={store} />
    </div>
  );
}

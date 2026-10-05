'use client';

import React from 'react';
import Link from 'next/link';
import {
  ShieldCheck,
  Star,
  ArrowRight,
  MessageCircle,
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
                className="bg-emerald-50 text-emerald-800 border-emerald-300 font-bold px-2.5 py-0.5"
              >
                <ShieldCheck className="w-3.5 h-3.5 inline mr-1 text-emerald-600" />
                Tienda activa
              </Badge>
            )}
            {store.category && (
              <span className="text-xs font-bold text-[#005141] bg-emerald-50/90 px-3 py-0.5 rounded-full border border-emerald-200/80">
                {store.category}
              </span>
            )}
            {store.rating && (
              <span className="inline-flex items-center gap-1 text-xs font-bold text-stone-800 bg-amber-50 px-2.5 py-0.5 rounded-full border border-amber-200/80">
                <Star className="w-3 h-3 text-[#feae2c] fill-[#feae2c]" />
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

        {/* Acciones Rápidas: WhatsApp y Catálogo */}
        <div className="flex items-center gap-2.5 shrink-0">
          {whatsappUrl && (
            <a
              href={whatsappUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-2xl bg-emerald-50 hover:bg-emerald-100 text-[#005141] border border-emerald-200/80 text-xs sm:text-sm font-bold shadow-sm transition-all hover:-translate-y-0.5 active:scale-95"
              title="Escribir por WhatsApp"
            >
              <MessageCircle className="w-4 h-4 text-emerald-600" />
              <span>WhatsApp</span>
            </a>
          )}

          <Link
            href="#catalogo"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-[#005141] hover:bg-[#00382d] text-white text-xs sm:text-sm font-bold shadow-md shadow-[#005141]/20 transition-all hover:-translate-y-0.5 active:scale-95"
          >
            <span>Ver productos</span>
            <ArrowRight className="w-4 h-4 text-[#feae2c]" />
          </Link>
        </div>
      </div>

      {/* Grid Modular de KPIs Operativos (Modalidad, Envío, Horario, Ubicación) */}
      <StoreKpisBar store={store} />
    </div>
  );
}

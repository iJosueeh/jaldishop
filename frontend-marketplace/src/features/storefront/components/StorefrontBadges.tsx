'use client';

import React from 'react';
import { Bike, DollarSign, Clock, MessageSquare, Phone, CheckCircle2 } from 'lucide-react';
import { PublicStore } from '../types/storefront.types';
import { Card } from '@/shared/components/ui/Card';
import { formatCurrency, getWhatsAppShareUrl, formatPhoneNumber } from '@/shared/utils/formatters';

interface StorefrontBadgesProps {
  store: PublicStore;
}

export function StorefrontBadges({ store }: StorefrontBadgesProps) {
  const whatsappUrl = store.phone
    ? getWhatsAppShareUrl(
        store.phone,
        `¡Hola ${store.name}! Vi su tienda en JaldiShop y deseo hacer un pedido.`
      )
    : null;

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5 sm:gap-4">
      {/* Delivery & Recojo */}
      <Card className="p-4 flex items-center gap-3.5 bg-white border border-stone-200/90 shadow-sm rounded-2xl hover:shadow-md transition-shadow">
        <div className="w-11 h-11 rounded-2xl bg-[#f0fdfa] text-[#005141] border border-teal-100 flex items-center justify-center shrink-0">
          <Bike className="w-5 h-5" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#78716c] uppercase tracking-wider">Modalidades</div>
          <div className="text-xs sm:text-sm font-extrabold text-[#1c1917] truncate">
            {store.deliveryEnabled && store.pickupEnabled
              ? 'Delivery & Retiro'
              : store.deliveryEnabled
              ? 'Solo Delivery'
              : store.pickupEnabled ? 'Solo recojo en local' : 'Sin modalidades habilitadas'}
          </div>
        </div>
      </Card>

      {/* Tarifa de Delivery & Pedido Mínimo */}
      <Card className="p-4 flex items-center gap-3.5 bg-white border border-stone-200/90 shadow-sm rounded-2xl hover:shadow-md transition-shadow">
        <div className="w-11 h-11 rounded-2xl bg-[#fff7ed] text-[#ea580c] border border-orange-100 flex items-center justify-center shrink-0">
          <DollarSign className="w-5 h-5" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#78716c] uppercase tracking-wider">Envío / Mínimo</div>
          <div className="text-xs sm:text-sm font-semibold text-[#1c1917] leading-relaxed">
            {store.deliveryEnabled
              ? typeof store.deliveryFee === 'number' ? `Envío: ${formatCurrency(store.deliveryFee)}` : 'Tarifa de envío no publicada'
              : 'Envío a domicilio no habilitado'}
            {typeof store.minOrderAmount === 'number' && <span className="block">Mínimo: {formatCurrency(store.minOrderAmount)}</span>}
          </div>
        </div>
      </Card>

      {/* Horario de Atención */}
      <Card className="p-4 flex items-center gap-3.5 bg-white border border-stone-200/90 shadow-sm rounded-2xl hover:shadow-md transition-shadow">
        <div className="w-11 h-11 rounded-2xl bg-[#fef3c7] text-[#92400e] border border-amber-100 flex items-center justify-center shrink-0">
          <Clock className="w-5 h-5" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#78716c] uppercase tracking-wider">Horario de Atención</div>
          <div className="text-xs sm:text-sm font-semibold text-[#1c1917] leading-relaxed">
            {store.openingHours || 'Horario de atención no publicado'}
          </div>
        </div>
      </Card>

      {/* WhatsApp Bridge */}
      {whatsappUrl ? (
        <a
          href={whatsappUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="block group"
        >
          <Card className="p-4 flex items-center gap-3.5 bg-[#f0fdfa] group-hover:bg-[#ccfbf1]/60 border border-[#005141]/40 group-hover:border-[#005141] transition-all cursor-pointer shadow-sm rounded-2xl">
            <div className="w-11 h-11 rounded-2xl bg-[#005141] text-white flex items-center justify-center shrink-0 shadow-md group-hover:scale-105 transition-transform">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[11px] font-bold text-[#005141] flex items-center gap-1 uppercase tracking-wider">
                WhatsApp Directo <CheckCircle2 className="w-3 h-3 text-[#005141]" />
              </div>
              <div className="text-xs sm:text-sm font-extrabold text-[#1c1917] truncate">
                {formatPhoneNumber(store.phone)}
              </div>
            </div>
          </Card>
        </a>
      ) : (
        <Card className="p-4 flex items-center gap-3.5 bg-white border border-stone-200/90 shadow-sm rounded-2xl">
          <div className="w-11 h-11 rounded-2xl bg-[#faf7f2] text-[#57534e] flex items-center justify-center shrink-0">
            <Phone className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-[11px] font-bold text-[#78716c] uppercase tracking-wider">Contacto</div>
            <div className="text-xs sm:text-sm font-semibold text-[#1c1917] leading-relaxed">Teléfono de contacto no publicado</div>
          </div>
        </Card>
      )}
    </div>
  );
}

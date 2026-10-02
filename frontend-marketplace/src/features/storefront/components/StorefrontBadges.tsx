'use client';

import React from 'react';
import { Bike, DollarSign, Clock, MessageSquare, Phone } from 'lucide-react';
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
        `¡Hola ${store.name}! Vi su tienda en JaldiShop (https://jaldishop.com/tienda/${store.slug}) y deseo hacer un pedido.`
      )
    : null;

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
      {/* Delivery & Recojo */}
      <Card className="p-4 flex items-center gap-3 bg-white border-2 border-[#e7e0d6]">
        <div className="w-10 h-10 rounded-2xl bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1] flex items-center justify-center shrink-0">
          <Bike className="w-4 h-4" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#57534e]">Modalidades</div>
          <div className="text-xs font-extrabold text-[#1c1917] truncate">
            {store.deliveryEnabled && store.pickupEnabled
              ? 'Delivery & Retiro'
              : store.deliveryEnabled
              ? 'Solo Delivery'
              : 'Solo Retiro en local'}
          </div>
        </div>
      </Card>

      {/* Tarifa de Delivery & Pedido Mínimo */}
      <Card className="p-4 flex items-center gap-3 bg-white border-2 border-[#e7e0d6]">
        <div className="w-10 h-10 rounded-2xl bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa] flex items-center justify-center shrink-0">
          <DollarSign className="w-4 h-4" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#57534e]">Envío / Mínimo</div>
          <div className="text-xs font-extrabold text-[#1c1917] truncate font-mono">
            {formatCurrency(store.deliveryFee)} • Mín {formatCurrency(store.minOrderAmount)}
          </div>
        </div>
      </Card>

      {/* Horario de Atención */}
      <Card className="p-4 flex items-center gap-3 bg-white border-2 border-[#e7e0d6]">
        <div className="w-10 h-10 rounded-2xl bg-[#fef3c7] text-[#92400e] border border-[#fde68a] flex items-center justify-center shrink-0">
          <Clock className="w-4 h-4" />
        </div>
        <div className="min-w-0">
          <div className="text-[11px] font-bold text-[#57534e]">Horario de Atención</div>
          <div className="text-xs font-extrabold text-[#1c1917] truncate">
            {store.openingHours || 'Lun - Sáb: 08:00 - 20:00'}
          </div>
        </div>
      </Card>

      {/* WhatsApp Bridge */}
      {whatsappUrl ? (
        <a
          href={whatsappUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="block"
        >
          <Card className="p-4 flex items-center gap-3 bg-[#f0fdfa] hover:bg-[#ccfbf1]/60 border-2 border-[#005141] transition-all cursor-pointer shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-[#005141] text-white flex items-center justify-center shrink-0 shadow-xs">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <div className="text-[11px] font-bold text-[#005141]">WhatsApp Directo</div>
              <div className="text-xs font-extrabold text-[#1c1917] truncate">
                {formatPhoneNumber(store.phone)}
              </div>
            </div>
          </Card>
        </a>
      ) : (
        <Card className="p-4 flex items-center gap-3 bg-white border-2 border-[#e7e0d6]">
          <div className="w-10 h-10 rounded-2xl bg-[#faf7f2] text-[#57534e] flex items-center justify-center shrink-0">
            <Phone className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <div className="text-[11px] font-bold text-[#57534e]">Contacto</div>
            <div className="text-xs font-extrabold text-[#1c1917] truncate">En línea</div>
          </div>
        </Card>
      )}
    </div>
  );
}

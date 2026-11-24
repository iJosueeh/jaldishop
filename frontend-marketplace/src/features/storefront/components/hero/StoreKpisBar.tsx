'use client';

import Link from 'next/link';
import { Truck, Store, MapPin, ArrowUpRight } from 'lucide-react';
import { PublicStore } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

export function StoreKpisBar({ store }: { store: PublicStore }) {
  const fee = store.deliveryFeeAmount ?? store.deliveryFee;
  const deliveryDetail = !store.deliveryEnabled
    ? 'Sin envío a domicilio'
    : typeof fee === 'number'
    ? fee === 0
      ? 'Envío sin costo'
      : 'Envío: ' + formatCurrency(fee, store.deliveryFeeCurrency || 'PEN')
    : 'Tarifa no publicada';

  return (
    <ul
      aria-label="Información de entrega y ubicación"
      className="grid grid-cols-1 sm:grid-cols-3 divide-y sm:divide-y-0 sm:divide-x divide-stone-200/80 rounded-xl bg-[#faf7f2] border border-stone-200/80"
    >
      {/* 1. Entrega */}
      <li className="flex items-center gap-3 px-4 py-3">
        <Truck aria-hidden="true" className="h-4 w-4 shrink-0 text-[#005141]" />
        <div className="min-w-0">
          <p className="text-xs font-bold text-stone-900 truncate">
            {store.deliveryEnabled ? 'Entrega a domicilio' : 'Entrega no habilitada'}
          </p>
          <p className="text-xs text-stone-600 truncate">{deliveryDetail}</p>
        </div>
      </li>

      {/* 2. Recojo */}
      <li className="flex items-center gap-3 px-4 py-3">
        <Store aria-hidden="true" className="h-4 w-4 shrink-0 text-[#ea580c]" />
        <div className="min-w-0">
          <p className="text-xs font-bold text-stone-900 truncate">
            {store.pickupEnabled ? 'Recojo en tienda' : 'Recojo no habilitado'}
          </p>
          <p className="text-xs text-stone-600 truncate">
            {store.pickupEnabled
              ? store.address
                ? 'Disponible en local'
                : 'Dirección no publicada'
              : 'Sin recojo en local'}
          </p>
        </div>
      </li>

      {/* 3. Ubicación */}
      <li>
        <Link
          href="#ubicacion"
          className="group flex items-center justify-between gap-3 px-4 py-3 h-full hover:bg-white/70 transition-colors rounded-b-xl sm:rounded-b-none sm:rounded-r-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <div className="flex items-center gap-3 min-w-0">
            <MapPin aria-hidden="true" className="h-4 w-4 shrink-0 text-amber-700" />
            <div className="min-w-0">
              <p className="text-xs font-bold text-stone-900 truncate">
                {store.address || 'Dirección no publicada'}
              </p>
              <span className="text-xs font-semibold text-[#005141] group-hover:underline">
                Ver ubicación
              </span>
            </div>
          </div>
          <ArrowUpRight
            aria-hidden="true"
            className="h-4 w-4 shrink-0 text-[#ea580c] transition-transform duration-150 group-hover:-translate-y-0.5 group-hover:translate-x-0.5"
          />
        </Link>
      </li>
    </ul>
  );
}

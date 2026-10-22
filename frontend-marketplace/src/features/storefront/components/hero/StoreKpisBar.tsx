'use client';

import Link from 'next/link';
import { Truck, Store, MapPin, ArrowUpRight } from 'lucide-react';
import { PublicStore } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

export function StoreKpisBar({ store }: { store: PublicStore }) {
  const fee = store.deliveryFeeAmount ?? store.deliveryFee;
  const deliveryDetail = !store.deliveryEnabled
    ? 'Esta tienda no ofrece envío a domicilio'
    : typeof fee === 'number'
    ? fee === 0
      ? 'Envío sin costo'
      : 'Envío: ' + formatCurrency(fee, store.deliveryFeeCurrency || 'PEN')
    : 'Tarifa de envío no publicada';

  const card =
    'group relative isolate overflow-hidden rounded-[12px] border border-stone-200/90 bg-stone-50/60 p-4 shadow-2xs transition duration-300 hover:bg-white hover:border-stone-300 hover:shadow-md hover:-translate-y-0.5';
  const glow =
    'pointer-events-none absolute -right-6 -top-8 -z-10 h-24 w-24 rounded-full blur-2xl opacity-15 transition-opacity duration-300 group-hover:opacity-30';
  const icon =
    'mb-3 flex h-9 w-9 items-center justify-center rounded-[8px] border shadow-2xs transition-transform duration-300 group-hover:scale-105';

  return (
    <ul aria-label="Información de entrega y ubicación" className="grid grid-cols-2 gap-3 border-t border-stone-100 pt-4 lg:grid-cols-[1fr_1fr_1.4fr]">
      {/* 1. Entrega a Domicilio */}
      <li className={card}>
        <span aria-hidden="true" className={glow + ' bg-emerald-400'} />
        <div className={icon + ' border-emerald-200 bg-emerald-50 text-[#005141]'}>
          <Truck aria-hidden="true" className="h-4 w-4" />
        </div>
        <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-[#005141]">Entrega</p>
        <p className="font-display text-sm font-bold leading-snug text-stone-900">
          {store.deliveryEnabled ? 'Entrega a domicilio' : 'Entrega no habilitada'}
        </p>
        <p className="mt-1 text-xs leading-relaxed text-stone-600">{deliveryDetail}</p>
      </li>

      {/* 2. Recojo en Tienda */}
      <li className={card}>
        <span aria-hidden="true" className={glow + ' bg-orange-400'} />
        <div className={icon + ' border-orange-200 bg-orange-50 text-[#ea580c]'}>
          <Store aria-hidden="true" className="h-4 w-4" />
        </div>
        <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-[#ea580c]">Recojo</p>
        <p className="font-display text-sm font-bold leading-snug text-stone-900">
          {store.pickupEnabled ? 'Recojo en tienda' : 'Recojo no habilitado'}
        </p>
        <p className="mt-1 text-xs leading-relaxed text-stone-600">
          {store.pickupEnabled
            ? store.address
              ? 'Encuentra el local en el mapa'
              : 'Dirección no publicada'
            : 'Esta tienda no ofrece recojo en local'}
        </p>
      </li>

      {/* 3. Ubicación del Local */}
      <li className="col-span-2 lg:col-span-1">
        <Link
          href="#ubicacion"
          className={
            card +
            ' block h-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141] focus-visible:ring-offset-2'
          }
        >
          <span aria-hidden="true" className={glow + ' bg-amber-400'} />
          <div className="flex items-start justify-between gap-3">
            <div className={icon + ' border-amber-200 bg-amber-50 text-[#feae2c]'}>
              <MapPin aria-hidden="true" className="h-4 w-4" />
            </div>
            <ArrowUpRight
              aria-hidden="true"
              className="h-4 w-4 text-[#ea580c] transition-transform duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5"
            />
          </div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-amber-800">Ubicación</p>
          <p className="font-display text-sm font-bold leading-snug text-stone-900 break-words">
            {store.address || 'Dirección no publicada'}
          </p>
          <span className="mt-2 inline-block text-xs font-semibold text-[#005141] group-hover:underline">
            Ver ubicación
          </span>
        </Link>
      </li>
    </ul>
  );
}

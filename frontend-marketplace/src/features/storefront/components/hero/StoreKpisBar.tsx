'use client';

import Link from 'next/link';
import { Truck, Store, MapPin, ArrowUpRight } from 'lucide-react';
import { PublicStore } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

export function StoreKpisBar({ store }: { store: PublicStore }) {
  const fee = store.deliveryFeeAmount ?? store.deliveryFee;
  const deliveryDetail = !store.deliveryEnabled ? 'Esta tienda no ofrece envío a domicilio'
    : typeof fee === 'number' ? fee === 0 ? 'Envío sin costo' : 'Envío: ' + formatCurrency(fee, store.deliveryFeeCurrency || 'PEN')
    : 'Tarifa de envío no publicada';
  const card = 'group relative isolate overflow-hidden rounded-2xl border p-4 shadow-xs transition duration-300 motion-safe:hover:-translate-y-1 hover:shadow-lg motion-reduce:transition-none';
  const glow = 'pointer-events-none absolute -right-6 -top-8 -z-10 h-24 w-24 rounded-full blur-2xl opacity-45 transition-opacity duration-300 group-hover:opacity-80 motion-reduce:transition-none';
  const icon = 'mb-3 flex h-9 w-9 items-center justify-center rounded-xl border shadow-xs transition-transform duration-300 motion-safe:group-hover:scale-105 motion-reduce:transition-none';

  return <ul aria-label="Información de entrega y ubicación" className="grid grid-cols-2 gap-3 border-t border-stone-100 pt-4 lg:grid-cols-[1fr_1fr_1.4fr]">
    <li className={card + ' border-emerald-200/70 bg-gradient-to-br from-emerald-50/90 via-white to-white hover:border-[#005141]/40 hover:shadow-emerald-900/5'}>
      <span aria-hidden="true" className={glow + ' bg-emerald-300'} />
      <div className={icon + ' border-emerald-200/80 bg-white text-[#005141]'}><Truck aria-hidden="true" className="h-4 w-4" /></div>
      <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-[#005141]/75">Entrega</p>
      <p className="font-display text-sm font-bold leading-snug text-stone-900">{store.deliveryEnabled ? 'Entrega a domicilio' : 'Entrega no habilitada'}</p>
      <p className="mt-1.5 text-xs leading-relaxed text-stone-600">{deliveryDetail}</p>
    </li>
    <li className={card + ' border-orange-200/70 bg-gradient-to-br from-orange-50/90 via-white to-white hover:border-orange-400/50 hover:shadow-orange-900/5'}>
      <span aria-hidden="true" className={glow + ' bg-orange-200'} />
      <div className={icon + ' border-orange-200/80 bg-white text-[#ea580c]'}><Store aria-hidden="true" className="h-4 w-4" /></div>
      <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-orange-800/75">Recojo</p>
      <p className="font-display text-sm font-bold leading-snug text-stone-900">{store.pickupEnabled ? 'Recojo en tienda' : 'Recojo no habilitado'}</p>
      <p className="mt-1.5 text-xs leading-relaxed text-stone-600">{store.pickupEnabled ? store.address ? 'Encuentra el local en el mapa' : 'Dirección no publicada' : 'Esta tienda no ofrece recojo en local'}</p>
    </li>
    <li className="col-span-2 lg:col-span-1">
      <Link href="#ubicacion" className={card + ' block h-full border-amber-200/70 bg-gradient-to-br from-amber-50/90 via-white to-white hover:border-amber-400/60 hover:shadow-amber-900/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141] focus-visible:ring-offset-2'}>
        <span aria-hidden="true" className={glow + ' bg-amber-200'} />
        <div className="flex items-start justify-between gap-3">
          <div className={icon + ' border-amber-200/80 bg-white text-amber-700'}><MapPin aria-hidden="true" className="h-4 w-4" /></div>
          <ArrowUpRight aria-hidden="true" className="h-4 w-4 text-amber-700 transition-transform duration-300 motion-safe:group-hover:-translate-y-0.5 motion-safe:group-hover:translate-x-0.5" />
        </div>
        <p className="mb-1 text-[10px] font-bold uppercase tracking-wider text-amber-800/75">Ubicación</p>
        <p className="font-display text-sm font-bold leading-snug text-stone-900 break-words">{store.address || 'Dirección no publicada'}</p>
        <span className="mt-2 inline-block text-xs font-semibold text-[#005141] group-hover:underline">Ver ubicación</span>
      </Link>
    </li>
  </ul>;
}

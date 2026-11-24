'use client';

import Link from 'next/link';
import Image from 'next/image';
import {
  ArrowUpRight,
  ChevronDown,
  Clock,
  CreditCard,
  MapPin,
  ShieldCheck,
  Star,
  Store as StoreIcon,
  Truck,
} from 'lucide-react';
import { PublicStore, PublicStoreSettings } from '../../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';

interface StoreTrustCardProps {
  store: PublicStore;
  storeSettings?: PublicStoreSettings | null;
}

export function StoreTrustCard({ store, storeSettings }: StoreTrustCardProps) {
  const deliveryFee = store.deliveryFee ?? store.deliveryFeeAmount;
  const prepMinutes = storeSettings?.estimatedPrepTimeMinutes ?? store.preparationTimeMinutes;

  return (
    <div className="overflow-hidden rounded-[12px] border border-stone-200/90 bg-white shadow-xs text-xs text-stone-600">
      {/* Cabecera Midnight Greenhouse */}
      <div className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] p-5 text-white border-b border-[#1e2c31]">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3.5 min-w-0">
            {store.logoUrl ? (
              <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-xl border border-white/15 bg-[#061a1c]">
                <Image src={store.logoUrl} alt={store.name} fill sizes="48px" className="object-cover" />
              </div>
            ) : (
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-[#133b32] bg-[#072720] text-[#36f4a4]">
                <StoreIcon className="h-5 w-5" />
              </div>
            )}
            <div className="min-w-0 space-y-0.5">
              <div className="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-[#36f4a4]">
                <span className="h-1.5 w-1.5 rounded-full bg-[#36f4a4]" aria-hidden="true" />
                <span>Comercio verificado</span>
              </div>
              <Link
                href={`/tienda/${store.slug}`}
                className="font-display text-base font-bold text-white hover:text-[#36f4a4] transition-colors truncate block"
              >
                {store.name}
              </Link>
              <div className="flex flex-wrap items-center gap-2 text-[11px] text-[#99b3ad]">
                {store.address && (
                  <span className="inline-flex items-center gap-1 truncate max-w-[190px]">
                    <MapPin className="h-3 w-3 shrink-0 text-[#36f4a4]" />
                    <span className="truncate">{store.address}</span>
                  </span>
                )}
                {typeof store.rating === 'number' && store.rating > 0 && (
                  <span className="inline-flex items-center gap-1 font-semibold text-white">
                    <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
                    <span>{store.rating.toFixed(1)}</span>
                  </span>
                )}
              </div>
            </div>
          </div>

          <Link
            href={`/tienda/${store.slug}`}
            className="shrink-0 inline-flex items-center gap-1 rounded-full border border-white/25 px-3 py-1.5 text-[11px] font-semibold text-white transition-colors hover:border-[#36f4a4] hover:text-[#36f4a4]"
          >
            <span>Visitar tienda</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>

      {/* Cuerpo estructurado */}
      <div className="p-5 space-y-4">
        {/* Mini-tiles de logística */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          <div className="rounded-xl border border-stone-200/80 bg-stone-50/70 p-3 space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-stone-800">
              <Truck className="h-3.5 w-3.5 text-[#ea580c]" />
              <span>Entrega</span>
            </div>
            <p className="text-[11px] text-stone-600 leading-snug">
              {store.deliveryEnabled !== false && store.pickupEnabled !== false
                ? 'Delivery y recojo en tienda'
                : store.pickupEnabled
                  ? 'Recojo en tienda'
                  : 'Delivery a domicilio'}
            </p>
            {typeof deliveryFee === 'number' && store.deliveryEnabled !== false && (
              <p className="text-[11px] font-semibold text-[#005141]">
                Envío:{' '}
                {deliveryFee === 0
                  ? 'Gratis'
                  : formatCurrency(deliveryFee, store.deliveryFeeCurrency || 'PEN')}
              </p>
            )}
          </div>

          <div className="rounded-xl border border-stone-200/80 bg-stone-50/70 p-3 space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-stone-800">
              <Clock className="h-3.5 w-3.5 text-[#005141]" />
              <span>Preparación</span>
            </div>
            <p className="text-[11px] text-stone-600 leading-snug">
              {prepMinutes ? `Aprox. ${prepMinutes} min` : 'Según turno disponible'}
            </p>
            {storeSettings?.minDeliveryAmount != null && (
              <p className="text-[11px] text-stone-500">
                Mín. delivery: {formatCurrency(storeSettings.minDeliveryAmount, 'PEN')}
              </p>
            )}
          </div>
        </div>

        {/* Garantía y pagos */}
        <div className="space-y-2 rounded-xl border border-emerald-200/70 bg-emerald-50/40 p-3">
          <div className="flex items-start gap-2">
            <ShieldCheck className="h-4 w-4 text-[#005141] shrink-0 mt-0.5" />
            <p className="text-[11px] text-stone-700 leading-relaxed">
              Compra protegida con <strong>Mercado Pago</strong> y reserva de capacidad garantizada.
            </p>
          </div>
          {storeSettings && (
            <div className="flex flex-wrap items-center gap-1.5 pt-1">
              <CreditCard className="h-3.5 w-3.5 text-stone-500 shrink-0" />
              {storeSettings.onlinePaymentsEnabled && (
                <span className="rounded-full bg-white border border-stone-200 px-2 py-0.5 text-[10px] font-semibold text-stone-700">
                  Pago online
                </span>
              )}
              {storeSettings.cashOnDeliveryEnabled && (
                <span className="rounded-full bg-white border border-stone-200 px-2 py-0.5 text-[10px] font-semibold text-stone-700">
                  Efectivo
                </span>
              )}
              {storeSettings.acceptsPosOnDelivery && (
                <span className="rounded-full bg-white border border-stone-200 px-2 py-0.5 text-[10px] font-semibold text-stone-700">
                  POS contra entrega
                </span>
              )}
            </div>
          )}
        </div>

        {/* Políticas desplegables */}
        {(storeSettings?.cancellationPolicy || storeSettings?.refundPolicy) && (
          <details className="group rounded-xl border border-stone-200/80 bg-stone-50/50 px-3.5 py-2.5">
            <summary className="flex cursor-pointer list-none items-center justify-between text-[11px] font-bold text-stone-700 hover:text-[#005141]">
              <span>Políticas de cancelación y reembolso</span>
              <ChevronDown className="h-3.5 w-3.5 text-stone-400 transition-transform group-open:rotate-180" />
            </summary>
            <div className="mt-2.5 space-y-2 border-t border-stone-200/70 pt-2.5 text-[11px] text-stone-600 leading-relaxed">
              {storeSettings.cancellationPolicy && (
                <p>
                  <strong className="text-stone-800">Cancelación:</strong>{' '}
                  {storeSettings.cancellationPolicy}
                </p>
              )}
              {storeSettings.refundPolicy && (
                <p>
                  <strong className="text-stone-800">Reembolsos:</strong> {storeSettings.refundPolicy}
                </p>
              )}
            </div>
          </details>
        )}
      </div>
    </div>
  );
}

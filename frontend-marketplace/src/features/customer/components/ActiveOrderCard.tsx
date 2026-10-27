import React from 'react';
import Link from 'next/link';
import { Store, Clock, ChevronRight, Package, MapPin } from 'lucide-react';
import { CustomerOrderSummary } from '../types/customer.types';
import { OrderStatusBadge } from './OrderStatusBadge';

interface ActiveOrderCardProps {
  order: CustomerOrderSummary;
}

export const ActiveOrderCard: React.FC<ActiveOrderCardProps> = ({ order }) => {
  const isDelivery = order.deliveryMode === 'DELIVERY';

  // Format service date for display
  const formattedDate = order.serviceDate
    ? new Date(order.serviceDate + 'T00:00:00').toLocaleDateString('es-PE', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
      })
    : '';

  const formattedTimeWindow =
    order.serviceStartTime && order.serviceEndTime
      ? `${order.serviceStartTime.slice(0, 5)} - ${order.serviceEndTime.slice(0, 5)}`
      : '';

  return (
    <div className="relative overflow-hidden rounded-2xl bg-white border border-emerald-200/80 shadow-xs hover:border-emerald-300 transition-all p-5 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-stone-100">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141] shrink-0 font-bold">
            {order.storeLogoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={order.storeLogoUrl}
                alt={order.storeName}
                className="w-full h-full object-cover rounded-xl"
              />
            ) : (
              <Store className="w-5 h-5 text-[#005141]" />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-stone-900 hover:text-[#005141] transition-colors">
                <Link href={`/tienda/${order.storeSlug}`}>{order.storeName}</Link>
              </h3>
              <span className="text-xs text-stone-400 font-mono">#{order.orderNumber}</span>
            </div>
            <div className="flex items-center gap-1.5 text-xs text-stone-500 mt-0.5">
              {isDelivery ? (
                <>
                  <MapPin className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Entrega a domicilio</span>
                </>
              ) : (
                <>
                  <Store className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Recojo en local</span>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-center">
          <OrderStatusBadge status={order.status} />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 py-4 text-sm">
        <div className="flex items-start gap-2.5">
          <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
          <div>
            <span className="text-xs text-stone-500 block">Franja horaria programada</span>
            <span className="font-medium text-stone-900 capitalize">
              {formattedDate} {formattedTimeWindow && `(${formattedTimeWindow})`}
            </span>
          </div>
        </div>

        <div className="flex items-start gap-2.5">
          <Package className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
          <div>
            <span className="text-xs text-stone-500 block">Productos</span>
            <span className="font-medium text-stone-900">
              {order.totalItems} {order.totalItems === 1 ? 'artículo' : 'artículos'}
            </span>
          </div>
        </div>

        <div className="sm:text-right">
          <span className="text-xs text-stone-500 block">Total del pedido</span>
          <span className="text-base font-bold text-stone-900">
            S/ {Number(order.totalAmount).toFixed(2)}
          </span>
        </div>
      </div>

      <div className="pt-4 border-t border-stone-100 flex flex-wrap items-center justify-between gap-3">
        <span className="text-xs text-stone-500 font-mono">
          Confirmado el{' '}
          {new Date(order.confirmedAt).toLocaleDateString('es-PE', {
            day: '2-digit',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>

        <Link
          href={`/mi-jaldishop/pedidos/${order.orderNumber}`}
          className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-semibold text-white bg-[#005141] hover:bg-[#003e32] rounded-xl transition-all shadow-xs"
        >
          <span>Ver seguimiento y detalle</span>
          <ChevronRight className="w-4 h-4" />
        </Link>
      </div>
    </div>
  );
};

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
    <article className="rounded-xl bg-white border border-[#005141]/30 hover:border-[#005141]/60 transition-colors p-5 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#e7e0d6]/70">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-[#f0fdfa] border border-[#ccfbf1] flex items-center justify-center text-[#005141] shrink-0 overflow-hidden">
            {order.storeLogoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={order.storeLogoUrl}
                alt={order.storeName}
                className="w-full h-full object-cover"
              />
            ) : (
              <Store className="w-5 h-5 text-[#005141]" />
            )}
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="font-semibold text-[#1c1917] hover:text-[#005141] transition-colors">
                <Link href={`/tienda/${order.storeSlug}`}>{order.storeName}</Link>
              </h3>
              <span className="text-xs text-[#78716c] font-mono tabular-nums">
                #{order.orderNumber}
              </span>
            </div>
            <div className="flex items-center gap-1.5 text-xs text-[#57534e] mt-0.5">
              {isDelivery ? (
                <>
                  <MapPin className="w-3.5 h-3.5 text-[#005141]" />
                  <span>Entrega a domicilio</span>
                </>
              ) : (
                <>
                  <Store className="w-3.5 h-3.5 text-[#005141]" />
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
            <span className="text-xs text-[#57534e] block">Franja horaria programada</span>
            <span className="font-medium text-[#1c1917] capitalize tabular-nums">
              {formattedDate} {formattedTimeWindow && `(${formattedTimeWindow})`}
            </span>
          </div>
        </div>

        <div className="flex items-start gap-2.5">
          <Package className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
          <div>
            <span className="text-xs text-[#57534e] block">Productos</span>
            <span className="font-medium text-[#1c1917] tabular-nums">
              {order.totalItems} {order.totalItems === 1 ? 'artículo' : 'artículos'}
            </span>
          </div>
        </div>

        <div className="sm:text-right">
          <span className="text-xs text-[#57534e] block">Total del pedido</span>
          <span className="text-base font-bold text-[#1c1917] tabular-nums">
            S/ {Number(order.totalAmount).toFixed(2)}
          </span>
        </div>
      </div>

      <div className="pt-4 border-t border-[#e7e0d6]/70 flex flex-wrap items-center justify-between gap-3">
        <span className="text-xs text-[#57534e] font-mono tabular-nums">
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
          className="min-h-[44px] inline-flex items-center gap-1.5 px-4 text-sm font-semibold text-white bg-[#005141] hover:bg-[#003d31] rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <span>Ver seguimiento y detalle</span>
          <ChevronRight className="w-4 h-4" />
        </Link>
      </div>
    </article>
  );
};

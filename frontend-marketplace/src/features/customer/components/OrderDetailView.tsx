'use client';

import React from 'react';
import Link from 'next/link';
import { ArrowLeft, Clock, MapPin, Store, Receipt, AlertCircle, RefreshCw, Star, MessageSquareQuote } from 'lucide-react';
import { useCustomerOrderDetail } from '../hooks/useCustomerOrderDetail';
import { OrderStatusBadge } from './OrderStatusBadge';
import { OrderTimelineStepper } from './OrderTimelineStepper';
import { WhatsAppContactButton } from './WhatsAppContactButton';

interface OrderDetailViewProps {
  orderNumber: string;
}

export const OrderDetailView: React.FC<OrderDetailViewProps> = ({ orderNumber }) => {
  const { order, isLoading, error, reload } = useCustomerOrderDetail(orderNumber);

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-6 w-36 bg-stone-200 rounded-md" />
        <div className="h-28 bg-white rounded-2xl border border-stone-200/90 p-6" />
        <div className="h-32 bg-white rounded-2xl border border-stone-200/90 p-6" />
        <div className="h-48 bg-white rounded-2xl border border-stone-200/90 p-6" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="rounded-2xl border border-rose-200 bg-rose-50/60 p-8 text-center max-w-lg mx-auto">
        <AlertCircle className="w-10 h-10 text-rose-600 mx-auto mb-3" />
        <h3 className="text-lg font-semibold text-rose-950 mb-1">
          No pudimos cargar este pedido
        </h3>
        <p className="text-sm text-rose-800 mb-6">
          {error || 'El pedido solicitado no existe o no tienes permisos para visualizarlo.'}
        </p>
        <div className="flex items-center justify-center gap-3">
          <Link
            href="/mi-jaldishop/pedidos"
            className="px-4 py-2 text-sm font-medium text-stone-700 bg-white border border-stone-300 rounded-xl hover:bg-stone-50 transition-colors"
          >
            Volver a mis pedidos
          </Link>
          <button
            type="button"
            onClick={reload}
            className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white bg-[#005141] rounded-xl hover:bg-[#003e32] transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reintentar</span>
          </button>
        </div>
      </div>
    );
  }

  const isDelivery = order.deliveryMode === 'DELIVERY';
  const formattedDate = order.serviceDate
    ? new Date(order.serviceDate + 'T00:00:00').toLocaleDateString('es-PE', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
      })
    : '';

  const formattedTimeWindow =
    order.serviceStartTime && order.serviceEndTime
      ? `${order.serviceStartTime.slice(0, 5)} a ${order.serviceEndTime.slice(0, 5)}`
      : '';

  return (
    <div className="space-y-6">
      {/* Back button */}
      <div>
        <Link
          href="/mi-jaldishop/pedidos"
          className="inline-flex items-center gap-2 text-sm font-medium text-stone-600 hover:text-[#005141] transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Volver a Mis Pedidos</span>
        </Link>
      </div>

      {/* Header Card */}
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-xs p-5 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-stone-100">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-xl sm:text-2xl font-bold text-stone-900 tracking-tight">
                Pedido #{order.orderNumber}
              </h1>
              <OrderStatusBadge status={order.status} />
            </div>
            <p className="text-xs text-stone-500 font-mono mt-1">
              Realizado el{' '}
              {new Date(order.confirmedAt).toLocaleDateString('es-PE', {
                day: '2-digit',
                month: 'long',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
          </div>

          <WhatsAppContactButton
            phone={order.store.whatsappNumber || order.store.contactPhone}
            orderNumber={order.orderNumber}
            storeName={order.store.name}
            variant="primary"
          />
        </div>

        {/* Store & Fulfillment banner */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-5">
          {/* Store Info */}
          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-stone-50/70 border border-stone-100">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141] shrink-0 font-bold">
              {order.store.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={order.store.logoUrl}
                  alt={order.store.name}
                  className="w-full h-full object-cover rounded-xl"
                />
              ) : (
                <Store className="w-5 h-5 text-[#005141]" />
              )}
            </div>
            <div className="space-y-0.5">
              <span className="text-xs font-semibold uppercase text-stone-400 tracking-wider">
                Comercio
              </span>
              <h3 className="font-semibold text-stone-900">
                <Link
                  href={`/tienda/${order.store.slug}`}
                  className="hover:text-[#005141] transition-colors"
                >
                  {order.store.name}
                </Link>
              </h3>
              {order.store.address && (
                <p className="text-xs text-stone-500">{order.store.address}</p>
              )}
            </div>
          </div>

          {/* Fulfillment Slot */}
          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-stone-50/70 border border-stone-100">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141] shrink-0">
              <Clock className="w-5 h-5 text-[#005141]" />
            </div>
            <div className="space-y-0.5">
              <span className="text-xs font-semibold uppercase text-stone-400 tracking-wider">
                {isDelivery ? 'Entrega programada' : 'Recojo en local'}
              </span>
              <p className="text-sm font-semibold text-stone-900 capitalize">
                {formattedDate}
              </p>
              <p className="text-xs text-stone-600">
                Franja: {formattedTimeWindow || 'Horario regular'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Live Timeline Tracker */}
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-xs p-5 sm:p-6 space-y-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-stone-700">
          Estado del pedido
        </h2>
        <OrderTimelineStepper
          status={order.status}
          deliveryMode={order.deliveryMode}
          statusHistory={order.statusHistory}
        />
      </div>

      {/* Delivery / Pickup address info */}
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-xs p-5 sm:p-6 space-y-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-stone-700 flex items-center gap-2">
          <MapPin className="w-4 h-4 text-[#005141]" />
          <span>{isDelivery ? 'Destino de entrega' : 'Dirección para recojo'}</span>
        </h2>
        {isDelivery ? (
          <div className="space-y-1 text-sm text-stone-700">
            <p className="font-semibold text-stone-900">
              {order.deliveryAddress || 'Dirección registrada'}
            </p>
            {order.deliveryReference && (
              <p className="text-xs text-stone-500">
                Referencia: {order.deliveryReference}
              </p>
            )}
            <p className="text-xs text-stone-500 pt-1">
              Receptor: {order.customerName} &bull; Teléfono: {order.customerPhone}
            </p>
          </div>
        ) : (
          <div className="space-y-1 text-sm text-stone-700">
            <p className="font-semibold text-stone-900">
              {order.store.address || 'Local principal del comercio'}
            </p>
            {order.store.addressReference && (
              <p className="text-xs text-stone-500">
                Referencia: {order.store.addressReference}
              </p>
            )}
            <p className="text-xs text-stone-500 pt-1">
              Comprador: {order.customerName}
            </p>
          </div>
        )}
      </div>

      {/* Items Table & Receipt */}
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-xs p-5 sm:p-6 space-y-5">
        <h2 className="text-sm font-bold uppercase tracking-wider text-stone-700 flex items-center gap-2">
          <Receipt className="w-4 h-4 text-[#005141]" />
          <span>Resumen de compra ({order.items.length} artículos)</span>
        </h2>

        <div className="divide-y divide-stone-100">
          {order.items.map((item) => (
            <div key={item.id} className="py-3.5 flex items-center justify-between gap-4 text-sm">
              <div className="space-y-0.5">
                <p className="font-medium text-stone-900">{item.productName}</p>
                {item.variantName && (
                  <p className="text-xs text-stone-500">Opción: {item.variantName}</p>
                )}
                <p className="text-xs text-stone-400">
                  {item.quantity} x S/ {Number(item.unitPriceAmount).toFixed(2)}
                </p>
              </div>
              <span className="font-semibold text-stone-900 text-right">
                S/ {Number(item.subtotalAmount).toFixed(2)}
              </span>
            </div>
          ))}
        </div>

        {/* Pricing calculations */}
        <div className="pt-4 border-t border-stone-100 space-y-2 text-sm">
          <div className="flex justify-between text-stone-600">
            <span>Subtotal de productos</span>
            <span>S/ {Number(order.productsSubtotalAmount).toFixed(2)}</span>
          </div>

          {Number(order.discountAmount) > 0 && (
            <div className="flex justify-between text-emerald-700">
              <span>Descuento {order.discountCode && `(${order.discountCode})`}</span>
              <span>- S/ {Number(order.discountAmount).toFixed(2)}</span>
            </div>
          )}

          <div className="flex justify-between text-stone-600">
            <span>Costo de envío</span>
            <span>
              {Number(order.deliveryFeeAmount) === 0
                ? 'Gratis'
                : `S/ ${Number(order.deliveryFeeAmount).toFixed(2)}`}
            </span>
          </div>

          <div className="flex justify-between items-baseline pt-3 border-t border-stone-200 text-base font-bold text-stone-900">
            <span>Total pagado</span>
            <span className="text-xl text-[#005141]">
              S/ {Number(order.totalAmount).toFixed(2)}
            </span>
          </div>
        </div>
      </div>

      {/* Verified Buyer Review Banner */}
      {order.status === 'COMPLETED' && (
        <div className="bg-emerald-50/70 border border-emerald-200/90 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white border border-emerald-200 flex items-center justify-center text-emerald-800 shrink-0">
              <Star className="w-5 h-5 fill-amber-400 text-amber-400" />
            </div>
            <div>
              <p className="text-sm font-bold text-stone-900">
                ¿Qué te parecieron los productos de esta compra?
              </p>
              <p className="text-xs text-stone-600">
                Como comprador verificado, tu reseña ayuda a la comunidad y a este comercio local.
              </p>
            </div>
          </div>
          <Link
            href="/mi-jaldishop/resenas"
            className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#005141] text-white text-xs font-semibold hover:bg-[#004033] transition-colors shrink-0 shadow-xs"
          >
            <MessageSquareQuote className="w-4 h-4" />
            <span>Gestionar mis reseñas</span>
          </Link>
        </div>
      )}
    </div>
  );
};

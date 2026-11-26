'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Clock,
  MapPin,
  Store,
  Receipt,
  AlertCircle,
  RefreshCw,
  Star,
  MessageSquareQuote,
  XCircle,
} from 'lucide-react';
import { useCustomerOrderDetail } from '../hooks/useCustomerOrderDetail';
import { OrderStatusBadge } from './OrderStatusBadge';
import { OrderTimelineStepper } from './OrderTimelineStepper';
import { WhatsAppContactButton } from './WhatsAppContactButton';
import { CancelOrderModal } from './CancelOrderModal';
import { OrderReviewAction } from './OrderReviewAction';
import { OrderReceiptCard } from './OrderReceiptCard';

interface OrderDetailViewProps {
  orderNumber: string;
}

export const OrderDetailView: React.FC<OrderDetailViewProps> = ({ orderNumber }) => {
  const {
    order,
    isLoading,
    error,
    reload,
    cancelOrder,
    isCancelling,
    cancelError,
    resetCancelError,
  } = useCustomerOrderDetail(orderNumber);

  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse" role="status" aria-busy="true" aria-label="Cargando detalle del pedido">
        <div className="h-6 w-36 bg-[#f0eae0] rounded-md" />
        <div className="h-32 bg-white rounded-xl border border-[#e7e0d6]" />
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)] gap-6">
          <div className="h-64 bg-white rounded-xl border border-[#e7e0d6]" />
          <div className="h-64 bg-white rounded-xl border border-[#e7e0d6]" />
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div
        role="alert"
        className="rounded-xl border border-rose-200 bg-rose-50/70 p-8 text-center max-w-lg mx-auto"
      >
        <AlertCircle className="w-10 h-10 text-rose-600 mx-auto mb-3" />
        <h2 className="text-lg font-semibold text-rose-950 mb-1">
          No pudimos cargar este pedido
        </h2>
        <p className="text-sm text-rose-800 mb-6">
          {error || 'El pedido solicitado no existe o no tienes permisos para visualizarlo.'}
        </p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/mi-jaldishop/pedidos"
            className="min-h-[44px] inline-flex items-center px-4 text-sm font-medium text-[#1c1917] bg-white border border-[#e7e0d6] rounded-lg hover:bg-[#f7f3ec] transition-colors"
          >
            Volver a mis pedidos
          </Link>
          <button
            type="button"
            onClick={reload}
            className="min-h-[44px] inline-flex items-center gap-1.5 px-4 text-sm font-medium text-white bg-[#005141] rounded-lg hover:bg-[#003d31] transition-colors cursor-pointer"
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
      {/* Back link */}
      <div>
        <Link
          href="/mi-jaldishop/pedidos"
          className="min-h-[44px] inline-flex items-center gap-2 text-sm font-semibold text-[#57534e] hover:text-[#005141] transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141] rounded"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Volver a Mis Pedidos</span>
        </Link>
      </div>

      {/* Header Card (no nested cards inside) */}
      <section className="bg-white rounded-xl border border-[#e7e0d6] p-5 sm:p-6 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-[#e7e0d6]/70">
          <div className="space-y-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="font-display text-xl sm:text-2xl font-bold text-[#1c1917] tracking-tight tabular-nums">
                Pedido #{order.orderNumber}
              </h1>
              <OrderStatusBadge status={order.status} size="sm" />
            </div>
            <p className="text-xs text-[#57534e] font-mono tabular-nums">
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

          <div className="flex flex-col sm:flex-row sm:items-center gap-2.5 w-full sm:w-auto">
            {order.status === 'CONFIRMED' && (
              <button
                type="button"
                onClick={() => {
                  resetCancelError();
                  setIsCancelModalOpen(true);
                }}
                className="min-h-[44px] w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 text-xs font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-600"
              >
                <XCircle className="w-4 h-4 text-rose-600" />
                <span>Cancelar pedido</span>
              </button>
            )}

            <WhatsAppContactButton
              phone={order.store.whatsappNumber || order.store.contactPhone}
              orderNumber={order.orderNumber}
              storeName={order.store.name}
              variant="primary"
              className="w-full sm:w-auto justify-center"
            />
          </div>
        </div>

        {/* Store & Fulfillment Split (clean divider, no nested boxes) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5 md:gap-0 md:divide-x md:divide-[#e7e0d6]/70">
          <div className="flex items-start gap-3.5 md:pr-6">
            <div className="w-10 h-10 rounded-lg bg-[#f0fdfa] border border-[#ccfbf1] flex items-center justify-center text-[#005141] shrink-0 overflow-hidden">
              {order.store.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={order.store.logoUrl}
                  alt={order.store.name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <Store className="w-5 h-5 text-[#005141]" />
              )}
            </div>
            <div className="space-y-0.5 min-w-0">
              <span className="text-[11px] font-bold uppercase text-[#78716c] tracking-wider">
                Comercio
              </span>
              <h2 className="font-semibold text-[#1c1917]">
                <Link
                  href={`/tienda/${order.store.slug}`}
                  className="hover:text-[#005141] transition-colors"
                >
                  {order.store.name}
                </Link>
              </h2>
              {order.store.address && (
                <p className="text-xs text-[#57534e]">{order.store.address}</p>
              )}
            </div>
          </div>

          <div className="flex items-start gap-3.5 pt-4 md:pt-0 border-t md:border-t-0 border-[#e7e0d6]/70 md:pl-6">
            <div className="w-10 h-10 rounded-lg bg-[#f0fdfa] border border-[#ccfbf1] flex items-center justify-center text-[#005141] shrink-0">
              <Clock className="w-5 h-5 text-[#005141]" />
            </div>
            <div className="space-y-0.5">
              <span className="text-[11px] font-bold uppercase text-[#78716c] tracking-wider">
                {isDelivery ? 'Entrega programada' : 'Recojo en local'}
              </span>
              <p className="text-sm font-semibold text-[#1c1917] capitalize tabular-nums">
                {formattedDate}
              </p>
              <p className="text-xs text-[#57534e] tabular-nums">
                Franja: {formattedTimeWindow || 'Horario regular'}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Full-Width Live Timeline Tracker (prevents narrow-column label wrapping) */}
      <section className="bg-white rounded-xl border border-[#e7e0d6] p-5 sm:p-6 space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e]">
          Estado del pedido
        </h2>
        <OrderTimelineStepper
          status={order.status}
          deliveryMode={order.deliveryMode}
          statusHistory={order.statusHistory}
        />
      </section>

      {/* Main 2-Column Desktop Body */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1.65fr)_minmax(300px,1fr)] gap-6 items-start">
        {/* Left: Items Receipt */}
        <div className="space-y-6 min-w-0">
          <section className="bg-white rounded-xl border border-[#e7e0d6] p-5 sm:p-6 space-y-5">
            <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e] flex items-center gap-2">
              <Receipt className="w-4 h-4 text-[#005141]" />
              <span className="tabular-nums">Resumen de compra ({order.items.length} artículos)</span>
            </h2>

            <div className="divide-y divide-[#e7e0d6]/70">
              {order.items.map((item) => (
                <div key={item.id} className="py-3.5 flex items-start justify-between gap-4 text-sm">
                  <div className="space-y-1 min-w-0">
                    <p className="font-medium text-[#1c1917]">{item.productName}</p>
                    {item.variantName && (
                      <p className="text-xs text-[#57534e]">Opción: {item.variantName}</p>
                    )}
                    <p className="text-xs text-[#78716c] tabular-nums">
                      {item.quantity} x S/ {Number(item.unitPriceAmount).toFixed(2)}
                    </p>
                    {order.status === 'COMPLETED' && item.productId && (
                      <OrderReviewAction productId={item.productId} productName={item.productName} />
                    )}
                  </div>
                  <span className="font-semibold text-[#1c1917] text-right tabular-nums shrink-0">
                    S/ {Number(item.subtotalAmount).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>

            <div className="pt-4 border-t border-[#e7e0d6]/70 space-y-2 text-sm">
              <div className="flex justify-between text-[#57534e]">
                <span>Subtotal de productos</span>
                <span className="tabular-nums">S/ {Number(order.productsSubtotalAmount).toFixed(2)}</span>
              </div>

              {Number(order.discountAmount) > 0 && (
                <div className="flex justify-between text-[#005141]">
                  <span>Descuento {order.discountCode && `(${order.discountCode})`}</span>
                  <span className="tabular-nums">- S/ {Number(order.discountAmount).toFixed(2)}</span>
                </div>
              )}

              <div className="flex justify-between text-[#57534e]">
                <span>Costo de envío</span>
                <span className="tabular-nums">
                  {Number(order.deliveryFeeAmount) === 0
                    ? 'Gratis'
                    : `S/ ${Number(order.deliveryFeeAmount).toFixed(2)}`}
                </span>
              </div>

              {Number(order.serviceFeeAmount ?? 0) > 0 && (
                <div className="flex justify-between text-[#57534e]">
                  <span>Tarifa de servicio</span>
                  <span className="tabular-nums">
                    S/ {Number(order.serviceFeeAmount).toFixed(2)}
                  </span>
                </div>
              )}

              {Number(order.includedTaxAmount ?? 0) > 0 && (
                <div className="flex justify-between text-xs text-[#78716c] pt-1">
                  <span>IGV incluido (18%)</span>
                  <span className="tabular-nums">
                    S/ {Number(order.includedTaxAmount).toFixed(2)}
                  </span>
                </div>
              )}

              <div className="flex justify-between items-baseline pt-3 border-t border-[#e7e0d6] text-base font-bold text-[#1c1917]">
                <span>Total pagado</span>
                <span className="text-xl text-[#005141] tabular-nums">
                  S/ {Number(order.totalAmount).toFixed(2)}
                </span>
              </div>
            </div>
          </section>
        </div>

        {/* Right: Delivery Address + Verified Review Callout */}
        <div className="space-y-6 min-w-0">
          <section className="bg-white rounded-xl border border-[#e7e0d6] p-5 sm:p-6 space-y-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e] flex items-center gap-2">
              <MapPin className="w-4 h-4 text-[#005141]" />
              <span>{isDelivery ? 'Destino de entrega' : 'Dirección para recojo'}</span>
            </h2>
            {isDelivery ? (
              <div className="space-y-1.5 text-sm text-[#57534e]">
                <p className="font-semibold text-[#1c1917]">
                  {order.deliveryAddress || 'Dirección registrada'}
                </p>
                {order.deliveryReference && (
                  <p className="text-xs text-[#57534e]">
                    Referencia: {order.deliveryReference}
                  </p>
                )}
                <p className="text-xs text-[#57534e] pt-1 tabular-nums">
                  Receptor: {order.customerName} &bull; Teléfono: {order.customerPhone}
                </p>
              </div>
            ) : (
              <div className="space-y-1.5 text-sm text-[#57534e]">
                <p className="font-semibold text-[#1c1917]">
                  {order.store.address || 'Local principal del comercio'}
                </p>
                {order.store.addressReference && (
                  <p className="text-xs text-[#57534e]">
                    Referencia: {order.store.addressReference}
                  </p>
                )}
                <p className="text-xs text-[#57534e] pt-1">
                  Comprador: {order.customerName}
                </p>
              </div>
            )}
          </section>

          <OrderReceiptCard orderId={order.id} />

          {order.status === 'COMPLETED' && (
            <section className="bg-[#f0fdfa] border border-[#ccfbf1] rounded-xl p-5 space-y-4">
              <div className="flex items-start gap-3">
                <Star className="w-5 h-5 fill-amber-400 text-amber-500 shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm font-bold text-[#1c1917]">
                    ¿Qué te parecieron los productos de esta compra?
                  </p>
                  <p className="text-xs text-[#57534e] mt-1 leading-relaxed">
                    Como comprador verificado, tu reseña ayuda a la comunidad y a este comercio local.
                  </p>
                </div>
              </div>
              <Link
                href="/mi-jaldishop/resenas"
                className="min-h-[44px] w-full inline-flex items-center justify-center gap-1.5 px-4 rounded-lg bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors"
              >
                <MessageSquareQuote className="w-4 h-4" />
                <span>Gestionar mis reseñas</span>
              </Link>
            </section>
          )}
        </div>
      </div>

      {/* Cancellation Modal */}
      {order && (
        <CancelOrderModal
          isOpen={isCancelModalOpen}
          onClose={() => setIsCancelModalOpen(false)}
          orderNumber={order.orderNumber}
          storeName={order.store.name}
          totalAmount={order.totalAmount}
          currency={order.currency}
          onConfirm={async (reason) => {
            await cancelOrder(reason);
          }}
          isCancelling={isCancelling}
          error={cancelError}
          onClearError={resetCancelError}
        />
      )}
    </div>
  );
};

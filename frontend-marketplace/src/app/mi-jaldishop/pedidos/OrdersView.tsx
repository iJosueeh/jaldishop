'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Package, Store, Clock, ChevronRight, RefreshCw, AlertCircle, MapPin } from 'lucide-react';
import { EmptyState } from '@/shared/components/ui/EmptyState';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { useCustomerOrders } from '@/features/customer/hooks/useCustomerOrders';
import { OrderStatusBadge } from '@/features/customer/components/OrderStatusBadge';

export function OrdersView() {
  const [activeTab, setActiveTab] = useState<'ACTIVE' | 'HISTORY'>('ACTIVE');
  const { activeOrders, pastOrders, isLoading, error, reload } = useCustomerOrders();

  const currentList = activeTab === 'ACTIVE' ? activeOrders : pastOrders;

  return (
    <div className="space-y-6 w-full">
      {/* Encabezado */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-200/80">
        <div>
          <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
            Mis Pedidos
          </h2>
          <p className="text-xs sm:text-sm text-stone-600 mt-0.5">
            Consulta el estado de preparación y las franjas horarias acordadas con cada comercio.
          </p>
        </div>
        <Badge variant="jade" size="sm" className="font-bold self-start sm:self-auto">
          Capacidad Garantizada
        </Badge>
      </div>

      {/* Tabs Filtro Activos / Historial con Full Width Flow */}
      <div className="flex items-center gap-6 border-b border-stone-200">
        <button
          type="button"
          onClick={() => setActiveTab('ACTIVE')}
          className={`pb-3.5 px-1 text-sm font-bold transition-all relative cursor-pointer flex items-center gap-2 ${
            activeTab === 'ACTIVE'
              ? 'text-[#005141]'
              : 'text-stone-500 hover:text-stone-900'
          }`}
        >
          <span>Pedidos Activos</span>
          {!isLoading && (
            <span
              className={`text-xs px-2 py-0.5 rounded-full ${
                activeTab === 'ACTIVE'
                  ? 'bg-emerald-100 text-[#005141]'
                  : 'bg-stone-100 text-stone-500'
              }`}
            >
              {activeOrders.length}
            </span>
          )}
          {activeTab === 'ACTIVE' && (
            <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" />
          )}
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('HISTORY')}
          className={`pb-3.5 px-1 text-sm font-bold transition-all relative cursor-pointer flex items-center gap-2 ${
            activeTab === 'HISTORY'
              ? 'text-[#005141]'
              : 'text-stone-500 hover:text-stone-900'
          }`}
        >
          <span>Historial de Compras</span>
          {!isLoading && (
            <span
              className={`text-xs px-2 py-0.5 rounded-full ${
                activeTab === 'HISTORY'
                  ? 'bg-emerald-100 text-[#005141]'
                  : 'bg-stone-100 text-stone-500'
              }`}
            >
              {pastOrders.length}
            </span>
          )}
          {activeTab === 'HISTORY' && (
            <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" />
          )}
        </button>
      </div>

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="space-y-4 animate-pulse">
          <div className="h-28 bg-white rounded-2xl border border-stone-200/90 p-5" />
          <div className="h-28 bg-white rounded-2xl border border-stone-200/90 p-5" />
        </div>
      )}

      {/* Error State */}
      {!isLoading && error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50/60 p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-rose-600 shrink-0" />
            <p className="text-sm text-rose-800">{error}</p>
          </div>
          <button
            type="button"
            onClick={reload}
            className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-[#005141] rounded-xl hover:bg-[#003e32] transition-colors shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reintentar</span>
          </button>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && currentList.length === 0 && (
        <div className="p-8 sm:p-14 rounded-2xl bg-white border border-stone-200/90 shadow-xs">
          <EmptyState
            icon={<Package className="w-8 h-8 text-[#005141]" />}
            title={
              activeTab === 'ACTIVE'
                ? 'No tienes pedidos activos en este momento'
                : 'No cuentas con pedidos anteriores'
            }
            description="En JaldiShop cada pedido tiene una fecha y franja horaria programada con el negocio. Explora el catálogo de los comercios locales y haz tu primer pedido."
            action={
              <Link href="/#tiendas-destacadas">
                <Button
                  variant="primary"
                  size="md"
                  className="rounded-full px-6 font-bold shadow-md shadow-[#005141]/10"
                  leftIcon={<Store className="w-4 h-4 text-white" />}
                >
                  Explorar comercios locales
                </Button>
              </Link>
            }
          />
        </div>
      )}

      {/* Orders List */}
      {!isLoading && !error && currentList.length > 0 && (
        <div className="space-y-4">
          {currentList.map((order) => {
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
              <div
                key={order.id}
                className="bg-white rounded-2xl border border-stone-200/90 hover:border-emerald-300 transition-all p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <span className="font-semibold text-stone-900 text-base">
                      {order.storeName}
                    </span>
                    <span className="text-xs font-mono text-stone-400">
                      #{order.orderNumber}
                    </span>
                    <OrderStatusBadge status={order.status} size="sm" />
                  </div>

                  <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-stone-500">
                    <span className="flex items-center gap-1.5 font-medium text-stone-700">
                      <Clock className="w-3.5 h-3.5 text-[#005141]" />
                      <span className="capitalize">{formattedDate}</span> ({formattedTimeWindow})
                    </span>

                    <span className="flex items-center gap-1.5">
                      {isDelivery ? (
                        <>
                          <MapPin className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Entrega a domicilio</span>
                        </>
                      ) : (
                        <>
                          <Store className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Recojo en tienda</span>
                        </>
                      )}
                    </span>

                    <span>
                      {order.totalItems} {order.totalItems === 1 ? 'artículo' : 'artículos'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between md:justify-end gap-5 pt-3 md:pt-0 border-t md:border-t-0 border-stone-100">
                  <div className="md:text-right">
                    <span className="text-[11px] text-stone-400 block uppercase tracking-wider">
                      Total
                    </span>
                    <span className="text-base font-bold text-stone-900">
                      S/ {Number(order.totalAmount).toFixed(2)}
                    </span>
                  </div>

                  <Link
                    href={`/mi-jaldishop/pedidos/${order.orderNumber}`}
                    className="inline-flex items-center gap-1 px-4 py-2 text-xs sm:text-sm font-semibold text-[#005141] bg-emerald-50 hover:bg-emerald-100 rounded-xl transition-colors"
                  >
                    <span>Ver detalle</span>
                    <ChevronRight className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Nota Informativa sobre la Operación MYPE */}
      <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-200/80 flex items-start gap-3.5 text-xs text-stone-700 shadow-2xs">
        <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold text-stone-900 text-sm">
            ¿Cómo se coordinan las entregas en JaldiShop?
          </p>
          <p className="leading-relaxed text-stone-600">
            Los comercios reciben los pedidos confirmados con su franja de preparación. Puedes consultar
            directamente con la tienda a través de los números oficiales de WhatsApp publicados en cada perfil de comercio.
          </p>
        </div>
      </div>
    </div>
  );
}

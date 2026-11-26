'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Package, Store, Clock, ChevronRight, RefreshCw, AlertCircle, MapPin, Search } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { useCustomerOrdersPaged } from '@/features/customer/hooks/useCustomerOrdersPaged';
import { useDebouncedValue } from '@/features/customer/hooks/useDebouncedValue';
import { OrderStatusBadge } from '@/features/customer/components/OrderStatusBadge';
import { AccountPageHeader } from '@/features/customer/components/AccountPageHeader';
import { AccountEmptyState } from '@/features/customer/components/AccountEmptyState';
import { AccountPagination } from '@/features/customer/components/AccountPagination';
import { cn } from '@/shared/utils/cn';

type DeliveryFilter = 'ALL' | 'DELIVERY' | 'PICKUP';

const PAGE_SIZE = 6;

export function OrdersView() {
  const [activeTab, setActiveTab] = useState<'ACTIVE' | 'HISTORY'>('ACTIVE');
  const [search, setSearch] = useState('');
  const [deliveryFilter, setDeliveryFilter] = useState<DeliveryFilter>('ALL');
  const [page, setPage] = useState(1);

  const debouncedSearch = useDebouncedValue(search, 300);

  const {
    items,
    totalElements,
    isLoading,
    isFetching,
    error,
    reload,
  } = useCustomerOrdersPaged({
    page: page - 1,
    size: PAGE_SIZE,
    tab: activeTab,
    deliveryMode: deliveryFilter,
    q: debouncedSearch,
  });

  const hasActiveFilters = deliveryFilter !== 'ALL' || debouncedSearch.trim().length > 0;

  const handleTabChange = (tab: 'ACTIVE' | 'HISTORY') => {
    setActiveTab(tab);
    setPage(1);
  };

  const resetFilters = () => {
    setSearch('');
    setDeliveryFilter('ALL');
    setPage(1);
  };

  return (
    <div className="space-y-5 w-full">
      {/* Encabezado */}
      <AccountPageHeader
        title="Mis Pedidos"
        description="Consulta el estado de preparación y las franjas horarias acordadas con cada comercio."
        actions={
          <Badge variant="jade" size="sm" className="font-bold">
            Capacidad Garantizada
          </Badge>
        }
      />

      {/* Tabs Filtro Activos / Historial */}
      <div
        role="tablist"
        aria-label="Filtrar pedidos"
        className="flex items-center gap-6 border-b border-[#e7e0d6]"
      >
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'ACTIVE'}
          onClick={() => handleTabChange('ACTIVE')}
          className={cn(
            'min-h-[44px] pb-3 px-1 text-sm font-bold transition-colors relative cursor-pointer inline-flex items-center gap-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
            activeTab === 'ACTIVE'
              ? 'text-[#005141]'
              : 'text-[#57534e] hover:text-[#1c1917]'
          )}
        >
          <span>Pedidos Activos</span>
          {activeTab === 'ACTIVE' && !isLoading && (
            <span className="text-xs px-2 py-0.5 rounded-full tabular-nums bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]">
              {totalElements}
            </span>
          )}
          {activeTab === 'ACTIVE' && (
            <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" aria-hidden="true" />
          )}
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'HISTORY'}
          onClick={() => handleTabChange('HISTORY')}
          className={cn(
            'min-h-[44px] pb-3 px-1 text-sm font-bold transition-colors relative cursor-pointer inline-flex items-center gap-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
            activeTab === 'HISTORY'
              ? 'text-[#005141]'
              : 'text-[#57534e] hover:text-[#1c1917]'
          )}
        >
          <span>Historial de Compras</span>
          {activeTab === 'HISTORY' && !isLoading && (
            <span className="text-xs px-2 py-0.5 rounded-full tabular-nums bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]">
              {totalElements}
            </span>
          )}
          {activeTab === 'HISTORY' && (
            <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" aria-hidden="true" />
          )}
        </button>
      </div>

      {/* Búsqueda y Filtro por Modalidad */}
      {!isLoading && !error && (totalElements > 0 || hasActiveFilters) && (
        <div className="rounded-xl border border-[#e7e0d6] bg-white p-3.5 sm:p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search
              className="w-4 h-4 text-[#78716c] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              aria-hidden="true"
            />
            <input
              type="search"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              aria-label="Buscar pedidos"
              placeholder="Buscar por comercio o # de pedido..."
              className="w-full min-h-[44px] pl-9 pr-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2]/50 focus:bg-white text-sm text-[#1c1917] placeholder-[#78716c] focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div
            role="group"
            aria-label="Filtrar por modalidad de entrega"
            className="flex flex-wrap items-center gap-1.5"
          >
            {(
              [
                { key: 'ALL', label: 'Todas' },
                { key: 'DELIVERY', label: 'Domicilio' },
                { key: 'PICKUP', label: 'Recojo en tienda' },
              ] as const
            ).map((opt) => {
              const isActive = deliveryFilter === opt.key;
              return (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => {
                    setDeliveryFilter(opt.key);
                    setPage(1);
                  }}
                  className={cn(
                    'min-h-[44px] px-3.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isActive
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="space-y-3 animate-pulse" role="status" aria-busy="true" aria-label="Cargando pedidos">
          <div className="h-24 bg-white rounded-xl border border-[#e7e0d6]" />
          <div className="h-24 bg-white rounded-xl border border-[#e7e0d6]" />
        </div>
      )}

      {/* Error State */}
      {!isLoading && error && (
        <div
          role="alert"
          className="rounded-xl border border-rose-200 bg-rose-50/70 p-5 flex flex-col sm:flex-row items-center justify-between gap-4"
        >
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <p className="text-sm text-rose-900">{error}</p>
          </div>
          <button
            type="button"
            onClick={() => reload()}
            className="min-h-[44px] inline-flex items-center gap-1.5 px-4 text-xs font-semibold text-white bg-[#005141] rounded-lg hover:bg-[#003d31] transition-colors shrink-0 cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reintentar</span>
          </button>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && items.length === 0 && !hasActiveFilters && (
        <AccountEmptyState
          icon={<Package className="w-6 h-6" />}
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
                className="min-h-[44px] rounded-full px-6 font-bold"
                leftIcon={<Store className="w-4 h-4 text-white" />}
              >
                Explorar comercios locales
              </Button>
            </Link>
          }
        />
      )}

      {/* Filtered Empty State */}
      {!isLoading && !error && items.length === 0 && hasActiveFilters && (
        <AccountEmptyState
          icon={<Search className="w-6 h-6" />}
          title="No encontramos pedidos con esos filtros"
          description="Prueba buscando otro comercio o limpia los filtros para ver todos tus pedidos."
          action={
            <button
              type="button"
              onClick={resetFilters}
              className="min-h-[44px] px-5 rounded-full bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors cursor-pointer"
            >
              Limpiar filtros
            </button>
          }
        />
      )}

      {/* Orders List */}
      {!isLoading && !error && items.length > 0 && (
        <>
          <div
            className={cn(
              'grid grid-cols-1 xl:grid-cols-2 gap-4 transition-opacity duration-150',
              isFetching && 'opacity-70'
            )}
          >
            {items.map((order) => {
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
                <article
                  key={order.id}
                  className="bg-white rounded-xl border border-[#e7e0d6] hover:border-[#005141]/40 transition-colors p-4 sm:p-5 flex flex-col justify-between gap-4"
                >
                  <div className="space-y-2 min-w-0">
                    <div className="flex flex-wrap items-center gap-2.5">
                      <span className="font-semibold text-[#1c1917] text-base">
                        {order.storeName}
                      </span>
                      <span className="text-xs font-mono tabular-nums text-[#78716c]">
                        #{order.orderNumber}
                      </span>
                      <OrderStatusBadge status={order.status} size="sm" />
                    </div>

                    <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-[#57534e]">
                      <span className="flex items-center gap-1.5 font-medium text-[#1c1917] tabular-nums">
                        <Clock className="w-3.5 h-3.5 text-[#005141] shrink-0" />
                        <span className="capitalize">{formattedDate}</span> ({formattedTimeWindow})
                      </span>

                      <span className="flex items-center gap-1.5">
                        {isDelivery ? (
                          <>
                            <MapPin className="w-3.5 h-3.5 text-[#005141] shrink-0" />
                            <span>Entrega a domicilio</span>
                          </>
                        ) : (
                          <>
                            <Store className="w-3.5 h-3.5 text-[#005141] shrink-0" />
                            <span>Recojo en tienda</span>
                          </>
                        )}
                      </span>

                      <span className="tabular-nums">
                        {order.totalItems} {order.totalItems === 1 ? 'artículo' : 'artículos'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-4 pt-3 border-t border-[#e7e0d6]/70">
                    <div>
                      <span className="text-[11px] text-[#78716c] block uppercase tracking-wider font-semibold">
                        Total
                      </span>
                      <span className="text-base font-bold text-[#1c1917] tabular-nums">
                        S/ {Number(order.totalAmount).toFixed(2)}
                      </span>
                    </div>

                    <Link
                      href={`/mi-jaldishop/pedidos/${order.orderNumber}`}
                      className="min-h-[44px] inline-flex items-center gap-1.5 px-4 text-xs sm:text-sm font-semibold text-[#005141] bg-[#f0fdfa] hover:bg-[#ccfbf1] border border-[#ccfbf1] rounded-lg transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                    >
                      <span>Ver detalle</span>
                      <ChevronRight className="w-4 h-4" />
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>

          <AccountPagination
            page={page}
            pageSize={PAGE_SIZE}
            totalItems={totalElements}
            onPageChange={setPage}
            itemLabel="pedidos"
          />
        </>
      )}

      {/* Nota Informativa sobre la Operación MYPE */}
      <div className="p-4 sm:p-5 rounded-xl bg-[#f0fdfa] border border-[#ccfbf1] flex items-start gap-3 text-xs text-[#1c1917]">
        <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold text-sm text-[#1c1917]">
            ¿Cómo se coordinan las entregas en JaldiShop?
          </p>
          <p className="leading-relaxed text-[#57534e]">
            Los comercios reciben los pedidos confirmados con su franja de preparación. Puedes consultar
            directamente con la tienda a través de los números oficiales de WhatsApp publicados en cada perfil de comercio.
          </p>
        </div>
      </div>
    </div>
  );
}

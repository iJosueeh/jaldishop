'use client';

import React, { useMemo, useState } from 'react';
import Link from 'next/link';
import {
  BarChart3,
  Download,
  ShoppingBag,
  Wallet,
  RotateCcw,
  Calendar,
  Package,
  AlertCircle,
  Store,
} from 'lucide-react';
import { useCustomerAnalytics } from '../hooks/useCustomerAnalytics';
import { Button } from '@/shared/components/ui/Button';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { formatCurrency } from '@/shared/utils/formatters';

type PresetDays = 7 | 30 | 90 | 'custom';

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getPresetRange(days: 7 | 30 | 90): { from: string; to: string } {
  const toDate = new Date();
  const fromDate = new Date(toDate);
  fromDate.setDate(toDate.getDate() - days);
  return { from: toIsoDate(fromDate), to: toIsoDate(toDate) };
}

function formatDateDisplay(iso: string | null | undefined): string {
  if (!iso) return 'Sin registro';
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return iso;
  return parsed.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

export function AnalyticsView() {
  const defaultRange = useMemo(() => getPresetRange(30), []);
  const [activePreset, setActivePreset] = useState<PresetDays>(30);
  const [from, setFrom] = useState<string>(defaultRange.from);
  const [to, setTo] = useState<string>(defaultRange.to);

  const {
    summary,
    isLoading,
    isFetching,
    error,
    reload,
    downloadPdf,
    isDownloadingPdf,
  } = useCustomerAnalytics({ from, to });

  const handlePreset = (days: 7 | 30 | 90) => {
    const range = getPresetRange(days);
    setActivePreset(days);
    setFrom(range.from);
    setTo(range.to);
  };

  if (isLoading && !summary) {
    return (
      <div className="space-y-6 w-full" role="status" aria-busy="true" aria-label="Cargando estadísticas">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-stone-200/80">
          <div className="space-y-2">
            <Skeleton className="h-7 w-56 bg-stone-200 rounded-lg" />
            <Skeleton className="h-4 w-80 bg-stone-200 rounded-md" />
          </div>
          <Skeleton className="h-11 w-40 bg-stone-200 rounded-full" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, idx) => (
            <Skeleton key={idx} className="h-28 bg-stone-200 rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-64 bg-stone-200 rounded-2xl" />
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div
        role="alert"
        className="rounded-2xl border border-rose-200 bg-rose-50 p-8 text-center space-y-3"
      >
        <AlertCircle className="w-8 h-8 text-rose-600 mx-auto" />
        <h3 className="text-base font-bold text-rose-900">No pudimos cargar tus estadísticas</h3>
        <p className="text-sm text-rose-700 max-w-md mx-auto">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()}>
          Reintentar
        </Button>
      </div>
    );
  }

  const spending = summary?.spending;
  const totalOrders = spending?.totalOrdersCount ?? 0;
  const totalSpent = Number(spending?.totalSpentAmount ?? 0);
  const totalRefunded = Number(spending?.totalRefundedAmount ?? 0);
  const topProducts = summary?.topPurchasedProducts ?? [];
  const hasActivity = totalOrders > 0 || topProducts.length > 0;

  return (
    <div className="space-y-6 w-full">
      {/* Header + Export PDF */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-stone-200/80">
        <div>
          <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
            Mis Estadísticas de Compras
          </h2>
          <p className="text-xs sm:text-sm text-stone-600 mt-0.5 leading-relaxed">
            Consulta tu gasto acumulado, pedidos realizados y productos más comprados por período.
          </p>
        </div>

        <Button
          type="button"
          variant="primary"
          size="md"
          isLoading={isDownloadingPdf}
          onClick={() => downloadPdf()}
          leftIcon={<Download className="w-4 h-4" />}
          className="min-h-[44px] rounded-full px-5 font-semibold shrink-0 cursor-pointer"
        >
          {isDownloadingPdf ? 'Generando PDF...' : 'Exportar PDF'}
        </Button>
      </div>

      {/* Period Filters */}
      <div className="rounded-2xl bg-white border border-stone-200/90 p-4 shadow-2xs flex flex-col lg:flex-row lg:items-end justify-between gap-4">
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Rangos rápidos">
          {([7, 30, 90] as const).map((days) => {
            const isActive = activePreset === days;
            return (
              <button
                key={days}
                type="button"
                onClick={() => handlePreset(days)}
                className={`min-h-[44px] px-4 rounded-xl text-xs font-semibold border transition-colors duration-150 cursor-pointer ${
                  isActive
                    ? 'bg-[#072720] text-white border-[#072720]'
                    : 'bg-stone-50 text-stone-700 border-stone-200 hover:bg-stone-100'
                }`}
              >
                Últimos {days} días
              </button>
            );
          })}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label
              htmlFor="analytics-from"
              className="block text-[11px] font-bold uppercase tracking-wider text-stone-600 mb-1"
            >
              Desde
            </label>
            <input
              id="analytics-from"
              type="date"
              value={from}
              max={to}
              onChange={(e) => {
                setActivePreset('custom');
                setFrom(e.target.value);
              }}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-stone-300 bg-white text-sm text-stone-900 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div>
            <label
              htmlFor="analytics-to"
              className="block text-[11px] font-bold uppercase tracking-wider text-stone-600 mb-1"
            >
              Hasta
            </label>
            <input
              id="analytics-to"
              type="date"
              value={to}
              min={from}
              onChange={(e) => {
                setActivePreset('custom');
                setTo(e.target.value);
              }}
              className="w-full min-h-[44px] px-3 py-2 rounded-xl border border-stone-300 bg-white text-sm text-stone-900 focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>
        </div>
      </div>

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <article className="rounded-2xl bg-white border border-stone-200/90 p-5 shadow-2xs space-y-2">
          <div className="flex items-center justify-between text-stone-500">
            <span className="text-xs font-semibold">Pedidos en el período</span>
            <ShoppingBag className="w-4 h-4 text-[#005141]" />
          </div>
          <p className="text-2xl font-bold text-stone-900 tabular-nums">
            {isFetching && !summary ? '—' : totalOrders}
          </p>
        </article>

        <article className="rounded-2xl bg-white border border-stone-200/90 p-5 shadow-2xs space-y-2">
          <div className="flex items-center justify-between text-stone-500">
            <span className="text-xs font-semibold">Total gastado</span>
            <Wallet className="w-4 h-4 text-[#005141]" />
          </div>
          <p className="text-2xl font-bold text-[#005141] tabular-nums">
            {formatCurrency(totalSpent, 'PEN')}
          </p>
        </article>

        <article className="rounded-2xl bg-white border border-stone-200/90 p-5 shadow-2xs space-y-2">
          <div className="flex items-center justify-between text-stone-500">
            <span className="text-xs font-semibold">Total reembolsado</span>
            <RotateCcw className="w-4 h-4 text-amber-600" />
          </div>
          <p className="text-2xl font-bold text-stone-900 tabular-nums">
            {formatCurrency(totalRefunded, 'PEN')}
          </p>
        </article>

        <article className="rounded-2xl bg-white border border-stone-200/90 p-5 shadow-2xs space-y-2">
          <div className="flex items-center justify-between text-stone-500">
            <span className="text-xs font-semibold">Última compra</span>
            <Calendar className="w-4 h-4 text-stone-500" />
          </div>
          <p className="text-base font-bold text-stone-900 pt-1">
            {formatDateDisplay(spending?.lastOrderAt)}
          </p>
        </article>
      </div>

      {/* Top Purchased Products or Empty State */}
      {!hasActivity ? (
        <div
          role="status"
          className="rounded-2xl border border-dashed border-stone-300 bg-white p-10 text-center space-y-3"
        >
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-200/70 text-[#005141] flex items-center justify-center mx-auto">
            <BarChart3 className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-stone-900">
            No registras compras en este período
          </h3>
          <p className="text-sm text-stone-600 max-w-md mx-auto">
            Prueba ampliando el rango de fechas o explora comercios locales para realizar tu próximo pedido.
          </p>
          <div className="pt-2">
            <Link href="/#tiendas-destacadas">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Store className="w-4 h-4 text-[#005141]" />}
                className="rounded-full"
              >
                Explorar comercios locales
              </Button>
            </Link>
          </div>
        </div>
      ) : (
        <section
          aria-labelledby="top-products-heading"
          className="rounded-2xl bg-white border border-stone-200/90 shadow-2xs overflow-hidden"
        >
          <div className="p-5 border-b border-stone-200/80 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <Package className="w-5 h-5 text-[#005141]" />
              <h3 id="top-products-heading" className="font-display text-lg font-bold text-stone-900">
                Productos más comprados
              </h3>
            </div>
            <span className="text-xs font-semibold text-stone-500">
              {topProducts.length} {topProducts.length === 1 ? 'producto' : 'productos'}
            </span>
          </div>

          {topProducts.length === 0 ? (
            <p className="p-6 text-sm text-stone-500 text-center">
              Aún no hay detalle de productos destacados para este período.
            </p>
          ) : (
            <>
              {/* Mobile List */}
              <ul role="list" className="divide-y divide-stone-100 sm:hidden">
                {topProducts.map((item) => (
                  <li key={item.variantId} className="p-4 space-y-1.5">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="text-sm font-bold text-stone-900">{item.productName}</p>
                        {item.variantName && (
                          <p className="text-xs text-stone-500">{item.variantName}</p>
                        )}
                      </div>
                      <span className="text-sm font-bold text-[#005141] tabular-nums shrink-0">
                        {formatCurrency(item.totalRevenue, 'PEN')}
                      </span>
                    </div>
                    <p className="text-xs text-stone-600 tabular-nums">
                      {item.unitsSold} {item.unitsSold === 1 ? 'unidad' : 'unidades'}
                    </p>
                  </li>
                ))}
              </ul>

              {/* Desktop Table */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-stone-200/80 bg-stone-50/70 text-[11px] font-bold uppercase tracking-wider text-stone-500">
                      <th className="py-3 px-5">Producto</th>
                      <th className="py-3 px-5">Presentación</th>
                      <th className="py-3 px-5 text-right">Unidades</th>
                      <th className="py-3 px-5 text-right">Monto</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-100 text-sm">
                    {topProducts.map((item) => (
                      <tr key={item.variantId} className="hover:bg-stone-50/60 transition-colors">
                        <td className="py-3.5 px-5 font-semibold text-stone-900">
                          {item.productName}
                        </td>
                        <td className="py-3.5 px-5 text-stone-600">
                          {item.variantName || 'Estándar'}
                        </td>
                        <td className="py-3.5 px-5 text-right text-stone-700 tabular-nums">
                          {item.unitsSold}
                        </td>
                        <td className="py-3.5 px-5 text-right font-bold text-[#005141] tabular-nums">
                          {formatCurrency(item.totalRevenue, 'PEN')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>
      )}
    </div>
  );
}

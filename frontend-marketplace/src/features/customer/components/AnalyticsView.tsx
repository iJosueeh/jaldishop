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
import { AccountPageHeader } from './AccountPageHeader';
import { AccountMetricTile } from './AccountMetricTile';
import { AccountEmptyState } from './AccountEmptyState';
import { Button } from '@/shared/components/ui/Button';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { formatCurrency } from '@/shared/utils/formatters';
import { cn } from '@/shared/utils/cn';

type PresetDays = 7 | 30 | 90 | 'custom';
type SortMetric = 'REVENUE' | 'UNITS';

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
  const [sortBy, setSortBy] = useState<SortMetric>('REVENUE');

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

  const spending = summary?.spending;
  const totalOrders = spending?.totalOrdersCount ?? 0;
  const totalSpent = Number(spending?.totalSpentAmount ?? 0);
  const totalRefunded = Number(spending?.totalRefundedAmount ?? 0);
  const avgTicket = totalOrders > 0 ? totalSpent / totalOrders : 0;

  const sortedProducts = useMemo(() => {
    const raw = summary?.topPurchasedProducts ?? [];
    return [...raw].sort((a, b) =>
      sortBy === 'UNITS'
        ? b.unitsSold - a.unitsSold || Number(b.totalRevenue) - Number(a.totalRevenue)
        : Number(b.totalRevenue) - Number(a.totalRevenue) || b.unitsSold - a.unitsSold
    );
  }, [summary?.topPurchasedProducts, sortBy]);

  const maxMetricValue = useMemo(() => {
    if (sortedProducts.length === 0) return 1;
    const values = sortedProducts.map((p) =>
      sortBy === 'UNITS' ? p.unitsSold : Number(p.totalRevenue)
    );
    return Math.max(1, ...values);
  }, [sortedProducts, sortBy]);

  if (isLoading && !summary) {
    return (
      <div className="space-y-6 w-full" role="status" aria-busy="true" aria-label="Cargando estadísticas">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-[#e7e0d6]">
          <div className="space-y-2">
            <Skeleton className="h-7 w-56 bg-[#f0eae0] rounded-lg" />
            <Skeleton className="h-4 w-80 bg-[#f0eae0] rounded-md" />
          </div>
          <Skeleton className="h-11 w-40 bg-[#f0eae0] rounded-full" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, idx) => (
            <Skeleton key={idx} className="h-24 bg-[#f0eae0] rounded-xl" />
          ))}
        </div>
        <Skeleton className="h-64 bg-[#f0eae0] rounded-xl" />
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div
        role="alert"
        className="rounded-xl border border-rose-200 bg-rose-50/70 p-8 text-center space-y-3"
      >
        <AlertCircle className="w-8 h-8 text-rose-600 mx-auto" />
        <h2 className="text-base font-bold text-rose-950">No pudimos cargar tus estadísticas</h2>
        <p className="text-sm text-rose-800 max-w-md mx-auto">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()} className="min-h-[44px]">
          Reintentar
        </Button>
      </div>
    );
  }

  const hasActivity = totalOrders > 0 || sortedProducts.length > 0;

  return (
    <div className="space-y-6 w-full">
      {/* Header + Export PDF */}
      <AccountPageHeader
        title="Mis Estadísticas de Compras"
        description="Consulta tu gasto acumulado, pedidos realizados y productos más comprados por período."
        actions={
          <Button
            type="button"
            variant="primary"
            size="md"
            isLoading={isDownloadingPdf}
            onClick={() => downloadPdf()}
            leftIcon={<Download className="w-4 h-4" />}
            className="min-h-[44px] w-full sm:w-auto rounded-full px-5 font-semibold shrink-0 cursor-pointer"
          >
            {isDownloadingPdf ? 'Generando PDF...' : 'Exportar PDF'}
          </Button>
        }
      />

      {/* Period Filters */}
      <div className="rounded-xl bg-white border border-[#e7e0d6] p-4 flex flex-col lg:flex-row lg:items-end justify-between gap-4">
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Rangos rápidos">
          {([7, 30, 90] as const).map((days) => {
            const isActive = activePreset === days;
            return (
              <button
                key={days}
                type="button"
                onClick={() => handlePreset(days)}
                className={cn(
                  'min-h-[44px] px-4 rounded-lg text-xs font-semibold border transition-colors duration-150 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                  isActive
                    ? 'bg-[#072720] text-white border-[#072720]'
                    : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                )}
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
              className="block text-[11px] font-bold uppercase tracking-wider text-[#57534e] mb-1"
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
              className="w-full min-h-[44px] px-3 py-2 rounded-lg border border-[#e7e0d6] bg-white text-sm text-[#1c1917] tabular-nums focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div>
            <label
              htmlFor="analytics-to"
              className="block text-[11px] font-bold uppercase tracking-wider text-[#57534e] mb-1"
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
              className="w-full min-h-[44px] px-3 py-2 rounded-lg border border-[#e7e0d6] bg-white text-sm text-[#1c1917] tabular-nums focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>
        </div>
      </div>

      {/* KPI Summary Tiles */}
      <div
        className={cn(
          'grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 transition-opacity duration-150',
          isFetching && 'opacity-70'
        )}
      >
        <AccountMetricTile
          label="Pedidos en el período"
          value={totalOrders}
          icon={<ShoppingBag className="w-4 h-4 text-[#005141]" />}
        />
        <AccountMetricTile
          label="Total gastado"
          value={formatCurrency(totalSpent, 'PEN')}
          valueClassName="text-[#005141]"
          hint={
            totalOrders > 0
              ? `Prom. ${formatCurrency(avgTicket, 'PEN')} / pedido`
              : undefined
          }
          icon={<Wallet className="w-4 h-4 text-[#005141]" />}
        />
        <AccountMetricTile
          label="Total reembolsado"
          value={formatCurrency(totalRefunded, 'PEN')}
          icon={<RotateCcw className="w-4 h-4 text-[#ea580c]" />}
        />
        <AccountMetricTile
          label="Última compra"
          value={formatDateDisplay(spending?.lastOrderAt)}
          valueClassName="text-base sm:text-lg pt-1"
          icon={<Calendar className="w-4 h-4 text-[#57534e]" />}
        />
      </div>

      {/* Top Purchased Products or Empty State */}
      {!hasActivity ? (
        <AccountEmptyState
          icon={<BarChart3 className="w-6 h-6" />}
          title="No registras compras en este período"
          description="Prueba ampliando el rango de fechas o explora comercios locales para realizar tu próximo pedido."
          action={
            <Link href="/#tiendas-destacadas">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Store className="w-4 h-4 text-[#005141]" />}
                className="min-h-[44px] rounded-full"
              >
                Explorar comercios locales
              </Button>
            </Link>
          }
        />
      ) : (
        <section
          aria-labelledby="top-products-heading"
          className={cn(
            'rounded-xl bg-white border border-[#e7e0d6] overflow-hidden transition-opacity duration-150',
            isFetching && 'opacity-70'
          )}
        >
          <div className="px-4 sm:px-5 py-4 border-b border-[#e7e0d6] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <Package className="w-4 h-4 text-[#005141]" aria-hidden="true" />
              <h2 id="top-products-heading" className="font-display text-base sm:text-lg font-bold text-[#1c1917]">
                Productos más comprados
              </h2>
              <span className="text-xs font-semibold text-[#57534e] tabular-nums">
                ({sortedProducts.length})
              </span>
            </div>

            {sortedProducts.length > 1 && (
              <div className="flex items-center gap-1.5" role="group" aria-label="Ordenar productos">
                <button
                  type="button"
                  onClick={() => setSortBy('REVENUE')}
                  className={cn(
                    'min-h-[44px] px-3 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    sortBy === 'REVENUE'
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  Por monto
                </button>
                <button
                  type="button"
                  onClick={() => setSortBy('UNITS')}
                  className={cn(
                    'min-h-[44px] px-3 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    sortBy === 'UNITS'
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  Por unidades
                </button>
              </div>
            )}
          </div>

          {sortedProducts.length === 0 ? (
            <p className="p-6 text-sm text-[#57534e] text-center">
              Aún no hay detalle de productos destacados para este período.
            </p>
          ) : (
            <>
              {/* Mobile List */}
              <ul role="list" className="divide-y divide-[#e7e0d6]/70 sm:hidden">
                {sortedProducts.map((item, idx) => {
                  const val = sortBy === 'UNITS' ? item.unitsSold : Number(item.totalRevenue);
                  const pct = Math.max(6, Math.round((val / maxMetricValue) * 100));

                  return (
                    <li key={item.variantId} className="p-4 space-y-2">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-start gap-2.5 min-w-0">
                          <span className="text-xs font-bold text-[#78716c] tabular-nums pt-0.5 shrink-0">
                            #{idx + 1}
                          </span>
                          <div className="min-w-0">
                            <p className="text-sm font-bold text-[#1c1917]">{item.productName}</p>
                            {item.variantName && (
                              <p className="text-xs text-[#57534e]">{item.variantName}</p>
                            )}
                          </div>
                        </div>
                        <span className="text-sm font-bold text-[#005141] tabular-nums shrink-0">
                          {formatCurrency(item.totalRevenue, 'PEN')}
                        </span>
                      </div>
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex-1 h-1.5 rounded-full bg-[#f0eae0] overflow-hidden">
                          <div
                            className="h-full rounded-full bg-[#005141]"
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                        <span className="text-xs text-[#57534e] tabular-nums shrink-0">
                          {item.unitsSold} {item.unitsSold === 1 ? 'unidad' : 'unidades'}
                        </span>
                      </div>
                    </li>
                  );
                })}
              </ul>

              {/* Desktop Table */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-[#e7e0d6] bg-[#faf7f2] text-[11px] font-bold uppercase tracking-wider text-[#57534e]">
                      <th className="py-3 px-5 w-14">#</th>
                      <th className="py-3 px-5">Producto</th>
                      <th className="py-3 px-5">Presentación</th>
                      <th className="py-3 px-5 text-right">Unidades</th>
                      <th className="py-3 px-5 text-right w-48">Monto</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#e7e0d6]/70 text-sm">
                    {sortedProducts.map((item, idx) => {
                      const val = sortBy === 'UNITS' ? item.unitsSold : Number(item.totalRevenue);
                      const pct = Math.max(6, Math.round((val / maxMetricValue) * 100));

                      return (
                        <tr key={item.variantId} className="hover:bg-[#faf7f2]/70 transition-colors">
                          <td className="py-3.5 px-5 text-xs font-bold text-[#78716c] tabular-nums">
                            #{idx + 1}
                          </td>
                          <td className="py-3.5 px-5 font-semibold text-[#1c1917]">
                            {item.productName}
                          </td>
                          <td className="py-3.5 px-5 text-[#57534e]">
                            {item.variantName || 'Estándar'}
                          </td>
                          <td className="py-3.5 px-5 text-right text-[#1c1917] tabular-nums">
                            {item.unitsSold}
                          </td>
                          <td className="py-3.5 px-5 text-right">
                            <div className="space-y-1">
                              <span className="font-bold text-[#005141] tabular-nums block">
                                {formatCurrency(item.totalRevenue, 'PEN')}
                              </span>
                              <div className="h-1.5 w-full rounded-full bg-[#f0eae0] overflow-hidden">
                                <div
                                  className="h-full rounded-full bg-[#005141] ml-auto"
                                  style={{ width: `${pct}%` }}
                                />
                              </div>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
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

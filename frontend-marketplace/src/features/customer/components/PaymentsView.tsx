'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  CreditCard,
  ShieldCheck,
  ArrowRight,
  CheckCircle2,
  Clock,
  XCircle,
  RotateCcw,
  AlertCircle,
  RefreshCw,
  Store,
  Search,
} from 'lucide-react';
import { useCustomerPaymentsPaged } from '../hooks/useCustomerPaymentsPaged';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { formatCurrency } from '@/shared/utils/formatters';
import { AccountPageHeader } from './AccountPageHeader';
import { AccountEmptyState } from './AccountEmptyState';
import { AccountPagination } from './AccountPagination';
import { Badge, BadgeProps } from '@/shared/components/ui/Badge';
import { cn } from '@/shared/utils/cn';

type PaymentFilter = 'ALL' | 'WITH_ORDER' | 'APPROVED' | 'REFUND' | 'NO_ORDER';

const PAGE_SIZE = 8;

interface StatusMeta {
  label: string;
  variant: NonNullable<BadgeProps['variant']>;
  icon: React.ComponentType<{ className?: string }>;
}

const PAYMENT_STATUS_MAP: Record<string, StatusMeta> = {
  APPROVED: { label: 'Aprobado', variant: 'jade', icon: CheckCircle2 },
  PAID: { label: 'Pagado', variant: 'jade', icon: CheckCircle2 },
  PENDING: { label: 'Pendiente', variant: 'amber', icon: Clock },
  IN_PROCESS: { label: 'En proceso', variant: 'info', icon: Clock },
  REJECTED: { label: 'Rechazado', variant: 'danger', icon: XCircle },
  CANCELLED: { label: 'Cancelado', variant: 'danger', icon: XCircle },
  REFUNDED: { label: 'Reembolsado', variant: 'default', icon: RotateCcw },
};

const ORDER_STATUS_LABELS: Record<string, string> = {
  CONFIRMED: 'Confirmado',
  IN_PREPARATION: 'En preparación',
  READY: 'Listo',
  OUT_FOR_DELIVERY: 'En camino',
  COMPLETED: 'Entregado',
  CANCELLED: 'Cancelado',
};

const REFUND_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Reembolso pendiente',
  APPROVED: 'Reembolso aprobado',
  COMPLETED: 'Reembolsado',
  REJECTED: 'Reembolso rechazado',
};

const NO_REFUND_STATUSES = new Set(['', 'NONE', 'NOT_REQUESTED', 'NOT_REQUIRED']);

const FILTER_OPTIONS: { key: PaymentFilter; label: string }[] = [
  { key: 'ALL', label: 'Todos' },
  { key: 'WITH_ORDER', label: 'Con pedido' },
  { key: 'APPROVED', label: 'Aprobados' },
  { key: 'REFUND', label: 'Con reembolso' },
  { key: 'NO_ORDER', label: 'Sin orden' },
];

function getPaymentStatusMeta(status: string): StatusMeta {
  const key = status?.toUpperCase() || '';
  return (
    PAYMENT_STATUS_MAP[key] || {
      label: status || 'Desconocido',
      variant: 'default',
      icon: CreditCard,
    }
  );
}

function hasActiveRefund(refundStatus?: string | null): boolean {
  if (!refundStatus) return false;
  return !NO_REFUND_STATUSES.has(refundStatus.toUpperCase());
}

export function PaymentsView() {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<PaymentFilter>('ALL');
  const [page, setPage] = useState(1);

  const debouncedSearch = useDebouncedValue(search, 300);

  const {
    items,
    totalElements,
    isLoading,
    isFetching,
    isError,
    refetch,
  } = useCustomerPaymentsPaged({
    page: page - 1,
    size: PAGE_SIZE,
    filter,
    q: debouncedSearch,
  });

  const hasActiveFilters = filter !== 'ALL' || debouncedSearch.trim().length > 0;

  const handleFilterChange = (nextFilter: PaymentFilter) => {
    setFilter(nextFilter);
    setPage(1);
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    setPage(1);
  };

  const resetFilters = () => {
    setSearch('');
    setFilter('ALL');
    setPage(1);
  };

  if (isLoading) {
    return (
      <div
        role="status"
        aria-busy="true"
        aria-label="Cargando historial de pagos"
        className="space-y-6 animate-pulse"
      >
        <div className="space-y-2 pb-5 border-b border-[#e7e0d6]">
          <div className="h-7 w-40 bg-[#f0eae0] rounded-lg" />
          <div className="h-4 w-80 bg-[#f0eae0] rounded-md" />
        </div>
        <div className="h-12 bg-[#f0eae0] rounded-xl" />
        <div className="h-56 bg-white rounded-xl border border-[#e7e0d6]" />
      </div>
    );
  }

  if (isError) {
    return (
      <div
        role="alert"
        className="rounded-xl border border-rose-200 bg-rose-50/70 p-6 text-center text-sm text-rose-900 space-y-3"
      >
        <AlertCircle className="w-7 h-7 text-rose-600 mx-auto" />
        <p className="font-semibold">No pudimos consultar tus pagos.</p>
        <button
          type="button"
          onClick={() => refetch()}
          className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-lg bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Reintentar</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-5 w-full">
      <AccountPageHeader
        title="Mis Pagos"
        description="Tus pagos se procesan de forma segura con Mercado Pago. JaldiShop no guarda tarjetas ni datos bancarios."
        badge={
          totalElements > 0 ? (
            <Badge variant="default" size="sm" className="tabular-nums">
              {totalElements} {totalElements === 1 ? 'registro' : 'registros'}
            </Badge>
          ) : undefined
        }
      />

      <div className="rounded-xl border border-[#ccfbf1] bg-[#f0fdfa] px-4 py-3 text-xs sm:text-sm text-[#005141] flex items-center gap-2.5">
        <ShieldCheck className="h-4 w-4 shrink-0" aria-hidden="true" />
        <span>Tus medios de pago sensibles permanecen con el proveedor de pagos.</span>
      </div>

      {(totalElements > 0 || hasActiveFilters) && (
        <div className="rounded-xl border border-[#e7e0d6] bg-white p-3.5 sm:p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search
              className="w-4 h-4 text-[#78716c] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              aria-hidden="true"
            />
            <input
              type="search"
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
              aria-label="Buscar pagos"
              placeholder="Buscar por # de pedido, comercio o referencia..."
              className="w-full min-h-[44px] pl-9 pr-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2]/50 focus:bg-white text-sm text-[#1c1917] placeholder-[#78716c] focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div
            role="group"
            aria-label="Filtrar pagos"
            className="flex flex-wrap items-center gap-1.5"
          >
            {FILTER_OPTIONS.map((option) => {
              const isActive = filter === option.key;
              return (
                <button
                  key={option.key}
                  type="button"
                  onClick={() => handleFilterChange(option.key)}
                  className={cn(
                    'min-h-[44px] px-3.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isActive
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {items.length === 0 && !hasActiveFilters ? (
        <AccountEmptyState
          icon={<CreditCard className="w-6 h-6" />}
          title="Aún no registras pagos"
          description="Cuando completes una compra con Mercado Pago, aquí verás el comprobante, estado y detalle de cada transacción."
        />
      ) : items.length === 0 ? (
        <AccountEmptyState
          icon={<Search className="w-6 h-6" />}
          title="No hay pagos que coincidan con tu búsqueda"
          description="Prueba cambiando el filtro seleccionado o limpia el buscador para ver todos tus registros."
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
      ) : (
        <>
          <div
            className={cn(
              'rounded-xl border border-[#e7e0d6] bg-white overflow-hidden transition-opacity duration-150',
              isFetching && 'opacity-70'
            )}
          >
            {/* Mobile Compact List */}
            <ul role="list" className="divide-y divide-[#e7e0d6]/70 md:hidden">
              {items.map((payment) => {
                const statusMeta = getPaymentStatusMeta(payment.paymentStatus);
                const StatusIcon = statusMeta.icon;
                const showRefund = hasActiveRefund(payment.refundStatus);
                const orderStatusLabel = payment.orderStatus
                  ? ORDER_STATUS_LABELS[payment.orderStatus] || payment.orderStatus
                  : null;

                return (
                  <li key={payment.paymentId} className="p-4 space-y-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="space-y-1 min-w-0">
                        <p className="font-semibold text-[#1c1917] tabular-nums">
                          {payment.orderNumber
                            ? `Pedido #${payment.orderNumber}`
                            : 'Intento de pago'}
                        </p>
                        {payment.storeName ? (
                          <p className="text-xs font-medium text-[#57534e] flex items-center gap-1.5">
                            <Store className="w-3.5 h-3.5 text-[#005141] shrink-0" />
                            <span className="truncate">{payment.storeName}</span>
                            {typeof payment.totalItems === 'number' && (
                              <span className="text-[#78716c] tabular-nums">
                                · {payment.totalItems}{' '}
                                {payment.totalItems === 1 ? 'artículo' : 'artículos'}
                              </span>
                            )}
                          </p>
                        ) : !payment.orderNumber ? (
                          <p className="text-xs text-[#78716c]">
                            {showRefund
                              ? 'Sin orden generada · Reembolso en trámite'
                              : `Sin orden confirmada · Ref. ${payment.paymentId.slice(0, 8)}`}
                          </p>
                        ) : null}
                        <p className="text-xs text-[#57534e] tabular-nums">
                          Mercado Pago · {new Date(payment.createdAt).toLocaleDateString('es-PE')}
                        </p>
                      </div>
                      <p className="font-bold text-[#005141] tabular-nums shrink-0">
                        {formatCurrency(payment.amount, payment.currency)}
                      </p>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <Badge variant={statusMeta.variant} size="sm" className="select-text">
                          <StatusIcon className="w-3 h-3" aria-hidden="true" />
                          <span>{statusMeta.label}</span>
                        </Badge>
                        {orderStatusLabel && (
                          <Badge variant="default" size="sm" className="select-text">
                            {orderStatusLabel}
                          </Badge>
                        )}
                        {showRefund && (
                          <Badge variant="warning" size="sm" className="select-text">
                            {REFUND_STATUS_LABELS[payment.refundStatus.toUpperCase()] ||
                              payment.refundStatus}
                          </Badge>
                        )}
                      </div>

                      {payment.orderNumber && (
                        <Link
                          href={`/mi-jaldishop/pedidos/${encodeURIComponent(payment.orderNumber)}`}
                          className="min-h-[44px] inline-flex items-center gap-1 px-3 rounded-lg border border-[#e7e0d6] hover:border-[#005141] text-xs font-semibold text-[#1c1917] hover:text-[#005141] transition-colors"
                        >
                          <span>Ver pedido</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </Link>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>

            {/* Desktop Semantic Table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#e7e0d6] bg-[#faf7f2] text-[11px] font-bold uppercase tracking-wider text-[#57534e]">
                    <th className="py-3 px-5">Pedido y Comercio</th>
                    <th className="py-3 px-5">Fecha</th>
                    <th className="py-3 px-5">Estado del pago</th>
                    <th className="py-3 px-5">Reembolso</th>
                    <th className="py-3 px-5 text-right">Monto</th>
                    <th className="py-3 px-5 text-right">Acción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e7e0d6]/70 text-sm">
                  {items.map((payment) => {
                    const statusMeta = getPaymentStatusMeta(payment.paymentStatus);
                    const StatusIcon = statusMeta.icon;
                    const showRefund = hasActiveRefund(payment.refundStatus);
                    const orderStatusLabel = payment.orderStatus
                      ? ORDER_STATUS_LABELS[payment.orderStatus] || payment.orderStatus
                      : null;

                    return (
                      <tr key={payment.paymentId} className="hover:bg-[#faf7f2]/70 transition-colors">
                        <td className="py-3.5 px-5">
                          <div className="flex flex-col gap-0.5">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-semibold text-[#1c1917] tabular-nums">
                                {payment.orderNumber
                                  ? `Pedido #${payment.orderNumber}`
                                  : 'Intento de pago'}
                              </span>
                              {orderStatusLabel && (
                                <span className="text-xs text-[#57534e]">· {orderStatusLabel}</span>
                              )}
                            </div>
                            {payment.storeName ? (
                              <span className="text-xs text-[#57534e] inline-flex items-center gap-1.5 tabular-nums">
                                <Store className="w-3.5 h-3.5 text-[#005141] shrink-0" />
                                <span>{payment.storeName}</span>
                                {typeof payment.totalItems === 'number' && (
                                  <span className="text-[#78716c]">
                                    · {payment.totalItems}{' '}
                                    {payment.totalItems === 1 ? 'artículo' : 'artículos'}
                                  </span>
                                )}
                              </span>
                            ) : !payment.orderNumber ? (
                              <span className="text-xs text-[#78716c]">
                                {showRefund
                                  ? 'Sin orden generada · Reembolso en trámite'
                                  : `Sin orden confirmada · Ref. ${payment.paymentId.slice(0, 8)}`}
                              </span>
                            ) : null}
                          </div>
                        </td>

                        <td className="py-3.5 px-5 text-xs text-[#57534e] tabular-nums whitespace-nowrap">
                          <div>{new Date(payment.createdAt).toLocaleDateString('es-PE')}</div>
                          {payment.approvedAt && (
                            <div className="text-[11px] text-[#78716c]">
                              Aprobado {new Date(payment.approvedAt).toLocaleDateString('es-PE')}
                            </div>
                          )}
                        </td>

                        <td className="py-3.5 px-5">
                          <Badge variant={statusMeta.variant} size="sm" className="select-text">
                            <StatusIcon className="w-3 h-3" aria-hidden="true" />
                            <span>{statusMeta.label}</span>
                          </Badge>
                        </td>

                        <td className="py-3.5 px-5 text-xs">
                          {showRefund ? (
                            <span className="font-medium text-amber-800 tabular-nums">
                              {REFUND_STATUS_LABELS[payment.refundStatus.toUpperCase()] ||
                                payment.refundStatus}
                              {payment.refundedAt &&
                                ` (${new Date(payment.refundedAt).toLocaleDateString('es-PE')})`}
                            </span>
                          ) : (
                            <span className="text-[#78716c]">—</span>
                          )}
                        </td>

                        <td className="py-3.5 px-5 text-right font-bold text-[#005141] tabular-nums whitespace-nowrap">
                          {formatCurrency(payment.amount, payment.currency)}
                        </td>

                        <td className="py-3.5 px-5 text-right whitespace-nowrap">
                          {payment.orderNumber ? (
                            <Link
                              href={`/mi-jaldishop/pedidos/${encodeURIComponent(payment.orderNumber)}`}
                              className="min-h-[44px] inline-flex items-center gap-1 px-3 rounded-lg border border-[#e7e0d6] hover:border-[#005141] text-xs font-semibold text-[#1c1917] hover:text-[#005141] transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                            >
                              <span>Ver pedido</span>
                              <ArrowRight className="w-3.5 h-3.5" />
                            </Link>
                          ) : (
                            <span className="text-xs text-[#78716c]">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <AccountPagination
            page={page}
            pageSize={PAGE_SIZE}
            totalItems={totalElements}
            onPageChange={setPage}
            itemLabel="pagos"
          />
        </>
      )}
    </div>
  );
}

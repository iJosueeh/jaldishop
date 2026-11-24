'use client';

import Link from 'next/link';
import { CreditCard, ShieldCheck, ArrowRight } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getCustomerPaymentHistory } from '../services/customerPaymentService';
import { formatCurrency } from '@/shared/utils/formatters';

export function PaymentsView() {
  const { data = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['customer', 'payments'],
    queryFn: getCustomerPaymentHistory,
    staleTime: 60_000,
  });

  if (isLoading) {
    return (
      <div
        role="status"
        aria-busy="true"
        aria-label="Cargando historial de pagos"
        className="h-48 animate-pulse rounded-2xl border border-stone-200 bg-white"
      />
    );
  }

  if (isError) {
    return (
      <div
        role="alert"
        className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-sm text-rose-800 space-y-2"
      >
        <p>No pudimos consultar tus pagos.</p>
        <button
          type="button"
          onClick={() => refetch()}
          className="font-bold underline cursor-pointer"
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="border-b border-stone-200 pb-4">
        <h2 className="font-display text-2xl font-bold text-stone-900">Mis Pagos</h2>
        <p className="mt-1 text-sm text-stone-500">
          Tus pagos se procesan de forma segura con Mercado Pago. JaldiShop no guarda tarjetas ni datos bancarios.
        </p>
      </header>

      <div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4 text-sm text-emerald-900 flex items-center gap-2">
        <ShieldCheck className="h-4 w-4 shrink-0" />
        <span>Tus medios de pago sensibles permanecen con el proveedor de pagos.</span>
      </div>

      {data.length === 0 ? (
        <div
          role="status"
          className="rounded-2xl border border-dashed border-stone-300 bg-white p-10 text-center text-sm text-stone-500"
        >
          <CreditCard className="mx-auto mb-3 h-7 w-7 text-stone-400" />
          Aún no registras pagos.
        </div>
      ) : (
        <div className="space-y-3">
          {data.map((payment) => {
            const hasRefund =
              payment.refundStatus &&
              payment.refundStatus !== 'NONE' &&
              payment.refundStatus !== 'NOT_REQUESTED';

            return (
              <article
                key={payment.paymentId}
                className="flex flex-col gap-3 rounded-2xl border border-stone-200 bg-white p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold text-stone-900">
                      {payment.orderNumber ? `Pedido #${payment.orderNumber}` : 'Pago de pedido'}
                    </p>
                    {payment.orderStatus && (
                      <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-stone-100 text-stone-700">
                        {payment.orderStatus}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-stone-500">
                    Mercado Pago · {new Date(payment.createdAt).toLocaleDateString('es-PE')}
                    {payment.approvedAt &&
                      ` · Aprobado ${new Date(payment.approvedAt).toLocaleDateString('es-PE')}`}
                  </p>
                  {hasRefund && (
                    <p className="text-xs font-medium text-amber-800">
                      Reembolso: {payment.refundStatus}
                      {payment.refundedAt &&
                        ` (${new Date(payment.refundedAt).toLocaleDateString('es-PE')})`}
                    </p>
                  )}
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-4">
                  <div className="text-left sm:text-right">
                    <p className="font-bold text-[#005141] tabular-nums">
                      {formatCurrency(payment.amount, payment.currency)}
                    </p>
                    <p className="text-xs font-semibold text-stone-500">
                      {payment.paymentStatus}
                    </p>
                  </div>

                  {payment.orderNumber && (
                    <Link
                      href={`/mi-jaldishop/pedidos/${encodeURIComponent(payment.orderNumber)}`}
                      className="min-h-[44px] inline-flex items-center gap-1 px-3 rounded-xl border border-stone-200 hover:border-[#005141] text-xs font-semibold text-stone-700 hover:text-[#005141] transition-colors"
                    >
                      <span>Ver pedido</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

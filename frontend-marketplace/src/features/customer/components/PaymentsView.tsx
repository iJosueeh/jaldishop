'use client';

import { CreditCard, ShieldCheck } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getCustomerPaymentHistory } from '../services/customerPaymentService';
import { formatCurrency } from '@/shared/utils/formatters';

export function PaymentsView() {
  const { data = [], isLoading, isError, refetch } = useQuery({ queryKey: ['customer', 'payments'], queryFn: getCustomerPaymentHistory, staleTime: 60_000 });
  if (isLoading) return <div className="h-48 animate-pulse rounded-2xl border border-stone-200 bg-white" />;
  if (isError) return <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center text-sm text-rose-800">No pudimos consultar tus pagos. <button type="button" onClick={() => refetch()} className="font-bold underline">Reintentar</button></div>;
  return <div className="space-y-6"><header className="border-b border-stone-200 pb-4"><h1 className="text-2xl font-bold text-stone-900">Pagos</h1><p className="mt-1 text-sm text-stone-500">Tus pagos se procesan de forma segura con Mercado Pago. JaldiShop no guarda tarjetas ni datos bancarios.</p></header><div className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4 text-sm text-emerald-900"><ShieldCheck className="mr-2 inline h-4 w-4" />Tus medios de pago sensibles permanecen con el proveedor de pagos.</div>{data.length === 0 ? <div className="rounded-2xl border border-dashed border-stone-300 bg-white p-10 text-center text-sm text-stone-500"><CreditCard className="mx-auto mb-3 h-7 w-7 text-stone-400" />Aún no registras pagos.</div> : <div className="space-y-3">{data.map((payment) => <article key={payment.paymentId} className="flex flex-col gap-2 rounded-2xl border border-stone-200 bg-white p-4 shadow-xs sm:flex-row sm:items-center sm:justify-between"><div><p className="font-semibold text-stone-900">{payment.orderNumber ? `Pedido #${payment.orderNumber}` : 'Pago de pedido'}</p><p className="text-xs text-stone-500">Mercado Pago · {new Date(payment.createdAt).toLocaleDateString('es-PE')}</p></div><div className="text-left sm:text-right"><p className="font-bold text-[#005141]">{formatCurrency(payment.amount, payment.currency)}</p><p className="text-xs font-semibold text-stone-500">{payment.paymentStatus}</p></div></article>)}</div>}</div>;
}

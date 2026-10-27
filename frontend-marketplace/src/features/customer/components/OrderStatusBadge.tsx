import React from 'react';
import { OrderStatus } from '../types/customer.types';

interface OrderStatusBadgeProps {
  status: OrderStatus;
  size?: 'sm' | 'md';
}

const STATUS_CONFIG: Record<
  OrderStatus,
  { label: string; bg: string; text: string; dot: string; border: string }
> = {
  CONFIRMED: {
    label: 'Confirmado',
    bg: 'bg-emerald-50 text-emerald-800',
    text: 'text-emerald-800',
    dot: 'bg-emerald-500',
    border: 'border-emerald-200',
  },
  IN_PREPARATION: {
    label: 'En preparación',
    bg: 'bg-amber-50 text-amber-800',
    text: 'text-amber-800',
    dot: 'bg-amber-500 animate-pulse',
    border: 'border-amber-200',
  },
  READY: {
    label: 'Listo para entrega / recojo',
    bg: 'bg-sky-50 text-sky-800',
    text: 'text-sky-800',
    dot: 'bg-sky-500',
    border: 'border-sky-200',
  },
  OUT_FOR_DELIVERY: {
    label: 'En camino',
    bg: 'bg-indigo-50 text-indigo-800',
    text: 'text-indigo-800',
    dot: 'bg-indigo-500 animate-pulse',
    border: 'border-indigo-200',
  },
  COMPLETED: {
    label: 'Entregado',
    bg: 'bg-stone-100 text-stone-700',
    text: 'text-stone-700',
    dot: 'bg-stone-400',
    border: 'border-stone-200',
  },
  CANCELLED: {
    label: 'Cancelado',
    bg: 'bg-rose-50 text-rose-800',
    text: 'text-rose-800',
    dot: 'bg-rose-500',
    border: 'border-rose-200',
  },
};

export const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({ status, size = 'md' }) => {
  const config = STATUS_CONFIG[status] || {
    label: status,
    bg: 'bg-stone-100 text-stone-700',
    text: 'text-stone-700',
    dot: 'bg-stone-400',
    border: 'border-stone-200',
  };

  const isSmall = size === 'sm';

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${config.bg} ${config.border} ${
        isSmall ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs sm:text-sm'
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
      <span>{config.label}</span>
    </span>
  );
};

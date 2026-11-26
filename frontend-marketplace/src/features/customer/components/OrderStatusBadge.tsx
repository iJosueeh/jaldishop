import React from 'react';
import {
  CheckCircle2,
  Clock,
  PackageCheck,
  Truck,
  Check,
  XCircle,
} from 'lucide-react';
import { OrderStatus } from '../types/customer.types';

interface OrderStatusBadgeProps {
  status: OrderStatus;
  size?: 'sm' | 'md';
}

const STATUS_CONFIG: Record<
  OrderStatus,
  {
    label: string;
    bg: string;
    border: string;
    icon: React.ComponentType<{ className?: string }>;
  }
> = {
  CONFIRMED: {
    label: 'Confirmado',
    bg: 'bg-[#f0fdfa] text-[#005141]',
    border: 'border-[#ccfbf1]',
    icon: CheckCircle2,
  },
  IN_PREPARATION: {
    label: 'En preparación',
    bg: 'bg-[#fffbeb] text-[#92400e]',
    border: 'border-[#fde68a]',
    icon: Clock,
  },
  READY: {
    label: 'Listo para entrega / recojo',
    bg: 'bg-sky-50 text-sky-900',
    border: 'border-sky-200',
    icon: PackageCheck,
  },
  OUT_FOR_DELIVERY: {
    label: 'En camino',
    bg: 'bg-[#fff7ed] text-[#c2410c]',
    border: 'border-[#fed7aa]',
    icon: Truck,
  },
  COMPLETED: {
    label: 'Entregado',
    bg: 'bg-[#f7f3ec] text-[#1c1917]',
    border: 'border-[#e7e0d6]',
    icon: Check,
  },
  CANCELLED: {
    label: 'Cancelado',
    bg: 'bg-rose-50 text-rose-800',
    border: 'border-rose-200',
    icon: XCircle,
  },
};

export const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({ status, size = 'md' }) => {
  const config = STATUS_CONFIG[status] || {
    label: status,
    bg: 'bg-[#f7f3ec] text-[#1c1917]',
    border: 'border-[#e7e0d6]',
    icon: Clock,
  };

  const Icon = config.icon;
  const isSmall = size === 'sm';

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-semibold rounded-full border ${config.bg} ${config.border} ${
        isSmall ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-xs sm:text-sm'
      }`}
    >
      <Icon className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />
      <span>{config.label}</span>
    </span>
  );
};

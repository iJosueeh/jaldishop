import React from 'react';
import { CheckCircle2, Clock, Package, Truck, Check, AlertCircle } from 'lucide-react';
import { CustomerOrderStatusHistory, DeliveryMode, OrderStatus } from '../types/customer.types';
import { cn } from '@/shared/utils/cn';

interface OrderTimelineStepperProps {
  status: OrderStatus;
  deliveryMode: DeliveryMode;
  statusHistory?: CustomerOrderStatusHistory[];
}

interface StepDef {
  key: OrderStatus;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
}

const STEPS_DELIVERY: StepDef[] = [
  {
    key: 'CONFIRMED',
    label: 'Confirmado',
    description: 'Pedido recibido por la tienda',
    icon: CheckCircle2,
  },
  {
    key: 'IN_PREPARATION',
    label: 'En preparación',
    description: 'Empaquetando tus productos',
    icon: Package,
  },
  {
    key: 'READY',
    label: 'Listo',
    description: 'Esperando al repartidor',
    icon: Clock,
  },
  {
    key: 'OUT_FOR_DELIVERY',
    label: 'En camino',
    description: 'Rumbo a tu dirección',
    icon: Truck,
  },
  {
    key: 'COMPLETED',
    label: 'Entregado',
    description: 'Pedido entregado con éxito',
    icon: Check,
  },
];

const STEPS_PICKUP: StepDef[] = [
  {
    key: 'CONFIRMED',
    label: 'Confirmado',
    description: 'Pedido recibido por la tienda',
    icon: CheckCircle2,
  },
  {
    key: 'IN_PREPARATION',
    label: 'En preparación',
    description: 'Alistando tu pedido en tienda',
    icon: Package,
  },
  {
    key: 'READY',
    label: 'Listo para recojo',
    description: 'Puedes pasar por el local',
    icon: Clock,
  },
  {
    key: 'COMPLETED',
    label: 'Recogido',
    description: 'Entregado en tienda',
    icon: Check,
  },
];

const STATUS_PROGRESSION: Record<OrderStatus, number> = {
  CONFIRMED: 0,
  IN_PREPARATION: 1,
  READY: 2,
  OUT_FOR_DELIVERY: 3,
  COMPLETED: 4,
  CANCELLED: -1,
};

export const OrderTimelineStepper: React.FC<OrderTimelineStepperProps> = ({
  status,
  deliveryMode,
  statusHistory = [],
}) => {
  if (status === 'CANCELLED') {
    const cancelEvent = statusHistory.find((h) => h.status === 'CANCELLED');
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50/70 p-4 sm:p-5 text-rose-950">
        <div className="flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h3 className="font-semibold text-sm sm:text-base">Pedido cancelado</h3>
            <p className="text-xs sm:text-sm text-rose-800">
              {cancelEvent?.reason || 'Este pedido fue cancelado y no continuará su preparación.'}
            </p>
            {cancelEvent?.changedAt && (
              <p className="text-xs text-rose-700 tabular-nums">
                Cancelado el {new Date(cancelEvent.changedAt).toLocaleString('es-PE')}
              </p>
            )}
          </div>
        </div>
      </div>
    );
  }

  const steps = deliveryMode === 'PICKUP' ? STEPS_PICKUP : STEPS_DELIVERY;
  const currentStepIndex = Math.min(
    steps.length - 1,
    deliveryMode === 'PICKUP' && status === 'COMPLETED'
      ? 3
      : deliveryMode === 'PICKUP' && status === 'READY'
      ? 2
      : STATUS_PROGRESSION[status] ?? 0
  );

  return (
    <div className="w-full">
      <ol
        aria-label="Progreso del pedido"
        className={cn(
          'grid grid-cols-1 gap-2.5 sm:grid-cols-2',
          steps.length === 4 ? 'lg:grid-cols-4' : 'lg:grid-cols-5'
        )}
      >
        {steps.map((step, idx) => {
          const isCompleted = idx < currentStepIndex;
          const isCurrent = idx === currentStepIndex;
          const StepIcon = step.icon;
          const historyEntry = statusHistory.find((h) => h.status === step.key);

          return (
            <li
              key={step.key}
              aria-current={isCurrent ? 'step' : undefined}
              className={cn(
                'min-w-0 flex lg:flex-col items-start gap-3 lg:gap-2.5 p-3.5 sm:p-4 rounded-xl border transition-colors',
                isCurrent
                  ? 'bg-[#f0fdfa] border-[#005141]'
                  : isCompleted
                  ? 'bg-[#faf7f2] border-[#e7e0d6]'
                  : 'bg-white border-[#e7e0d6]/80'
              )}
            >
              {/* Icon + (Desktop: timestamp on right) */}
              <div className="flex items-center justify-between gap-2 shrink-0 lg:w-full">
                <div
                  className={cn(
                    'flex items-center justify-center w-9 h-9 lg:w-8 lg:h-8 rounded-lg shrink-0',
                    isCurrent
                      ? 'bg-[#005141] text-white'
                      : isCompleted
                      ? 'bg-[#ccfbf1] text-[#005141]'
                      : 'bg-[#f7f3ec] text-[#57534e]'
                  )}
                >
                  <StepIcon className="w-4 h-4" aria-hidden="true" />
                </div>

                {historyEntry && (
                  <span className="hidden lg:inline text-[11px] font-mono tabular-nums text-[#57534e]">
                    {new Date(historyEntry.changedAt).toLocaleTimeString('es-PE', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                )}
              </div>

              {/* Label + Description + (Mobile: timestamp aligned right on label row) */}
              <div className="min-w-0 flex-1 lg:w-full space-y-1">
                <div className="flex items-baseline justify-between gap-2">
                  <span
                    className={cn(
                      'text-sm font-bold leading-snug break-words',
                      isCurrent
                        ? 'text-[#005141]'
                        : isCompleted
                        ? 'text-[#1c1917]'
                        : 'text-[#57534e]'
                    )}
                  >
                    {step.label}
                  </span>

                  {historyEntry && (
                    <span className="lg:hidden text-[11px] font-mono tabular-nums text-[#57534e] shrink-0">
                      {new Date(historyEntry.changedAt).toLocaleTimeString('es-PE', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  )}
                </div>

                <p className="text-xs text-[#57534e] leading-relaxed break-words">
                  {step.description}
                </p>
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
};

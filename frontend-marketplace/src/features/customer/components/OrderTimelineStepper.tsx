import React from 'react';
import { CheckCircle2, Clock, Package, Truck, Check, AlertCircle } from 'lucide-react';
import { CustomerOrderStatusHistory, DeliveryMode, OrderStatus } from '../types/customer.types';

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
            <h4 className="font-semibold text-sm sm:text-base">Pedido cancelado</h4>
            <p className="text-xs sm:text-sm text-rose-800">
              {cancelEvent?.reason || 'Este pedido fue cancelado y no continuará su preparación.'}
            </p>
            {cancelEvent?.changedAt && (
              <p className="text-xs text-rose-600">
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
      <div className="relative">
        <div className="grid grid-cols-1 sm:grid-cols-4 md:grid-cols-5 gap-3 sm:gap-2">
          {steps.map((step, idx) => {
            const isCompleted = idx < currentStepIndex;
            const isCurrent = idx === currentStepIndex;
            const StepIcon = step.icon;

            // Find history entry for this status if available
            const historyEntry = statusHistory.find((h) => h.status === step.key);

            return (
              <div
                key={step.key}
                className={`relative flex flex-col p-3 rounded-xl border transition-all ${
                  isCurrent
                    ? 'bg-emerald-50/60 border-emerald-300 ring-1 ring-emerald-300 shadow-xs'
                    : isCompleted
                    ? 'bg-stone-50/60 border-stone-200'
                    : 'bg-white/40 border-stone-200/60 opacity-60'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <div
                    className={`flex items-center justify-center w-7 h-7 rounded-lg text-xs font-bold shrink-0 ${
                      isCurrent
                        ? 'bg-[#005141] text-white shadow-xs'
                        : isCompleted
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-stone-100 text-stone-400'
                    }`}
                  >
                    <StepIcon className="w-4 h-4" />
                  </div>
                  <span
                    className={`text-xs font-semibold uppercase tracking-wider ${
                      isCurrent
                        ? 'text-[#005141]'
                        : isCompleted
                        ? 'text-stone-800'
                        : 'text-stone-400'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>

                <p className="text-xs text-stone-600 leading-snug line-clamp-2">
                  {step.description}
                </p>

                {historyEntry && (
                  <span className="mt-2 text-[11px] font-mono text-stone-500">
                    {new Date(historyEntry.changedAt).toLocaleTimeString('es-PE', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

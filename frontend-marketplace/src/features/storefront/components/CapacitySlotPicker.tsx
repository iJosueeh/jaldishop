'use client';

import React, { useState } from 'react';
import { Clock, Bike, Store, ShieldCheck, Flame, Check, AlertCircle } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import { motion } from 'framer-motion';

export interface CapacitySlot {
  id: string;
  timeRange: string;
  totalCapacity: number;
  reservedCount: number;
  status: 'available' | 'few_left' | 'sold_out';
  estimatedDeliveryMinutes: number;
}

export const DEFAULT_SLOTS: CapacitySlot[] = [
  {
    id: 'slot-1',
    timeRange: '09:00 - 10:00 AM',
    totalCapacity: 12,
    reservedCount: 12,
    status: 'sold_out',
    estimatedDeliveryMinutes: 25,
  },
  {
    id: 'slot-2',
    timeRange: '10:00 - 11:00 AM',
    totalCapacity: 15,
    reservedCount: 13,
    status: 'few_left',
    estimatedDeliveryMinutes: 30,
  },
  {
    id: 'slot-3',
    timeRange: '11:00 - 12:00 PM',
    totalCapacity: 15,
    reservedCount: 8,
    status: 'available',
    estimatedDeliveryMinutes: 35,
  },
  {
    id: 'slot-4',
    timeRange: '12:00 - 01:00 PM',
    totalCapacity: 10,
    reservedCount: 4,
    status: 'available',
    estimatedDeliveryMinutes: 40,
  },
  {
    id: 'slot-5',
    timeRange: '01:00 - 02:00 PM',
    totalCapacity: 10,
    reservedCount: 9,
    status: 'few_left',
    estimatedDeliveryMinutes: 35,
  },
];

export interface CapacitySlotPickerProps {
  slots?: CapacitySlot[];
  selectedSlotId?: string;
  onSelectSlot?: (slot: CapacitySlot) => void;
  fulfillmentType?: 'DELIVERY' | 'PICKUP';
  onFulfillmentTypeChange?: (type: 'DELIVERY' | 'PICKUP') => void;
}

export function CapacitySlotPicker({
  slots = DEFAULT_SLOTS,
  selectedSlotId: controlledSelectedId,
  onSelectSlot,
  fulfillmentType = 'DELIVERY',
  onFulfillmentTypeChange,
}: CapacitySlotPickerProps) {
  // Find first available slot if not selected
  const defaultSlot = slots.find((s) => s.status !== 'sold_out') || slots[0];
  const [internalSelectedId, setInternalSelectedId] = useState(defaultSlot.id);
  const activeSlotId = controlledSelectedId !== undefined ? controlledSelectedId : internalSelectedId;

  const handleSelect = (slot: CapacitySlot) => {
    if (slot.status === 'sold_out') return;
    setInternalSelectedId(slot.id);
    onSelectSlot?.(slot);
  };

  const selectedSlot = slots.find((s) => s.id === activeSlotId);

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-7 border border-stone-200/90 shadow-xl shadow-stone-900/5 space-y-5">
      {/* Top Header: Title & Guarantee Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-stone-100">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#005141] animate-pulse" />
            <h3 className="text-base sm:text-lg font-black text-[#1c1917] tracking-tight flex items-center gap-2">
              Horario de Entrega / Despacho
            </h3>
          </div>
          <p className="text-xs text-[#57534e] mt-0.5">
            Selecciona la franja horaria para asegurar tu turno de preparación sin retrasos.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="jade" size="sm" className="font-bold flex items-center gap-1 shadow-2xs">
            <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
            Cupo protegido 10 min
          </Badge>
        </div>
      </div>

      {/* Fulfillment Toggle (Delivery vs Pickup) */}
      <div className="flex items-center gap-2 p-1.5 bg-[#faf7f2] rounded-2xl border border-stone-200/80 max-w-sm">
        <button
          type="button"
          onClick={() => onFulfillmentTypeChange?.('DELIVERY')}
          className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
            fulfillmentType === 'DELIVERY'
              ? 'bg-[#005141] text-white shadow-sm'
              : 'text-[#57534e] hover:text-[#1c1917] hover:bg-white/60'
          }`}
        >
          <Bike className="w-3.5 h-3.5" />
          <span>Delivery a Domicilio</span>
        </button>

        <button
          type="button"
          onClick={() => onFulfillmentTypeChange?.('PICKUP')}
          className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
            fulfillmentType === 'PICKUP'
              ? 'bg-[#005141] text-white shadow-sm'
              : 'text-[#57534e] hover:text-[#1c1917] hover:bg-white/60'
          }`}
        >
          <Store className="w-3.5 h-3.5" />
          <span>Retiro en Tienda</span>
        </button>
      </div>

      {/* Horizontal Visual Timeline of Slots */}
      <div>
        <div className="flex items-center justify-between text-xs text-[#57534e] font-bold mb-3">
          <span className="flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-[#ea580c]" /> Franjas de hoy (capacidad en tiempo real)
          </span>
          <span className="text-[11px] text-[#78716c] font-medium hidden sm:inline">
            Al seleccionar se aparta tu lugar
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {slots.map((slot) => {
            const isSelected = activeSlotId === slot.id;
            const isSoldOut = slot.status === 'sold_out';
            const isFewLeft = slot.status === 'few_left';
            const slotsAvailable = slot.totalCapacity - slot.reservedCount;
            const occupancyPct = Math.round((slot.reservedCount / slot.totalCapacity) * 100);

            return (
              <button
                key={slot.id}
                type="button"
                disabled={isSoldOut}
                onClick={() => handleSelect(slot)}
                className={`relative p-3.5 rounded-2xl text-left border transition-all cursor-pointer flex flex-col justify-between min-h-[110px] ${
                  isSoldOut
                    ? 'bg-stone-50 border-stone-200/60 opacity-50 cursor-not-allowed'
                    : isSelected
                    ? 'bg-[#f0fdfa] border-[#005141] ring-2 ring-[#005141]/20 shadow-md scale-[1.02]'
                    : 'bg-white border-stone-200 hover:border-stone-300 hover:bg-stone-50/70 shadow-xs'
                }`}
              >
                {/* Active Checkmark Pill */}
                {isSelected && (
                  <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-[#005141] text-white flex items-center justify-center shadow-xs">
                    <Check className="w-3 h-3 stroke-[3]" />
                  </span>
                )}

                <div className="space-y-1">
                  <div className="text-xs font-black text-[#1c1917] font-mono">
                    {slot.timeRange}
                  </div>

                  <div className="flex items-center gap-1 text-[11px]">
                    {isSoldOut ? (
                      <span className="text-rose-600 font-bold flex items-center gap-1">
                        <AlertCircle className="w-3 h-3" /> Agotado
                      </span>
                    ) : isFewLeft ? (
                      <span className="text-[#ea580c] font-black flex items-center gap-1">
                        <Flame className="w-3 h-3" /> ¡Último {slotsAvailable}!
                      </span>
                    ) : (
                      <span className="text-[#005141] font-bold">
                        {slotsAvailable} cupos libres
                      </span>
                    )}
                  </div>
                </div>

                {/* Progress bar gauge */}
                <div className="pt-2 w-full space-y-1">
                  <div className="w-full h-1.5 bg-stone-100 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        isSoldOut
                          ? 'bg-rose-400'
                          : isFewLeft
                          ? 'bg-gradient-to-r from-[#ea580c] to-[#feae2c]'
                          : 'bg-[#005141]'
                      }`}
                      style={{ width: `${occupancyPct}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between text-[9px] font-bold text-[#78716c]">
                    <span>{occupancyPct}% lleno</span>
                    <span>~{slot.estimatedDeliveryMinutes}m</span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Selected Slot Information Bar */}
      {selectedSlot && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-3 bg-[#faf7f2] rounded-2xl border border-stone-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
        >
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#005141]" />
            <span className="text-[#1c1917] font-bold">
              Franja seleccionada: <strong className="font-mono text-[#005141]">{selectedSlot.timeRange}</strong>
            </span>
            <span className="text-[#78716c] hidden sm:inline">•</span>
            <span className="text-[#57534e]">
              Modalidad: <strong>{fulfillmentType === 'DELIVERY' ? 'Envío a Domicilio' : 'Retiro en Tienda'}</strong>
            </span>
          </div>

          <div className="text-[11px] text-[#005141] font-bold flex items-center gap-1.5 self-start sm:self-auto">
            <Clock className="w-3.5 h-3.5 text-[#005141]" />
            <span>10 minutos de gracia para pagar al confirmar</span>
          </div>
        </motion.div>
      )}
    </div>
  );
}

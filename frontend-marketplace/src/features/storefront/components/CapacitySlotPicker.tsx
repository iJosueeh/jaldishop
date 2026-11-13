'use client';

import React, { useState } from 'react';
import { Clock, Bike, Store, ShieldCheck, Flame, Check, AlertCircle, Calendar } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import { motion } from 'framer-motion';

export interface CapacitySlot {
  id: string;
  timeRange: string;
  totalCapacity: number;
  reservedCount: number;
  status: 'available' | 'few_left' | 'sold_out';
  estimatedDeliveryMinutes?: number;
  startTime?: string;
  endTime?: string;
  serviceDate?: string;
  availableCapacity?: number;
}

export interface CapacitySlotPickerProps {
  slots?: CapacitySlot[];
  selectedSlotId?: string | null;
  onSelectSlot?: (slot: CapacitySlot) => void;
  fulfillmentType?: 'DELIVERY' | 'PICKUP';
  onFulfillmentTypeChange?: (type: 'DELIVERY' | 'PICKUP') => void;
  selectedDate?: string;
  onDateChange?: (date: string) => void;
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  minDate?: string;
}

export function CapacitySlotPicker({
  slots = [],
  selectedSlotId: controlledSelectedId,
  onSelectSlot,
  fulfillmentType = 'DELIVERY',
  onFulfillmentTypeChange,
  selectedDate,
  onDateChange,
  isLoading = false,
  error = null,
  onRetry,
  minDate,
}: CapacitySlotPickerProps) {
  // Controlled vs uncontrolled: when controlledSelectedId is passed (even as null), respect it strictly without auto-selecting
  const defaultSlot = controlledSelectedId !== undefined ? null : (slots.find((s) => s.status !== 'sold_out') || slots[0]);
  const [internalSelectedId, setInternalSelectedId] = useState(defaultSlot?.id);
  const activeSlotId = controlledSelectedId !== undefined ? (controlledSelectedId ?? undefined) : internalSelectedId;

  const handleSelect = (slot: CapacitySlot) => {
    if (slot.status === 'sold_out') return;
    setInternalSelectedId(slot.id);
    onSelectSlot?.(slot);
  };

  const selectedSlot = slots.find((s) => s.id === activeSlotId);

  if (slots.length === 0 && !isLoading && !error && !onDateChange) {
    return (
      <section className="rounded-[12px] border border-stone-200/90 bg-white p-6 shadow-2xs">
        <h3 className="font-semibold text-stone-900">No hay horarios disponibles</h3>
        <p className="mt-2 text-sm text-stone-600">
          Esta tienda no tiene horarios publicados para seleccionar. No se ha reservado ningún cupo.
        </p>
      </section>
    );
  }

  const allSoldOut = slots.length > 0 && slots.every((s) => s.status === 'sold_out');

  return (
    <div className="bg-white rounded-[12px] p-5 sm:p-7 border border-stone-200/90 shadow-sm space-y-5">
      {/* Top Header: Title & Guarantee Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-stone-100">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#005141] animate-pulse" />
            <h3 className="text-base sm:text-lg font-black text-stone-900 tracking-tight flex items-center gap-2">
              Horario de Entrega / Despacho
            </h3>
          </div>
          <p className="text-xs text-stone-600 mt-0.5">
            Selecciona la franja horaria para asegurar tu turno de preparación sin retrasos.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="jade" size="sm" className="font-bold flex items-center gap-1 shadow-2xs rounded-full px-3 py-1">
            <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
            Elige tu horario
          </Badge>
        </div>
      </div>

      {/* Fulfillment Toggle & Date Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Fulfillment Toggle (Delivery vs Pickup) */}
        <div className="flex items-center gap-1 p-1 bg-stone-100/80 rounded-full border border-stone-200/80 max-w-sm">
          <button
            type="button"
            onClick={() => onFulfillmentTypeChange?.('DELIVERY')}
            className={`flex-1 py-2 px-3.5 rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              fulfillmentType === 'DELIVERY'
                ? 'bg-[#005141] text-white shadow-xs'
                : 'text-stone-600 hover:text-stone-900 hover:bg-white/60'
            }`}
          >
            <Bike className="w-3.5 h-3.5" />
            <span>Delivery a Domicilio</span>
          </button>

          <button
            type="button"
            onClick={() => onFulfillmentTypeChange?.('PICKUP')}
            className={`flex-1 py-2 px-3.5 rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              fulfillmentType === 'PICKUP'
                ? 'bg-[#005141] text-white shadow-xs'
                : 'text-stone-600 hover:text-stone-900 hover:bg-white/60'
            }`}
          >
            <Store className="w-3.5 h-3.5" />
            <span>Retiro en Tienda</span>
          </button>
        </div>

        {/* Date Selector */}
        {onDateChange && selectedDate && (
          <div className="flex items-center gap-2">
            <label htmlFor="capacity-date-picker" className="text-xs font-bold text-stone-700 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-[#005141]" />
              <span className="hidden sm:inline">Fecha:</span>
            </label>
            <input
              id="capacity-date-picker"
              type="date"
              min={minDate}
              value={selectedDate}
              onChange={(e) => onDateChange(e.target.value)}
              className="text-xs font-semibold px-3 py-1.5 rounded-full border border-stone-200/90 bg-stone-50/60 text-stone-800 focus:outline-none focus:ring-2 focus:ring-[#005141]/20 focus:border-[#005141] cursor-pointer"
            />
          </div>
        )}
      </div>

      {/* Horizontal Visual Timeline of Slots */}
      <div>
        <div className="flex items-center justify-between text-xs text-stone-600 font-bold mb-3">
          <span className="flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-[#ea580c]" /> Franjas del día (capacidad en tiempo real)
          </span>
          <span className="text-[11px] text-stone-500 font-medium hidden sm:inline">
            Seleccionar no reserva un cupo
          </span>
        </div>

        {isLoading ? (
          <div role="status" className="p-8 text-center bg-stone-50/60 rounded-[12px] border border-dashed border-stone-300 text-stone-600 text-sm animate-pulse space-y-2">
            <Clock className="w-6 h-6 mx-auto text-[#005141] animate-spin" />
            <p className="font-medium text-xs">Consultando disponibilidad de horarios en tiempo real...</p>
          </div>
        ) : error ? (
          <div className="p-4 bg-amber-50 rounded-[12px] border border-amber-200 text-amber-800 text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <span>{error}</span>
            {onRetry && (
              <button
                type="button"
                onClick={onRetry}
                className="px-3.5 py-1.5 rounded-full bg-[#005141] text-white text-xs font-bold hover:bg-[#00382d] transition-colors cursor-pointer shrink-0"
              >
                Reintentar
              </button>
            )}
          </div>
        ) : slots.length === 0 ? (
          <div className="p-6 text-center bg-stone-50 rounded-[12px] border border-stone-200 text-stone-600 text-sm">
            <p className="font-semibold text-stone-800">No hay horarios disponibles para esta fecha</p>
            <p className="text-xs mt-1 text-stone-500">Selecciona otra fecha o consúltanos directamente por WhatsApp.</p>
          </div>
        ) : (
          <>
            {allSoldOut && (
              <div className="mb-3 p-3 bg-rose-50 rounded-[10px] border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>Todos los horarios para esta fecha están agotados. Selecciona otra fecha para continuar.</span>
              </div>
            )}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
              {slots.map((slot) => {
                const isSelected = activeSlotId === slot.id;
                const isSoldOut = slot.status === 'sold_out';
                const isFewLeft = slot.status === 'few_left';
                const slotsAvailable = slot.availableCapacity !== undefined
                  ? slot.availableCapacity
                  : slot.totalCapacity - slot.reservedCount;
                const occupancyPct = slot.totalCapacity > 0
                  ? Math.min(100, Math.round((slot.reservedCount / slot.totalCapacity) * 100))
                  : 100;

                return (
                  <button
                    key={slot.id}
                    type="button"
                    disabled={isSoldOut}
                    onClick={() => handleSelect(slot)}
                    className={`relative p-3.5 rounded-[12px] text-left border transition-all cursor-pointer flex flex-col justify-between min-h-[110px] ${
                      isSoldOut
                        ? 'bg-stone-50/70 border-stone-200/60 opacity-50 cursor-not-allowed'
                        : isSelected
                        ? 'bg-emerald-50/60 border-[#005141] ring-2 ring-[#005141]/20 shadow-xs scale-[1.01]'
                        : 'bg-white border-stone-200/90 hover:border-stone-300 hover:bg-stone-50/60 shadow-2xs'
                    }`}
                  >
                    {/* Active Checkmark Pill */}
                    {isSelected && (
                      <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-[#005141] text-white flex items-center justify-center shadow-xs">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </span>
                    )}

                    <div className="space-y-1">
                      <div className="text-xs font-bold text-stone-900 font-mono">
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
                      <div className="flex items-center justify-between text-[9px] font-bold text-stone-500">
                        <span>{occupancyPct}% lleno</span>
                        {slot.estimatedDeliveryMinutes != null && (
                          <span>~{slot.estimatedDeliveryMinutes}m</span>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </>
        )}
      </div>

      {/* Selected Slot Information Bar */}
      {selectedSlot && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-3 bg-stone-50/80 rounded-[12px] border border-stone-200/90 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
        >
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#005141]" />
            <span className="text-stone-900 font-bold">
              Franja seleccionada: <strong className="font-mono text-[#005141]">{selectedSlot.timeRange}</strong>
            </span>
            <span className="text-stone-300 hidden sm:inline">•</span>
            <span className="text-stone-600">
              Modalidad: <strong>{fulfillmentType === 'DELIVERY' ? 'Envío a Domicilio' : 'Retiro en Tienda'}</strong>
            </span>
          </div>

          <div className="text-[11px] text-[#005141] font-bold flex items-center gap-1.5 self-start sm:self-auto">
            <Clock className="w-3.5 h-3.5 text-[#005141]" />
            <span>Horario elegido; sin reserva confirmada</span>
          </div>
        </motion.div>
      )}
    </div>
  );
}

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
  status: 'available' | 'few_left' | 'sold_out' | 'expired';
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
  deliveryEnabled?: boolean;
  pickupEnabled?: boolean;
  embedded?: boolean;
  hasActiveReservation?: boolean;
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
  deliveryEnabled = true,
  pickupEnabled = true,
  embedded = false,
  hasActiveReservation = false,
}: CapacitySlotPickerProps) {
  // Controlled vs uncontrolled: when controlledSelectedId is passed (even as null), respect it strictly without auto-selecting
  const defaultSlot = controlledSelectedId !== undefined ? null : (slots.find((s) => s.status !== 'sold_out') || slots[0]);
  const [internalSelectedId, setInternalSelectedId] = useState(defaultSlot?.id);
  const activeSlotId = controlledSelectedId !== undefined ? (controlledSelectedId ?? undefined) : internalSelectedId;

  const handleSelect = (slot: CapacitySlot) => {
    if (slot.status === 'sold_out' || slot.status === 'expired') return;
    setInternalSelectedId(slot.id);
    onSelectSlot?.(slot);
  };

  const selectedSlot = slots.find((s) => s.id === activeSlotId);

  if (slots.length === 0 && !isLoading && !error && !onDateChange) {
    return (
      <section className={embedded ? 'p-5 sm:p-7 text-[#1c1917]' : 'rounded-[12px] border border-[#e7e0d6] bg-white p-6 text-[#1c1917]'}>
        <h3 className="font-display font-semibold text-[#1c1917]">No hay horarios disponibles</h3>
        <p className="mt-2 text-sm text-[#57534e]">
          Esta tienda no tiene horarios publicados para seleccionar. No se ha reservado ningún cupo.
        </p>
      </section>
    );
  }

  const allSoldOut = slots.length > 0 && slots.every((s) => s.status === 'sold_out' || s.status === 'expired');

  return (
    <div className={embedded ? 'p-5 sm:p-7 text-[#1c1917] space-y-5' : 'bg-white rounded-[12px] p-5 sm:p-7 border border-[#e7e0d6] text-[#1c1917] space-y-5'}>
      {/* Top Header: Title & Guarantee Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-[#e7e0d6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#005141]" />
            <h3 className="font-display text-base sm:text-lg font-semibold text-[#1c1917] tracking-tight flex items-center gap-2">
              Horario de Entrega / Despacho
            </h3>
          </div>
          <p className="text-xs text-[#57534e] mt-0.5">
            Selecciona la franja horaria para asegurar tu turno de preparación sin retrasos.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="jade" size="sm" className="font-semibold flex items-center gap-1 rounded-full px-3 py-1 bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]">
            <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
            Elige tu horario
          </Badge>
        </div>
      </div>

      {/* Fulfillment Toggle & Date Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Fulfillment Toggle (Delivery vs Pickup) */}
        <div className="flex items-center gap-1 p-1 bg-[#f7f3ec] rounded-full border border-[#e7e0d6] max-w-sm">
          <button
            type="button"
            onClick={() => onFulfillmentTypeChange?.('DELIVERY')}
            disabled={!deliveryEnabled}
            title={!deliveryEnabled ? 'Delivery no habilitado por este comercio' : undefined}
            className={`flex-1 py-2 px-3.5 rounded-full text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              !deliveryEnabled
                ? 'opacity-40 cursor-not-allowed text-[#78716c] pointer-events-none'
                : fulfillmentType === 'DELIVERY'
                ? 'bg-[#005141] text-white'
                : 'text-[#57534e] hover:text-[#1c1917]'
            }`}
          >
            <Bike className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Delivery a Domicilio</span>
          </button>

          <button
            type="button"
            onClick={() => onFulfillmentTypeChange?.('PICKUP')}
            disabled={!pickupEnabled}
            title={!pickupEnabled ? 'Retiro en tienda no habilitado por este comercio' : undefined}
            className={`flex-1 py-2 px-3.5 rounded-full text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              !pickupEnabled
                ? 'opacity-40 cursor-not-allowed text-[#78716c] pointer-events-none'
                : fulfillmentType === 'PICKUP'
                ? 'bg-[#005141] text-white'
                : 'text-[#57534e] hover:text-[#1c1917]'
            }`}
          >
            <Store className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Retiro en Tienda</span>
          </button>
        </div>

        {/* Date Selector */}
        {onDateChange && selectedDate && (
          <div className="flex items-center gap-2">
            <label htmlFor="capacity-date-picker" className="text-xs font-medium text-[#57534e] flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
              <span className="hidden sm:inline">Fecha:</span>
            </label>
            <input
              id="capacity-date-picker"
              type="date"
              min={minDate}
              value={selectedDate}
              onChange={(e) => onDateChange(e.target.value)}
              className="text-xs font-semibold px-3 py-1.5 rounded-[8px] border border-[#e7e0d6] bg-[#faf7f2] text-[#1c1917] focus:outline-none focus:bg-white focus:border-[#005141] cursor-pointer"
            />
          </div>
        )}
      </div>

      {/* Horizontal Visual Timeline of Slots */}
      <div>
        <div className="flex items-center justify-between text-xs text-[#57534e] font-medium mb-3">
          <span className="flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" /> Franjas del día (capacidad en tiempo real)
          </span>
          <span className="text-[11px] text-[#78716c] hidden sm:inline">
            Seleccionar no reserva un cupo
          </span>
        </div>

        {isLoading ? (
          <div role="status" className="p-8 text-center bg-[#faf7f2] rounded-[12px] border border-dashed border-[#e7e0d6] text-[#57534e] text-sm animate-pulse space-y-2">
            <Clock className="w-6 h-6 mx-auto text-[#005141] animate-spin" />
            <p className="font-medium text-xs">Consultando disponibilidad de horarios en tiempo real...</p>
          </div>
        ) : error ? (
          <div className="p-4 bg-amber-50 rounded-[12px] border border-amber-200 text-amber-900 text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <span>{error}</span>
            {onRetry && (
              <button
                type="button"
                onClick={onRetry}
                className="px-3.5 py-1.5 rounded-full bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors cursor-pointer shrink-0"
              >
                Reintentar
              </button>
            )}
          </div>
        ) : slots.length === 0 ? (
          <div className="p-6 text-center bg-[#faf7f2] rounded-[12px] border border-[#e7e0d6] text-[#57534e] text-sm">
            <p className="font-semibold text-[#1c1917]">No hay horarios disponibles para esta fecha</p>
            <p className="text-xs mt-1 text-[#78716c]">Selecciona otra fecha o consúltanos directamente por WhatsApp.</p>
          </div>
        ) : (
          <>
            {allSoldOut && (
              <div className="mb-3 p-3 bg-rose-50 rounded-[10px] border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" aria-hidden="true" />
                <span>Todos los horarios para esta fecha están agotados. Selecciona otra fecha para continuar.</span>
              </div>
            )}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-3">
              {slots.map((slot) => {
                const isSelected = activeSlotId === slot.id;
                const isExpired = slot.status === 'expired';
                const isSoldOut = slot.status === 'sold_out';
                const isDisabled = isSoldOut || isExpired;
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
                    disabled={isDisabled}
                    aria-pressed={isSelected}
                    onClick={() => handleSelect(slot)}
                    className={`relative p-3.5 rounded-[12px] text-left border transition-all cursor-pointer flex flex-col justify-between min-h-[110px] ${
                      isDisabled
                        ? 'bg-[#f7f3ec]/70 border-[#e7e0d6] opacity-55 cursor-not-allowed'
                        : isSelected
                        ? 'bg-[#f0fdfa] border-[#005141] ring-1 ring-[#005141]/25'
                        : 'bg-white border-[#e7e0d6] hover:border-[#005141]/50 hover:bg-[#faf7f2]'
                    }`}
                  >
                    {/* Active Checkmark Pill */}
                    {isSelected && (
                      <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-[#005141] text-white flex items-center justify-center">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </span>
                    )}

                    <div className="space-y-1">
                      <div className="text-xs font-bold text-[#1c1917] font-mono tabular-nums">
                        {slot.timeRange}
                      </div>

                      <div className="flex items-center gap-1 text-[11px]">
                        {isExpired ? (
                          <span className="text-[#78716c] font-semibold flex items-center gap-1">
                            <Clock className="w-3 h-3" /> Finalizado
                          </span>
                        ) : isSoldOut ? (
                          <span className="text-rose-700 font-semibold flex items-center gap-1">
                            <AlertCircle className="w-3 h-3" /> Agotado
                          </span>
                        ) : isFewLeft ? (
                          <span className="text-amber-700 font-semibold flex items-center gap-1">
                            <Flame className="w-3 h-3" /> ¡Último {slotsAvailable}!
                          </span>
                        ) : (
                          <span className="text-[#005141] font-semibold">
                            {slotsAvailable} cupos libres
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Progress bar gauge */}
                    <div className="pt-2 w-full space-y-1">
                      <div className="w-full h-1.5 bg-[#e7e0d6] rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-300 ${
                            isExpired
                              ? 'bg-[#78716c]'
                              : isSoldOut
                              ? 'bg-rose-500'
                              : isFewLeft
                              ? 'bg-amber-500'
                              : 'bg-[#005141]'
                          }`}
                          style={{ width: `${occupancyPct}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[10px] font-medium text-[#78716c] tabular-nums">
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
          className="p-3 bg-[#f0fdfa] rounded-[10px] border border-[#ccfbf1] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
        >
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#005141]" />
            <span className="text-[#1c1917] font-medium">
              Franja seleccionada: <strong className="font-mono text-[#005141] tabular-nums">{selectedSlot.timeRange}</strong>
            </span>
            <span className="text-[#e7e0d6] hidden sm:inline">•</span>
            <span className="text-[#57534e]">
              Modalidad: <strong className="text-[#1c1917]">{fulfillmentType === 'DELIVERY' ? 'Envío a Domicilio' : 'Retiro en Tienda'}</strong>
            </span>
          </div>

          <div className="text-[11px] text-[#005141] font-medium flex items-center gap-1.5 self-start sm:self-auto">
            {hasActiveReservation ? (
              <>
                <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
                <span>Reserva de horario activa</span>
              </>
            ) : (
              <>
                <Clock className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
                <span>Horario elegido; sin reserva confirmada</span>
              </>
            )}
          </div>
        </motion.div>
      )}
    </div>
  );
}

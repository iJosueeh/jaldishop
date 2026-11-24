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
  deliveryEnabled?: boolean;
  pickupEnabled?: boolean;
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
      <section className="rounded-[12px] border border-[#1e2c31] bg-[#061a1c] p-6 text-white">
        <h3 className="font-medium text-white">No hay horarios disponibles</h3>
        <p className="mt-2 text-sm text-[#99b3ad]">
          Esta tienda no tiene horarios publicados para seleccionar. No se ha reservado ningún cupo.
        </p>
      </section>
    );
  }

  const allSoldOut = slots.length > 0 && slots.every((s) => s.status === 'sold_out');

  return (
    <div className="bg-[#061a1c] rounded-[12px] p-5 sm:p-7 border border-[#1e2c31] text-white space-y-5">
      {/* Top Header: Title & Guarantee Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-[#1e2c31]">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#36f4a4] animate-pulse" />
            <h3 className="text-base sm:text-lg font-medium text-white tracking-tight flex items-center gap-2">
              Horario de Entrega / Despacho
            </h3>
          </div>
          <p className="text-xs text-[#99b3ad] mt-0.5">
            Selecciona la franja horaria para asegurar tu turno de preparación sin retrasos.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="jade" size="sm" className="font-semibold flex items-center gap-1 rounded-full px-3 py-1 bg-[#072720] text-[#36f4a4] border border-[#133b32]">
            <ShieldCheck className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
            Elige tu horario
          </Badge>
        </div>
      </div>

      {/* Fulfillment Toggle & Date Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Fulfillment Toggle (Delivery vs Pickup) */}
        <div className="flex items-center gap-1 p-1 bg-[#02090a] rounded-full border border-[#1e2c31] max-w-sm">
          <button
            type="button"
            onClick={() => onFulfillmentTypeChange?.('DELIVERY')}
            disabled={!deliveryEnabled}
            title={!deliveryEnabled ? 'Delivery no habilitado por este comercio' : undefined}
            className={`flex-1 py-2 px-3.5 rounded-full text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              !deliveryEnabled
                ? 'opacity-40 cursor-not-allowed text-[#71717a] pointer-events-none'
                : fulfillmentType === 'DELIVERY'
                ? 'bg-white text-[#02090a]'
                : 'text-[#99b3ad] hover:text-white'
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
                ? 'opacity-40 cursor-not-allowed text-[#71717a] pointer-events-none'
                : fulfillmentType === 'PICKUP'
                ? 'bg-white text-[#02090a]'
                : 'text-[#99b3ad] hover:text-white'
            }`}
          >
            <Store className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Retiro en Tienda</span>
          </button>
        </div>

        {/* Date Selector */}
        {onDateChange && selectedDate && (
          <div className="flex items-center gap-2">
            <label htmlFor="capacity-date-picker" className="text-xs font-medium text-[#99b3ad] flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
              <span className="hidden sm:inline">Fecha:</span>
            </label>
            <input
              id="capacity-date-picker"
              type="date"
              min={minDate}
              value={selectedDate}
              onChange={(e) => onDateChange(e.target.value)}
              className="text-xs font-semibold px-3 py-1.5 rounded-[4px] border border-[#1e2c31] bg-[#02090a] text-white focus:outline-none focus:border-[#36f4a4] cursor-pointer"
            />
          </div>
        )}
      </div>

      {/* Horizontal Visual Timeline of Slots */}
      <div>
        <div className="flex items-center justify-between text-xs text-[#99b3ad] font-medium mb-3">
          <span className="flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" /> Franjas del día (capacidad en tiempo real)
          </span>
          <span className="text-[11px] text-[#9dabad] hidden sm:inline">
            Seleccionar no reserva un cupo
          </span>
        </div>

        {isLoading ? (
          <div role="status" className="p-8 text-center bg-[#02090a] rounded-[12px] border border-dashed border-[#1e2c31] text-[#99b3ad] text-sm animate-pulse space-y-2">
            <Clock className="w-6 h-6 mx-auto text-[#36f4a4] animate-spin" />
            <p className="font-medium text-xs">Consultando disponibilidad de horarios en tiempo real...</p>
          </div>
        ) : error ? (
          <div className="p-4 bg-amber-950/40 rounded-[12px] border border-amber-500/40 text-amber-200 text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <span>{error}</span>
            {onRetry && (
              <button
                type="button"
                onClick={onRetry}
                className="px-3.5 py-1.5 rounded-full bg-white text-[#02090a] text-xs font-semibold hover:bg-stone-100 transition-colors cursor-pointer shrink-0"
              >
                Reintentar
              </button>
            )}
          </div>
        ) : slots.length === 0 ? (
          <div className="p-6 text-center bg-[#02090a] rounded-[12px] border border-[#1e2c31] text-[#99b3ad] text-sm">
            <p className="font-semibold text-white">No hay horarios disponibles para esta fecha</p>
            <p className="text-xs mt-1 text-[#9dabad]">Selecciona otra fecha o consúltanos directamente por WhatsApp.</p>
          </div>
        ) : (
          <>
            {allSoldOut && (
              <div className="mb-3 p-3 bg-rose-950/40 rounded-[12px] border border-rose-500/40 text-rose-200 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" aria-hidden="true" />
                <span>Todos los horarios para esta fecha están agotados. Selecciona otra fecha para continuar.</span>
              </div>
            )}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-3">
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
                    aria-pressed={isSelected}
                    onClick={() => handleSelect(slot)}
                    className={`relative p-3.5 rounded-[12px] text-left border transition-all cursor-pointer flex flex-col justify-between min-h-[110px] ${
                      isSoldOut
                        ? 'bg-[#02090a]/60 border-[#1e2c31]/60 opacity-45 cursor-not-allowed'
                        : isSelected
                        ? 'bg-[#072720] border-[#36f4a4] ring-1 ring-[#36f4a4]/40'
                        : 'bg-[#02090a] border-[#1e2c31] hover:border-white/30'
                    }`}
                  >
                    {/* Active Checkmark Pill */}
                    {isSelected && (
                      <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-[#36f4a4] text-[#02090a] flex items-center justify-center shadow-xs">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </span>
                    )}

                    <div className="space-y-1">
                      <div className="text-xs font-bold text-white font-mono">
                        {slot.timeRange}
                      </div>

                      <div className="flex items-center gap-1 text-[11px]">
                        {isSoldOut ? (
                          <span className="text-rose-400 font-semibold flex items-center gap-1">
                            <AlertCircle className="w-3 h-3" /> Agotado
                          </span>
                        ) : isFewLeft ? (
                          <span className="text-amber-400 font-semibold flex items-center gap-1">
                            <Flame className="w-3 h-3" /> ¡Último {slotsAvailable}!
                          </span>
                        ) : (
                          <span className="text-[#36f4a4] font-semibold">
                            {slotsAvailable} cupos libres
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Progress bar gauge */}
                    <div className="pt-2 w-full space-y-1">
                      <div className="w-full h-1.5 bg-[#1e2c31] rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-300 ${
                            isSoldOut
                              ? 'bg-rose-400'
                              : isFewLeft
                              ? 'bg-amber-400'
                              : 'bg-[#36f4a4]'
                          }`}
                          style={{ width: `${occupancyPct}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[9px] font-medium text-[#9dabad]">
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
          className="p-3 bg-[#072720] rounded-[12px] border border-[#133b32] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
        >
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#36f4a4]" />
            <span className="text-white font-medium">
              Franja seleccionada: <strong className="font-mono text-[#36f4a4]">{selectedSlot.timeRange}</strong>
            </span>
            <span className="text-[#1e2c31] hidden sm:inline">•</span>
            <span className="text-[#99b3ad]">
              Modalidad: <strong className="text-white">{fulfillmentType === 'DELIVERY' ? 'Envío a Domicilio' : 'Retiro en Tienda'}</strong>
            </span>
          </div>

          <div className="text-[11px] text-[#36f4a4] font-medium flex items-center gap-1.5 self-start sm:self-auto">
            <Clock className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
            <span>Horario elegido; sin reserva confirmada</span>
          </div>
        </motion.div>
      )}
    </div>
  );
}

'use client';

import React, { useState } from 'react';
import { Clock, CheckCircle2, ShieldAlert, Sparkles, Store, Bike, ShoppingBag, ArrowRight, Utensils, Cake, RefreshCw } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';

interface BusinessPreset {
  id: string;
  name: string;
  category: string;
  icon: React.ReactNode;
  productSample: string;
  priceSample: number;
  slots: Array<{ time: string; capacity: number; reserved: number; available: boolean }>;
}

const PRESETS: BusinessPreset[] = [
  {
    id: 'bakery',
    name: 'Panadería Don Pepe',
    category: 'Masa Madre & Horno Matutino',
    icon: <Utensils className="w-4 h-4 text-[#ea580c]" />,
    productSample: '2x Pan Campesino + 1x Croissant',
    priceSample: 22.5,
    slots: [
      { time: '08:00 - 09:00 AM', capacity: 15, reserved: 15, available: false },
      { time: '09:00 - 10:00 AM', capacity: 20, reserved: 16, available: true },
      { time: '10:00 - 11:00 AM', capacity: 15, reserved: 8, available: true },
      { time: '11:00 - 12:00 PM', capacity: 10, reserved: 10, available: false },
    ],
  },
  {
    id: 'pastry',
    name: 'Dulce Amor Repostería',
    category: 'Tortas & Repostería Fina',
    icon: <Cake className="w-4 h-4 text-pink-600" />,
    productSample: '1x Torta Selva Negra Mediana',
    priceSample: 45.0,
    slots: [
      { time: '02:00 - 03:00 PM', capacity: 5, reserved: 5, available: false },
      { time: '03:00 - 04:00 PM', capacity: 6, reserved: 4, available: true },
      { time: '04:00 - 05:00 PM', capacity: 8, reserved: 2, available: true },
      { time: '05:00 - 06:00 PM', capacity: 4, reserved: 4, available: false },
    ],
  },
  {
    id: 'dark-kitchen',
    name: 'Tokyo Dark Kitchen',
    category: 'Ramen & Sushi Rolls',
    icon: <Sparkles className="w-4 h-4 text-[#005141]" />,
    productSample: '1x Tonkotsu Ramen + 1x Gyoza',
    priceSample: 38.0,
    slots: [
      { time: '12:30 - 01:15 PM', capacity: 12, reserved: 12, available: false },
      { time: '01:15 - 02:00 PM', capacity: 14, reserved: 11, available: true },
      { time: '02:00 - 02:45 PM', capacity: 10, reserved: 4, available: true },
      { time: '02:45 - 03:30 PM', capacity: 8, reserved: 8, available: false },
    ],
  },
];

export function LiveDemoWidget() {
  const [activePresetId, setActivePresetId] = useState('bakery');
  const [fulfillmentType, setFulfillmentType] = useState<'PICKUP' | 'DELIVERY'>('DELIVERY');
  const [selectedSlotTime, setSelectedSlotTime] = useState<string>('09:00 - 10:00 AM');
  const [isReserved, setIsReserved] = useState(false);
  const [countdown, setCountdown] = useState(600); // 10 minutes hold

  const activePreset = PRESETS.find((p) => p.id === activePresetId) || PRESETS[0];

  const handleSelectPreset = (id: string) => {
    setActivePresetId(id);
    const preset = PRESETS.find((p) => p.id === id);
    if (preset) {
      const firstAvailable = preset.slots.find((s) => s.available);
      if (firstAvailable) setSelectedSlotTime(firstAvailable.time);
    }
    setIsReserved(false);
  };

  const handleSimulateReservation = () => {
    setIsReserved(true);
    setCountdown(599);
  };

  const minutes = Math.floor(countdown / 60);
  const seconds = countdown % 60;

  return (
    <div className="relative w-full max-w-lg mx-auto bg-white rounded-3xl p-6 sm:p-7 shadow-2xl shadow-[#1c1917]/10 border-2 border-[#e7e0d6] text-[#1c1917]">
      {/* Decorative Warm Tag Header */}
      <div className="flex items-center justify-between pb-4 border-b border-[#e7e0d6]">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-[#005141] animate-pulse" />
          <span className="text-xs font-bold text-[#005141] uppercase tracking-wider">
            Simulador Interactivo de Capacidad
          </span>
        </div>
        <Badge variant="amber" size="sm">
          Hold 10 min
        </Badge>
      </div>

      {/* Business Type Quick Switcher */}
      <div className="mt-4">
        <label className="text-[11px] font-bold text-[#57534e] uppercase tracking-wider block mb-2">
          Pruébalo en tu rubro:
        </label>
        <div className="grid grid-cols-3 gap-1.5 p-1 bg-[#faf7f2] rounded-2xl border border-[#e7e0d6]">
          {PRESETS.map((preset) => {
            const isSelected = activePresetId === preset.id;
            return (
              <button
                key={preset.id}
                onClick={() => handleSelectPreset(preset.id)}
                className={`py-2 px-1 rounded-xl text-xs font-bold flex flex-col items-center justify-center gap-1 transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-white text-[#1c1917] shadow-sm border border-[#e7e0d6]'
                    : 'text-[#57534e] hover:text-[#1c1917]'
                }`}
              >
                {preset.icon}
                <span className="truncate w-full text-center text-[11px]">{preset.name.split(' ')[0]}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Store Header Info */}
      <div className="mt-4 p-3 bg-[#faf7f2] rounded-2xl border border-[#e7e0d6] flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-[#005141] text-white flex items-center justify-center font-bold text-xs shadow-xs">
            {activePreset.name.substring(0, 2).toUpperCase()}
          </div>
          <div>
            <h4 className="text-xs font-bold text-[#1c1917]">{activePreset.name}</h4>
            <p className="text-[11px] text-[#57534e]">{activePreset.category}</p>
          </div>
        </div>
        <Badge variant="jade" size="sm">
          En Línea
        </Badge>
      </div>

      {/* Fulfillment Switch */}
      <div className="mt-4 p-1 bg-[#f7f3ec] rounded-2xl flex gap-1 border border-[#e7e0d6]">
        <button
          onClick={() => setFulfillmentType('DELIVERY')}
          className={`flex-1 py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
            fulfillmentType === 'DELIVERY'
              ? 'bg-[#005141] text-white shadow-xs'
              : 'text-[#57534e] hover:text-[#1c1917]'
          }`}
        >
          <Bike className="w-3.5 h-3.5" />
          Delivery
        </button>
        <button
          onClick={() => setFulfillmentType('PICKUP')}
          className={`flex-1 py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
            fulfillmentType === 'PICKUP'
              ? 'bg-[#005141] text-white shadow-xs'
              : 'text-[#57534e] hover:text-[#1c1917]'
          }`}
        >
          <Store className="w-3.5 h-3.5" />
          Retiro en tienda
        </button>
      </div>

      {/* Slots selection */}
      <div className="mt-4 space-y-2">
        <div className="flex items-center justify-between text-xs text-[#57534e] mb-1">
          <span className="font-semibold">Franjas horarias disponibles:</span>
          <span className="text-[#005141] font-bold">Capacidad en vivo</span>
        </div>

        {activePreset.slots.map((slot) => {
          const isSelected = selectedSlotTime === slot.time;
          const slotsLeft = slot.capacity - slot.reserved;

          return (
            <button
              key={slot.time}
              disabled={!slot.available}
              onClick={() => setSelectedSlotTime(slot.time)}
              className={`w-full p-2.5 rounded-2xl text-left border-2 flex items-center justify-between transition-all cursor-pointer ${
                !slot.available
                  ? 'bg-[#f7f3ec]/60 border-[#e7e0d6]/60 opacity-50 cursor-not-allowed'
                  : isSelected
                  ? 'bg-[#f0fdfa] border-[#005141] text-[#1c1917] shadow-sm'
                  : 'bg-white border-[#e7e0d6] text-[#57534e] hover:border-[#a8a29e]'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Clock className={`w-3.5 h-3.5 ${isSelected ? 'text-[#005141]' : 'text-[#a8a29e]'}`} />
                <span className="text-xs font-bold">{slot.time}</span>
              </div>
              <div className="flex items-center gap-2">
                {slot.available ? (
                  <span className={`text-[11px] font-bold ${slotsLeft <= 3 ? 'text-[#ea580c]' : 'text-[#005141]'}`}>
                    {slotsLeft} {slotsLeft === 1 ? 'cupo' : 'cupos'}
                  </span>
                ) : (
                  <span className="text-[11px] font-bold text-[#b91c1c] flex items-center gap-1">
                    <ShieldAlert className="w-3 h-3" /> Agotado
                  </span>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {/* Interactive Hold Demonstration */}
      <div className="mt-4 p-4 rounded-2xl bg-[#faf7f2] border-2 border-[#e7e0d6] space-y-3">
        <div className="flex items-center justify-between text-xs">
          <span className="text-[#57534e] font-semibold flex items-center gap-1.5">
            <ShoppingBag className="w-3.5 h-3.5 text-[#ea580c]" />
            {activePreset.productSample}
          </span>
          <span className="font-extrabold text-[#1c1917] font-mono">
            S/ {activePreset.priceSample.toFixed(2)}
          </span>
        </div>

        {isReserved ? (
          <div className="bg-[#f0fdfa] border-2 border-[#005141] rounded-2xl p-3 space-y-1.5 text-center animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-center gap-1.5 text-xs text-[#005141] font-bold">
              <CheckCircle2 className="w-4 h-4 text-[#005141]" />
              ¡Cupo bloqueado temporalmente (Hold 10m)!
            </div>
            <div className="text-[11px] text-[#57534e]">
              Tiempo para completar pago:{' '}
              <span className="font-mono font-extrabold text-[#ea580c] bg-white px-2 py-0.5 rounded-lg border border-[#fed7aa]">
                {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
              </span>
            </div>
            <button
              onClick={() => setIsReserved(false)}
              className="text-[10px] text-[#57534e] hover:text-[#005141] underline flex items-center justify-center gap-1 mx-auto pt-1 cursor-pointer"
            >
              <RefreshCw className="w-2.5 h-2.5" /> Liberar y reiniciar prueba
            </button>
          </div>
        ) : (
          <Button
            onClick={handleSimulateReservation}
            variant="terracotta"
            size="sm"
            className="w-full text-xs font-bold py-3 rounded-2xl shadow-md cursor-pointer"
            rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
          >
            Simular Reserva de Cupo (Hold 10m)
          </Button>
        )}
      </div>
    </div>
  );
}

'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Clock, Zap, MessageSquare, Sparkles } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

interface LiveOrder {
  id: string;
  store: string;
  category: string;
  item: string;
  price: string;
  slot: string;
  customer: string;
  status: string;
  iconEmoji: string;
  accentBg: string;
  accentText: string;
}

const LIVE_ORDERS: LiveOrder[] = [
  {
    id: 'ord-101',
    store: 'Panadería Don Pepe',
    category: 'Masa Madre & Horno',
    item: '2x Hogaza Masa Madre + 1x Croissant de Mantequilla',
    price: 'S/ 24.50',
    slot: '10:00 - 11:00 AM',
    customer: 'María R.',
    status: 'Cupo 9/10 asegurado',
    iconEmoji: '🥖',
    accentBg: 'bg-[#fff7ed] border-[#fed7aa]',
    accentText: 'text-[#ea580c]',
  },
  {
    id: 'ord-102',
    store: 'Dulce Amor Repostería',
    category: 'Repostería Creativa',
    item: '1x Torta Selva Negra Mediana + 6x Alfajores',
    price: 'S/ 52.00',
    slot: '03:30 - 04:30 PM',
    customer: 'Carlos G.',
    status: 'Hold temporal activo',
    iconEmoji: '🍰',
    accentBg: 'bg-pink-50 border-pink-200',
    accentText: 'text-pink-700',
  },
  {
    id: 'ord-103',
    store: 'Tokyo Dark Kitchen',
    category: 'Ramen & Sushi Bowls',
    item: '2x Tonkotsu Ramen Clásico + 1x Gyoza al Vapor',
    price: 'S/ 48.00',
    slot: '01:15 - 02:00 PM',
    customer: 'Lucía V.',
    status: 'Franja horaria confirmada',
    iconEmoji: '🍜',
    accentBg: 'bg-[#f0fdfa] border-[#ccfbf1]',
    accentText: 'text-[#005141]',
  },
];

export function HeroShowcase() {
  const [currentIndex, setCurrentIndex] = useState(0);

  // Auto-cycle live orders every 3.8 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % LIVE_ORDERS.length);
    }, 3800);
    return () => clearInterval(timer);
  }, []);

  const currentOrder = LIVE_ORDERS[currentIndex];

  return (
    <div className="relative w-full max-w-lg mx-auto select-none">
      {/* Warm Breathing Aurora Mesh Background Glows */}
      <div className="absolute -inset-4 bg-gradient-to-tr from-[#005141]/20 via-[#feae2c]/20 to-[#ea580c]/20 blur-3xl rounded-full opacity-70 animate-pulse pointer-events-none" />

      {/* Main Showcase Device Mockup */}
      <div className="relative bg-white/95 backdrop-blur-xl rounded-[32px] p-6 sm:p-7 shadow-2xl shadow-[#1c1917]/12 border-2 border-[#e7e0d6]">
        {/* Device Top Bar */}
        <div className="flex items-center justify-between pb-4 border-b border-[#e7e0d6]">
          <div className="flex items-center gap-2.5">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#005141] opacity-75" />
              <span className="relative inline-flex rounded-full h-3 w-3 bg-[#005141]" />
            </span>
            <span className="text-xs font-bold text-[#005141] uppercase tracking-wider">
              Flujo de Pedidos en Vivo
            </span>
          </div>
          <Badge variant="amber" size="sm" className="font-mono font-bold">
            0 sobreventas
          </Badge>
        </div>

        {/* Store Live Header */}
        <div className="mt-5 p-4 rounded-2xl bg-[#faf7f2] border-2 border-[#e7e0d6] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-[#ea580c] to-[#feae2c] text-white flex items-center justify-center text-2xl shadow-sm">
              {currentOrder.iconEmoji}
            </div>
            <div>
              <h4 className="text-sm font-extrabold text-[#1c1917]">{currentOrder.store}</h4>
              <p className="text-xs text-[#57534e] font-medium">{currentOrder.category}</p>
            </div>
          </div>
          <div className="text-right">
            <div className="text-[10px] uppercase font-bold text-[#78716c]">Capacidad</div>
            <div className="text-xs font-bold text-[#005141] flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-[#feae2c]" /> 92% ocupado
            </div>
          </div>
        </div>

        {/* Dynamic Live Order Card with Smooth Crossfade */}
        <div className="mt-5 relative min-h-[165px]">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentOrder.id}
              initial={{ opacity: 0, y: 16, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -16, scale: 0.98 }}
              transition={{ duration: 0.4, ease: [0.21, 0.47, 0.32, 0.98] }}
              className="p-5 rounded-2xl bg-white border-2 border-[#e7e0d6] shadow-sm space-y-3.5"
            >
              <div className="flex items-center justify-between text-xs pb-2 border-b border-[#e7e0d6]/70">
                <span className="font-bold text-[#1c1917] flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-[#ea580c]" />
                  Nuevo Pedido de {currentOrder.customer}
                </span>
                <span className="font-mono font-extrabold text-sm text-[#005141]">
                  {currentOrder.price}
                </span>
              </div>

              <p className="text-xs sm:text-sm font-semibold text-[#1c1917] leading-snug">
                {currentOrder.item}
              </p>

              {/* Slot & Capacity status */}
              <div className="pt-1 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5 font-bold text-[#57534e]">
                  <Clock className="w-3.5 h-3.5 text-[#005141]" />
                  <span>Franja: {currentOrder.slot}</span>
                </div>
                <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${currentOrder.accentBg} ${currentOrder.accentText}`}>
                  {currentOrder.status}
                </span>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Real-time Visual Capacity Gauge */}
        <div className="mt-4 p-3.5 rounded-2xl bg-[#faf7f2] border border-[#e7e0d6] space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-[#57534e]">
            <span className="flex items-center gap-1.5 text-[#1c1917]">
              <Clock className="w-3.5 h-3.5 text-[#ea580c]" /> Control de Franja en Tiempo Real
            </span>
            <span className="font-mono text-[#005141]">9 / 10 cupos</span>
          </div>
          <div className="w-full h-2.5 bg-[#e7e0d6] rounded-full overflow-hidden">
            <motion.div
              initial={{ width: '60%' }}
              animate={{ width: '90%' }}
              transition={{ duration: 1.5, repeat: Infinity, repeatType: 'reverse' }}
              className="h-full bg-gradient-to-r from-[#005141] via-[#166a57] to-[#feae2c] rounded-full"
            />
          </div>
        </div>

        {/* Carousel Progress Indicators */}
        <div className="mt-4 flex items-center justify-center gap-1.5">
          {LIVE_ORDERS.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setCurrentIndex(idx)}
              className={`h-1.5 rounded-full transition-all cursor-pointer ${
                currentIndex === idx ? 'w-6 bg-[#005141]' : 'w-2 bg-[#e7e0d6] hover:bg-[#a8a29e]'
              }`}
              aria-label={`Ver orden ${idx + 1}`}
            />
          ))}
        </div>
      </div>

      {/* Floating Satellite Card 1 (Top-Right): Hold 10 min */}
      <motion.div
        animate={{ y: [0, -8, 0] }}
        transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
        className="hidden sm:flex absolute -top-6 -right-6 z-20 items-center gap-2.5 bg-white/95 backdrop-blur-md p-3.5 rounded-2xl border-2 border-[#e7e0d6] shadow-xl shadow-[#1c1917]/10"
      >
        <div className="w-9 h-9 rounded-xl bg-[#fffbeb] text-[#92400e] border border-[#fef3c7] flex items-center justify-center">
          <Zap className="w-5 h-5 text-[#feae2c] fill-[#feae2c]" />
        </div>
        <div className="text-left">
          <div className="text-[10px] font-bold text-[#78716c] uppercase">Hold Temporal</div>
          <div className="text-xs font-extrabold text-[#1c1917] font-mono">09:42 min asegurado</div>
        </div>
      </motion.div>

      {/* Floating Satellite Card 2 (Bottom-Left): WhatsApp Bridge */}
      <motion.div
        animate={{ y: [0, 8, 0] }}
        transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
        className="hidden sm:flex absolute -bottom-6 -left-6 z-20 items-center gap-2.5 bg-white/95 backdrop-blur-md p-3.5 rounded-2xl border-2 border-[#005141] shadow-xl shadow-[#005141]/15"
      >
        <div className="w-9 h-9 rounded-xl bg-[#005141] text-white flex items-center justify-center shadow-xs">
          <MessageSquare className="w-4 h-4" />
        </div>
        <div className="text-left">
          <div className="text-[10px] font-bold text-[#005141] uppercase">WhatsApp Order</div>
          <div className="text-xs font-extrabold text-[#1c1917]">Cero pedidos traspapelados</div>
        </div>
      </motion.div>
    </div>
  );
}

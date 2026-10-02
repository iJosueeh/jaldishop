'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { Clock, Zap, MessageSquare, ShieldCheck, Flame } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import Image from 'next/image';

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
  badgeLabel: string;
  badgeVariant: 'jade' | 'terracotta' | 'amber';
}

const LIVE_ORDERS: LiveOrder[] = [
  {
    id: 'ord-101',
    store: 'Panadería Don Pepe',
    category: 'Masa Madre & Horno Matutino',
    item: '2x Hogaza Masa Madre + 1x Croissant Francés',
    price: 'S/ 24.50',
    slot: '10:00 - 11:00 AM',
    customer: 'María R. (Miraflores)',
    status: 'Cupo 9/10 asegurado',
    iconEmoji: '🥐',
    badgeLabel: 'Horneado Hoy',
    badgeVariant: 'jade',
  },
  {
    id: 'ord-102',
    store: 'Dulce Amor Repostería',
    category: 'Repostería Artesanal & Eventos',
    item: '1x Torta Selva Negra Mediana + 6x Alfajores',
    price: 'S/ 52.00',
    slot: '03:30 - 04:30 PM',
    customer: 'Carlos G. (Surco)',
    status: 'Hold 10m temporal activo',
    iconEmoji: '🍰',
    badgeLabel: 'Personalizado',
    badgeVariant: 'terracotta',
  },
  {
    id: 'ord-103',
    store: 'Tokyo Dark Kitchen',
    category: 'Ramen & Sushi Bowls al Vacío',
    item: '2x Tonkotsu Ramen Clásico + 1x Gyoza al Vapor',
    price: 'S/ 48.00',
    slot: '01:15 - 02:00 PM',
    customer: 'Lucía V. (San Isidro)',
    status: 'Franja de almuerzo lista',
    iconEmoji: '🍜',
    badgeLabel: 'Alta Precisión',
    badgeVariant: 'amber',
  },
];

export function HeroShowcase() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  // 3D Tilt Physics using Framer Motion Springs
  const x = useMotionValue(0);
  const y = useMotionValue(0);

  const mouseXSpring = useSpring(x, { stiffness: 180, damping: 20 });
  const mouseYSpring = useSpring(y, { stiffness: 180, damping: 20 });

  const rotateX = useTransform(mouseYSpring, [-0.5, 0.5], ['10deg', '-10deg']);
  const rotateY = useTransform(mouseXSpring, [-0.5, 0.5], ['-12deg', '12deg']);
  const glareX = useTransform(mouseXSpring, [-0.5, 0.5], ['0%', '100%']);
  const glareY = useTransform(mouseYSpring, [-0.5, 0.5], ['0%', '100%']);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const width = rect.width;
    const height = rect.height;

    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const xPct = mouseX / width - 0.5;
    const yPct = mouseY / height - 0.5;

    x.set(xPct);
    y.set(yPct);
  };

  const handleMouseLeave = () => {
    x.set(0);
    y.set(0);
  };

  // Auto-cycle live orders every 4 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % LIVE_ORDERS.length);
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  const currentOrder = LIVE_ORDERS[currentIndex];

  return (
    <div
      ref={containerRef}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      className="relative w-full max-w-lg mx-auto select-none [perspective:1200px]"
    >
      {/* Warm Breathing Aurora Glow Layer */}
      <div className="absolute -inset-6 bg-gradient-to-tr from-[#005141]/25 via-[#feae2c]/20 to-[#ea580c]/25 blur-3xl rounded-[40px] opacity-75 animate-pulse pointer-events-none -z-10" />

      {/* 3D Motion Canvas Card */}
      <motion.div
        style={{
          rotateX,
          rotateY,
          transformStyle: 'preserve-3d',
        }}
        className="relative bg-white rounded-[36px] p-6 sm:p-8 shadow-2xl shadow-[#1c1917]/15 border-2 border-[#e7e0d6] transition-shadow duration-300"
      >
        {/* Dynamic Light Glare Highlight */}
        <motion.div
          style={{
            left: glareX,
            top: glareY,
          }}
          className="absolute w-40 h-40 -translate-x-1/2 -translate-y-1/2 bg-gradient-to-br from-white/70 to-transparent blur-2xl pointer-events-none rounded-full"
        />

        {/* Device Top Bar with Brand Badge */}
        <div
          style={{ transform: 'translateZ(25px)' }}
          className="flex items-center justify-between pb-4 border-b border-[#e7e0d6]"
        >
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#005141] flex items-center justify-center p-1.5 shadow-sm">
              <Image src="/favicon.svg" alt="JaldiShop" width={20} height={20} className="object-contain" />
            </div>
            <div>
              <span className="text-xs font-extrabold text-[#1c1917] flex items-center gap-1.5">
                Terminal de Pedidos MYPE
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#005141] opacity-75" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-[#005141]" />
                </span>
              </span>
              <span className="text-[10px] font-bold text-[#57534e]">Sincronización en tiempo real</span>
            </div>
          </div>
          <Badge variant="jade" size="sm" className="font-mono font-bold">
            0 sobreventas
          </Badge>
        </div>

        {/* Store Profile Ribbon */}
        <div
          style={{ transform: 'translateZ(35px)' }}
          className="mt-5 p-4 rounded-2xl bg-[#faf7f2] border-2 border-[#e7e0d6] flex items-center justify-between shadow-xs"
        >
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#ea580c] to-[#feae2c] text-white flex items-center justify-center text-2xl shadow-sm border-2 border-white">
              {currentOrder.iconEmoji}
            </div>
            <div>
              <h4 className="text-sm font-extrabold text-[#1c1917] flex items-center gap-1.5">
                {currentOrder.store}
                <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
              </h4>
              <p className="text-xs text-[#57534e] font-medium">{currentOrder.category}</p>
            </div>
          </div>
          <Badge variant={currentOrder.badgeVariant} size="sm">
            {currentOrder.badgeLabel}
          </Badge>
        </div>

        {/* Dynamic Order Card (High Depth Layer) */}
        <div
          style={{ transform: 'translateZ(50px)' }}
          className="mt-5 relative min-h-[160px]"
        >
          <AnimatePresence mode="wait">
            <motion.div
              key={currentOrder.id}
              initial={{ opacity: 0, y: 14, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -14, scale: 0.96 }}
              transition={{ duration: 0.35, ease: [0.21, 0.47, 0.32, 0.98] }}
              className="p-5 rounded-2xl bg-white border-2 border-[#e7e0d6] shadow-md space-y-3.5"
            >
              <div className="flex items-center justify-between text-xs pb-2 border-b border-[#e7e0d6]/70">
                <span className="font-bold text-[#1c1917] flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-[#ea580c]" />
                  {currentOrder.customer}
                </span>
                <span className="font-mono font-extrabold text-sm text-[#005141] bg-[#f0fdfa] px-2.5 py-0.5 rounded-lg border border-[#ccfbf1]">
                  {currentOrder.price}
                </span>
              </div>

              <p className="text-xs sm:text-sm font-bold text-[#1c1917] leading-snug">
                {currentOrder.item}
              </p>

              {/* Slot & Capacity status */}
              <div className="pt-1 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5 font-bold text-[#57534e]">
                  <Clock className="w-3.5 h-3.5 text-[#005141]" />
                  <span>Franja: {currentOrder.slot}</span>
                </div>
                <span className="text-[11px] font-bold text-[#005141] bg-[#f0fdfa] px-2.5 py-0.5 rounded-full border border-[#99f6e4]">
                  {currentOrder.status}
                </span>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Real-time Capacity Progress Bar */}
        <div
          style={{ transform: 'translateZ(30px)' }}
          className="mt-4 p-3.5 rounded-2xl bg-[#faf7f2] border border-[#e7e0d6] space-y-2"
        >
          <div className="flex items-center justify-between text-xs font-bold text-[#57534e]">
            <span className="flex items-center gap-1.5 text-[#1c1917]">
              <Clock className="w-3.5 h-3.5 text-[#ea580c]" /> Ocupación de la Franja
            </span>
            <span className="font-mono text-[#005141] font-extrabold">9 / 10 cupos</span>
          </div>
          <div className="w-full h-2.5 bg-[#e7e0d6] rounded-full overflow-hidden">
            <motion.div
              initial={{ width: '65%' }}
              animate={{ width: '90%' }}
              transition={{ duration: 2, repeat: Infinity, repeatType: 'reverse' }}
              className="h-full bg-gradient-to-r from-[#005141] via-[#166a57] to-[#feae2c] rounded-full"
            />
          </div>
        </div>

        {/* Carousel Indicators */}
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
      </motion.div>

      {/* Floating 3D Satellite Card 1 (Top-Right): Hold 10 min */}
      <motion.div
        style={{ transform: 'translateZ(65px)' }}
        animate={{ y: [0, -8, 0] }}
        transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
        className="hidden sm:flex absolute -top-5 -right-5 z-20 items-center gap-3 bg-white/95 backdrop-blur-md p-3.5 rounded-2xl border-2 border-[#feae2c] shadow-xl shadow-[#feae2c]/15"
      >
        <div className="w-10 h-10 rounded-xl bg-[#fffbeb] text-[#92400e] border border-[#fef3c7] flex items-center justify-center">
          <Zap className="w-5 h-5 text-[#feae2c] fill-[#feae2c]" />
        </div>
        <div className="text-left">
          <div className="text-[10px] font-bold text-[#78716c] uppercase tracking-wider">Hold Temporal</div>
          <div className="text-xs font-extrabold text-[#1c1917] font-mono">09:42 min asegurado</div>
        </div>
      </motion.div>

      {/* Floating 3D Satellite Card 2 (Bottom-Left): WhatsApp Bridge */}
      <motion.div
        style={{ transform: 'translateZ(65px)' }}
        animate={{ y: [0, 8, 0] }}
        transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
        className="hidden sm:flex absolute -bottom-5 -left-5 z-20 items-center gap-3 bg-white/95 backdrop-blur-md p-3.5 rounded-2xl border-2 border-[#005141] shadow-xl shadow-[#005141]/20"
      >
        <div className="w-10 h-10 rounded-xl bg-[#005141] text-white flex items-center justify-center shadow-xs">
          <MessageSquare className="w-5 h-5" />
        </div>
        <div className="text-left">
          <div className="text-[10px] font-bold text-[#005141] uppercase tracking-wider">WhatsApp Bridge</div>
          <div className="text-xs font-extrabold text-[#1c1917]">Comprobante validado</div>
        </div>
      </motion.div>

      {/* Floating 3D Satellite Card 3 (Bottom-Right): Sello Artesanal MYPE */}
      <motion.div
        style={{ transform: 'translateZ(75px)' }}
        animate={{ y: [0, -6, 0] }}
        transition={{ duration: 4.8, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
        className="hidden sm:flex absolute -bottom-8 -right-3 z-20 items-center gap-2 bg-[#faf7f2] px-3.5 py-2 rounded-2xl border-2 border-[#e7e0d6] shadow-lg shadow-[#1c1917]/10"
      >
        <Flame className="w-4 h-4 text-[#ea580c]" />
        <span className="text-[11px] font-extrabold text-[#1c1917]">
          100% Producción Real
        </span>
      </motion.div>
    </div>
  );
}

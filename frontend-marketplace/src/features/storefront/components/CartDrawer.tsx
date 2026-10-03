'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  ShoppingBag,
  Plus,
  Minus,
  Trash2,
  ShieldCheck,
  Bike,
  Store,
  ArrowRight,
  MessageSquare,
  Lock,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { CartItem } from './CatalogContainer';
import { CapacitySlot } from './CapacitySlotPicker';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartItem[];
  onUpdateQuantity: (productId: string, delta: number) => void;
  onRemoveItem?: (productId: string) => void;
  selectedSlot?: CapacitySlot | null;
  fulfillmentType?: 'DELIVERY' | 'PICKUP';
  deliveryFee?: number;
  storeName?: string;
  onProceedCheckout?: () => void;
  countdownSeconds?: number;
}

const TOTAL_COUNTDOWN_SECONDS = 600; // 10 minutes

export function CartDrawer({
  isOpen,
  onClose,
  cart,
  onUpdateQuantity,
  onRemoveItem,
  selectedSlot,
  fulfillmentType = 'DELIVERY',
  deliveryFee = 5.0,
  storeName = 'La Tienda',
  onProceedCheckout,
  countdownSeconds,
}: CartDrawerProps) {
  const [internalSecondsLeft, setInternalSecondsLeft] = useState(TOTAL_COUNTDOWN_SECONDS);

  // If controlled from parent, use countdownSeconds, otherwise internal
  const secondsLeft = countdownSeconds !== undefined ? countdownSeconds : internalSecondsLeft;

  // Uncontrolled fallback: tick countdown timer only if countdownSeconds is not passed
  useEffect(() => {
    if (countdownSeconds !== undefined) return;
    if (cart.length === 0) {
      setInternalSecondsLeft(TOTAL_COUNTDOWN_SECONDS);
      return;
    }

    const interval = setInterval(() => {
      if (document.visibilityState === 'hidden') return;
      setInternalSecondsLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(interval);
  }, [countdownSeconds, cart.length]);

  // SVG Circular Ring parameters
  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  const progressRatio = secondsLeft / TOTAL_COUNTDOWN_SECONDS;
  const strokeDashoffset = circumference - progressRatio * circumference;

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const formattedTime = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

  const subtotal = cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const actualDeliveryFee = fulfillmentType === 'PICKUP' ? 0 : (subtotal > 0 ? deliveryFee : 0);
  const total = subtotal + actualDeliveryFee;
  const totalItemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity"
            aria-hidden="true"
          />

          {/* Slide-over Drawer Panel */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="relative w-full max-w-md bg-white h-full shadow-2xl flex flex-col z-10 overflow-hidden"
          >
            {/* Drawer Top Header */}
            <div className="p-5 sm:p-6 pb-4 border-b border-stone-200/90 flex items-center justify-between bg-white">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-2xl bg-[#005141] text-white flex items-center justify-center shadow-xs">
                  <ShoppingBag className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-black text-[#1c1917] tracking-tight">
                    Tu Pedido en {storeName}
                  </h3>
                  <p className="text-[11px] font-bold text-[#57534e]">
                    {totalItemsCount} {totalItemsCount === 1 ? 'producto' : 'productos'} en tu lista
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-xl bg-stone-100 hover:bg-stone-200 text-stone-500 hover:text-[#1c1917] cursor-pointer transition-colors"
                aria-label="Cerrar carrito"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* 10-Minute Reservation SVG Countdown Ring Box */}
            {cart.length > 0 && (
              <div className="p-4 mx-5 my-3 bg-[#faf7f2] rounded-2xl border border-stone-200/90 flex items-center gap-4 shadow-xs">
                {/* SVG Countdown Ring */}
                <div className="relative w-16 h-16 shrink-0 flex items-center justify-center">
                  <svg className="w-16 h-16 -rotate-90 transform" viewBox="0 0 100 100">
                    <circle
                      cx="50"
                      cy="50"
                      r={radius}
                      stroke="#e7e5e4"
                      strokeWidth="7"
                      fill="none"
                    />
                    <motion.circle
                      cx="50"
                      cy="50"
                      r={radius}
                      stroke={secondsLeft < 120 ? '#ea580c' : '#005141'}
                      strokeWidth="7"
                      strokeDasharray={circumference}
                      strokeDashoffset={strokeDashoffset}
                      strokeLinecap="round"
                      fill="none"
                      className="transition-all duration-1000 ease-linear"
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                    <span className="font-mono font-black text-xs text-[#1c1917] leading-none">
                      {formattedTime}
                    </span>
                    <span className="text-[8px] font-bold text-[#78716c] uppercase mt-0.5">
                      min
                    </span>
                  </div>
                </div>

                {/* Ring Explanatory Text */}
                <div className="space-y-0.5 text-xs">
                  <div className="font-black text-[#1c1917] flex items-center gap-1.5">
                    <Lock className="w-3.5 h-3.5 text-[#005141]" />
                    <span>10 min de reserva para pagar</span>
                  </div>
                  <p className="text-[11px] text-[#57534e] leading-snug">
                    Tu cupo de cocina está apartado. No perderás tu lugar mientras confirmas tu pago.
                  </p>
                </div>
              </div>
            )}

            {/* Selected Slot Notice */}
            {selectedSlot && (
              <div className="px-5 py-2 mx-5 mb-2 bg-[#f0fdfa] rounded-xl border border-teal-200/70 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5 font-bold text-[#005141]">
                  {fulfillmentType === 'DELIVERY' ? (
                    <Bike className="w-3.5 h-3.5 text-[#ea580c]" />
                  ) : (
                    <Store className="w-3.5 h-3.5 text-[#005141]" />
                  )}
                  <span>{selectedSlot.timeRange}</span>
                </div>
                <span className="text-[10px] font-black uppercase text-[#005141] bg-white px-2 py-0.5 rounded-md border border-teal-100">
                  {fulfillmentType === 'DELIVERY' ? 'Delivery' : 'Retiro'}
                </span>
              </div>
            )}

            {/* Cart Items Scrollable Container */}
            <div className="flex-1 overflow-y-auto px-5 py-2 space-y-3">
              {cart.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-3 text-[#57534e]">
                  <div className="w-16 h-16 rounded-full bg-[#faf7f2] flex items-center justify-center text-2xl border border-stone-200">
                    🛒
                  </div>
                  <h4 className="text-base font-bold text-[#1c1917]">Tu carrito está vacío</h4>
                  <p className="text-xs text-[#78716c] max-w-xs">
                    Explora el menú y añade tus delicias favoritas para apartar tu franja horaria.
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={onClose}
                    className="mt-2 rounded-xl text-xs font-bold border-stone-300"
                  >
                    Ver Menú
                  </Button>
                </div>
              ) : (
                cart.map((item) => (
                  <div
                    key={item.product.id}
                    className="p-3.5 rounded-2xl bg-white border border-stone-200/90 shadow-2xs flex items-center justify-between gap-3 text-xs"
                  >
                    {/* Item Icon & Details */}
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div className={`w-11 h-11 rounded-xl ${item.product.imageBg} flex items-center justify-center text-xl shrink-0 shadow-2xs`}>
                        {item.product.iconText}
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="font-bold text-[#1c1917] truncate leading-tight">
                          {item.product.name}
                        </div>
                        <div className="font-mono text-[#005141] font-black mt-0.5">
                          {formatCurrency(item.product.price * item.quantity)}
                        </div>
                        {item.notes && (
                          <div className="text-[10px] text-stone-500 italic truncate mt-0.5">
                            “{item.notes}”
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Stepper Controls */}
                    <div className="flex items-center gap-1.5 bg-[#faf7f2] p-1 rounded-xl border border-stone-200 shrink-0">
                      <button
                        type="button"
                        onClick={() => {
                          if (item.quantity === 1 && onRemoveItem) {
                            onRemoveItem(item.product.id);
                          } else {
                            onUpdateQuantity(item.product.id, -1);
                          }
                        }}
                        className="w-6 h-6 rounded-lg bg-white hover:bg-stone-100 text-stone-700 flex items-center justify-center cursor-pointer transition-colors shadow-2xs"
                        aria-label="Disminuir"
                      >
                        {item.quantity === 1 ? (
                          <Trash2 className="w-3 h-3 text-rose-500" />
                        ) : (
                          <Minus className="w-3 h-3" />
                        )}
                      </button>

                      <span className="w-5 text-center font-bold font-mono text-xs text-[#1c1917]">
                        {item.quantity}
                      </span>

                      <button
                        type="button"
                        onClick={() => onUpdateQuantity(item.product.id, 1)}
                        className="w-6 h-6 rounded-lg bg-[#005141] hover:bg-[#00382d] text-white flex items-center justify-center cursor-pointer transition-colors shadow-2xs"
                        aria-label="Aumentar"
                      >
                        <Plus className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Drawer Footer: Price breakdown & Checkout CTA */}
            {cart.length > 0 && (
              <div className="p-5 bg-[#faf7f2] border-t border-stone-200/90 space-y-3">
                <div className="space-y-1.5 text-xs text-[#57534e]">
                  <div className="flex justify-between">
                    <span>Subtotal de productos</span>
                    <span className="font-mono font-bold text-[#1c1917]">{formatCurrency(subtotal)}</span>
                  </div>

                  <div className="flex justify-between">
                    <span>{fulfillmentType === 'DELIVERY' ? 'Costo de envío estimado' : 'Retiro en tienda'}</span>
                    <span className="font-mono font-bold text-[#1c1917]">
                      {actualDeliveryFee === 0 ? 'Gratis' : formatCurrency(actualDeliveryFee)}
                    </span>
                  </div>

                  <div className="flex justify-between text-sm font-black text-[#1c1917] pt-2 border-t border-stone-200">
                    <span>Total a pagar</span>
                    <span className="font-mono text-base text-[#005141] font-black">{formatCurrency(total)}</span>
                  </div>
                </div>

                {/* Primary Proceed Button */}
                <Button
                  variant="terracotta"
                  size="lg"
                  onClick={() => {
                    onProceedCheckout?.();
                  }}
                  className="w-full py-4 rounded-2xl font-black text-sm shadow-xl shadow-[#ea580c]/30 cursor-pointer border-0 bg-[#ea580c] hover:bg-[#c2410c] flex items-center justify-center gap-2"
                >
                  <MessageSquare className="w-4 h-4" />
                  <span>Continuar al Pedido por WhatsApp</span>
                  <ArrowRight className="w-4 h-4 ml-1" />
                </Button>

                <div className="text-center text-[10px] text-[#78716c] font-medium flex items-center justify-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
                  <span>Pedido seguro • 10 minutos de reserva garantizada</span>
                </div>
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

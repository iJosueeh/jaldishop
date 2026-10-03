'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  Copy,
  Check,
  Send,
  Sparkles,
  ShieldCheck,
  Clock,
  ExternalLink,
  ChevronRight,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';
import confetti from 'canvas-confetti';
import { CartItem } from './CatalogContainer';
import { CapacitySlot } from './CapacitySlotPicker';
import { PaymentMethod } from './PaymentHubModal';

export interface WhatsAppBridgeModalProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartItem[];
  selectedSlot?: CapacitySlot | null;
  fulfillmentType?: 'DELIVERY' | 'PICKUP';
  deliveryFee?: number;
  storeName?: string;
  storePhone?: string;
  paymentMethod?: PaymentMethod;
  hasReceipt?: boolean;
  onTrackOrder?: (orderId: string) => void;
}

export function WhatsAppBridgeModal({
  isOpen,
  onClose,
  cart,
  selectedSlot,
  fulfillmentType = 'DELIVERY',
  deliveryFee = 5.0,
  storeName = 'Panadería Don Pepe',
  storePhone = '51987654321',
  paymentMethod = 'yape',
  hasReceipt = true,
  onTrackOrder,
}: WhatsAppBridgeModalProps) {
  const [copied, setCopied] = useState(false);
  const [orderSent, setOrderSent] = useState(false);
  const [orderId, setOrderId] = useState('JALDI-1048');

  // Generate randomized order ID on client mount to prevent SSR hydration mismatch
  useEffect(() => {
    setOrderId(`JALDI-${Math.floor(1000 + Math.random() * 9000)}`);
  }, []);

  const subtotal = cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const actualDeliveryFee = fulfillmentType === 'PICKUP' ? 0 : (subtotal > 0 ? deliveryFee : 0);
  const total = subtotal + actualDeliveryFee;
  const slotTime = selectedSlot?.timeRange || '10:00 - 11:00 AM';

  const paymentMethodLabel =
    paymentMethod === 'yape'
      ? 'Yape'
      : paymentMethod === 'plin'
      ? 'Plin'
      : 'Transferencia BCP';

  // Build the clean WhatsApp message
  const itemsText = cart
    .map((item) => `• ${item.quantity}x ${item.product.name} (${formatCurrency(item.product.price * item.quantity)})${item.notes ? ` [Nota: ${item.notes}]` : ''}`)
    .join('\n');

  const formattedWhatsAppMessage = `👋 ¡Hola ${storeName}! Acabo de generar mi pedido por JaldiShop.

📦 *PEDIDO #${orderId}*
${itemsText}

⏰ *Franja Horaria:* ${slotTime} (Cupo apartado)
🛵 *Modalidad:* ${fulfillmentType === 'DELIVERY' ? 'Delivery a Domicilio' : 'Retiro en Tienda'}
💰 *Total a Pagar:* ${formatCurrency(total)}
🧾 *Pago:* ${paymentMethodLabel} ${hasReceipt ? '✅ Comprobante adjunto' : '⏳ Por transferir'}

Por favor confírmenme la recepción del pedido para la preparación. ¡Muchas gracias!`;

  const handleCopyMessage = () => {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(formattedWhatsAppMessage);
    }
    setCopied(true);
    toast.success('¡Mensaje para WhatsApp copiado al portapapeles!');
    setTimeout(() => setCopied(false), 2500);
  };

  const handleSendToWhatsApp = () => {
    // Trigger celebration confetti
    try {
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#005141', '#ea580c', '#feae2c', '#25D366'],
      });
    } catch {
      // Confetti fallback
    }

    setOrderSent(true);
    toast.success('¡Pedido enviado al WhatsApp del comercio!', {
      description: `Código #${orderId}. Franja ${slotTime} protegida contra sobreventa.`,
    });

    const encoded = encodeURIComponent(formattedWhatsAppMessage);
    const waUrl = `https://wa.me/${storePhone}?text=${encoded}`;
    window.open(waUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/75 backdrop-blur-xs"
            aria-hidden="true"
          />

          {/* Modal Content */}
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 20 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[92vh]"
          >
            {/* WhatsApp Window Header */}
            <div className="p-4 sm:p-5 bg-[#075e54] text-white flex items-center justify-between shadow-md">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-emerald-400 text-[#075e54] font-black flex items-center justify-center text-sm shadow-sm ring-2 ring-white/20">
                  {storeName.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-black flex items-center gap-1.5 leading-snug">
                    <span>{storeName}</span>
                    <ShieldCheck className="w-4 h-4 text-emerald-300" />
                  </h3>
                  <div className="text-[11px] text-emerald-200 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span>WhatsApp Order Bridge • En línea</span>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white cursor-pointer transition-colors"
                aria-label="Cerrar modal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Sub-header Ribbon */}
            <div className="bg-[#e5ddd5]/60 px-5 py-2.5 border-b border-stone-200 flex items-center justify-between text-xs">
              <span className="text-stone-600 font-bold flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
                Pedido estructurado listo para enviar
              </span>
              <span className="font-mono font-black text-[#005141] bg-white px-2 py-0.5 rounded-md shadow-2xs border border-stone-200">
                #{orderId}
              </span>
            </div>

            {/* Chat Body Simulation */}
            <div className="p-5 sm:p-6 overflow-y-auto space-y-4 bg-[#f0f2f5]/80 flex-1">
              {/* WhatsApp Bubble */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
                className="relative bg-[#dcf8c6] text-[#1c1917] p-4 sm:p-5 rounded-2xl rounded-tr-none shadow-md border border-emerald-200/60 space-y-3 font-sans"
              >
                <div className="text-xs sm:text-sm font-semibold leading-relaxed whitespace-pre-line text-stone-800">
                  {formattedWhatsAppMessage}
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-emerald-300/50 text-[10px] text-emerald-800 font-mono">
                  <span>Generado con JaldiShop Bridge</span>
                  <span className="font-bold flex items-center gap-1">
                    10:42 AM <Check className="w-3 h-3 text-[#34b7f1] stroke-[3]" />
                  </span>
                </div>
              </motion.div>

              {/* Verified Information Badges */}
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-3 bg-white rounded-2xl border border-stone-200 shadow-2xs space-y-1">
                  <div className="text-[10px] text-stone-500 font-bold uppercase flex items-center gap-1">
                    <Clock className="w-3 h-3 text-[#ea580c]" /> Franja Asegurada
                  </div>
                  <div className="font-black text-[#1c1917] font-mono truncate">{slotTime}</div>
                </div>

                <div className="p-3 bg-white rounded-2xl border border-stone-200 shadow-2xs space-y-1">
                  <div className="text-[10px] text-stone-500 font-bold uppercase flex items-center gap-1">
                    <ShieldCheck className="w-3 h-3 text-[#005141]" /> Total Confirmado
                  </div>
                  <div className="font-black text-[#005141] font-mono">{formatCurrency(total)}</div>
                </div>
              </div>

              {/* Order Sent Success Status */}
              {orderSent && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="p-3.5 bg-[#f0fdfa] rounded-2xl border border-teal-300 text-center space-y-1"
                >
                  <div className="text-xs font-black text-[#005141] flex items-center justify-center gap-1.5">
                    <Check className="w-4 h-4 text-emerald-500 stroke-[3]" />
                    ¡Pedido enviado al comercio exitosamente!
                  </div>
                  <p className="text-[11px] text-stone-600">
                    El negocio preparará tu orden según la franja horaria pactada.
                  </p>
                </motion.div>
              )}
            </div>

            {/* Modal Actions Footer */}
            <div className="p-4 sm:p-5 bg-white border-t border-stone-200 flex flex-col sm:flex-row items-center gap-3">
              <button
                type="button"
                onClick={handleCopyMessage}
                className="w-full sm:w-auto px-4 py-3 rounded-2xl bg-stone-100 hover:bg-stone-200 text-stone-800 text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-[#005141]" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Mensaje copiado' : 'Copiar texto'}</span>
              </button>

              <Button
                type="button"
                variant="primary"
                size="lg"
                onClick={handleSendToWhatsApp}
                className="w-full sm:flex-1 py-3.5 rounded-2xl font-black text-xs sm:text-sm bg-[#25d366] hover:bg-[#128c7e] text-white border-0 shadow-lg shadow-emerald-500/25 cursor-pointer flex items-center justify-center gap-2"
              >
                <Send className="w-4 h-4" />
                <span>Abrir WhatsApp y Enviar Pedido</span>
                <ExternalLink className="w-3.5 h-3.5 ml-0.5 opacity-80" />
              </Button>
            </div>

            {/* Action to proceed to tracking */}
            {onTrackOrder && (
              <div className="px-5 py-2.5 bg-stone-50 border-t border-stone-100 text-center">
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onTrackOrder(orderId);
                  }}
                  className="text-xs font-bold text-[#005141] hover:text-[#00382d] inline-flex items-center gap-1 cursor-pointer"
                >
                  <span>Ver seguimiento y recibo digital en vivo</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

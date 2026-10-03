'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  CheckCircle2,
  Clock,
  Flame,
  Bike,
  Store,
  Sparkles,
  Check,
  ExternalLink,
  ShieldCheck,
  Share2,
  PackageCheck,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';
import { CartItem } from './CatalogContainer';
import { CapacitySlot } from './CapacitySlotPicker';
import { PaymentMethod } from './PaymentHubModal';

export type OrderStatusStep = 'CONFIRMED' | 'PREPARING' | 'IN_TRANSIT' | 'DELIVERED';

export interface OrderTrackerModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderId?: string;
  storeName?: string;
  storePhone?: string;
  cart: CartItem[];
  selectedSlot: CapacitySlot;
  fulfillmentType: 'DELIVERY' | 'PICKUP';
  deliveryFee: number;
  paymentMethod: PaymentMethod;
  initialStep?: OrderStatusStep;
}

interface StepInfo {
  id: OrderStatusStep;
  label: string;
  sublabel: string;
  description: string;
  icon: React.ReactNode;
  estimatedTime?: string;
}

export function OrderTrackerModal({
  isOpen,
  onClose,
  orderId = 'JALDI-1048',
  storeName = 'Panadería Don Pepe',
  storePhone = '51999888777',
  cart,
  selectedSlot,
  fulfillmentType,
  deliveryFee,
  paymentMethod,
  initialStep = 'CONFIRMED',
}: OrderTrackerModalProps) {
  const [currentStep, setCurrentStep] = useState<OrderStatusStep>(initialStep);
  const [copiedLink, setCopiedLink] = useState(false);
  const [activeTab, setActiveTab] = useState<'TRACKER' | 'RECEIPT'>('TRACKER');

  useEffect(() => {
    setCurrentStep(initialStep);
  }, [initialStep, isOpen]);

  const steps: StepInfo[] = [
    {
      id: 'CONFIRMED',
      label: 'Pago Confirmado',
      sublabel: 'Cupo Bloqueado',
      description: 'El negocio ha recibido tu pedido y tu cupo de franja está 100% asegurado.',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-600" />,
      estimatedTime: 'Completado',
    },
    {
      id: 'PREPARING',
      label: 'En Preparación',
      sublabel: 'Horno & Taller Activo',
      description: 'Los artesanos están elaborando tu orden con ingredientes frescos del día.',
      icon: <Flame className="w-5 h-5 text-amber-500" />,
      estimatedTime: '~15-20 min',
    },
    {
      id: 'IN_TRANSIT',
      label: fulfillmentType === 'DELIVERY' ? 'En Camino' : 'Listo para Retiro',
      sublabel: fulfillmentType === 'DELIVERY' ? 'Repartidor en Ruta' : 'Empacado en Mostrador',
      description:
        fulfillmentType === 'DELIVERY'
          ? 'Tu pedido sellado en empaque térmico kraft va en camino a tu dirección.'
          : 'Tu pedido está empacado con sello de frescura listo para que lo recojas.',
      icon:
        fulfillmentType === 'DELIVERY' ? (
          <Bike className="w-5 h-5 text-emerald-600" />
        ) : (
          <Store className="w-5 h-5 text-emerald-600" />
        ),
      estimatedTime: `Franja ${selectedSlot.timeRange}`,
    },
    {
      id: 'DELIVERED',
      label: fulfillmentType === 'DELIVERY' ? 'Entregado' : 'Retirado',
      sublabel: '¡Que lo disfrutes!',
      description: '¡Orden completada con éxito! Gracias por apoyar el comercio local.',
      icon: <PackageCheck className="w-5 h-5 text-teal-600" />,
      estimatedTime: 'Finalizado',
    },
  ];

  const stepOrder: OrderStatusStep[] = ['CONFIRMED', 'PREPARING', 'IN_TRANSIT', 'DELIVERED'];
  const currentStepIndex = stepOrder.indexOf(currentStep);

  const subtotal = cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const total = subtotal + deliveryFee;

  const handleCopyLink = () => {
    if (typeof window !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(
        `${window.location.origin}/pedido/${orderId}?status=${currentStep.toLowerCase()}`
      );
      setCopiedLink(true);
      toast.success('Enlace de seguimiento copiado', {
        description: 'Puedes compartirlo o abrirlo en cualquier dispositivo.',
      });
      setTimeout(() => setCopiedLink(false), 2500);
    }
  };

  const handleOpenSupport = () => {
    const message = encodeURIComponent(
      `¡Hola ${storeName}! Tengo una consulta sobre mi pedido #${orderId} programado para la franja ${selectedSlot.timeRange}.`
    );
    window.open(`https://wa.me/${storePhone}?text=${message}`, '_blank', 'noopener,noreferrer');
  };

  const getPaymentMethodLabel = () => {
    switch (paymentMethod) {
      case 'yape':
        return 'Yape';
      case 'plin':
        return 'Plin';
      case 'transferencia':
        return 'Transferencia Bancaria BCP';
      default:
        return 'Pago Digital';
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/75 backdrop-blur-xs"
            aria-hidden="true"
          />

          {/* Modal Container */}
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 20 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="relative w-full max-w-lg bg-[#faf7f2] rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[92vh] border border-stone-200"
          >
            {/* Top Bar Header */}
            <div className="p-4 sm:p-5 bg-stone-900 text-white flex items-center justify-between shadow-md">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-[#005141] text-emerald-300 flex items-center justify-center font-mono font-black text-sm shadow-sm ring-1 ring-white/10">
                  {storeName.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm sm:text-base font-black tracking-tight">{storeName}</h3>
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  </div>
                  <div className="text-[11px] text-stone-300 flex items-center gap-1.5 font-mono">
                    <span>Pedido #{orderId}</span>
                    <span>•</span>
                    <span className="text-emerald-400 font-bold flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                      En Vivo
                    </span>
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

            {/* View Switcher Tabs (Tracker vs. Boarding Pass Receipt) */}
            <div className="bg-white px-5 pt-3 border-b border-stone-200 flex items-center gap-3">
              <button
                type="button"
                onClick={() => setActiveTab('TRACKER')}
                className={`pb-2.5 text-xs font-extrabold transition-all border-b-2 cursor-pointer flex items-center gap-1.5 ${
                  activeTab === 'TRACKER'
                    ? 'border-[#005141] text-[#005141]'
                    : 'border-transparent text-[#78716c] hover:text-[#1c1917]'
                }`}
              >
                <Clock className="w-3.5 h-3.5" />
                <span>Seguimiento en Vivo (4 Pasos)</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('RECEIPT')}
                className={`pb-2.5 text-xs font-extrabold transition-all border-b-2 cursor-pointer flex items-center gap-1.5 ${
                  activeTab === 'RECEIPT'
                    ? 'border-[#005141] text-[#005141]'
                    : 'border-transparent text-[#78716c] hover:text-[#1c1917]'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
                <span>Recibo Boarding Pass</span>
              </button>
            </div>

            {/* Scrollable Content Body */}
            <div className="p-4 sm:p-5 overflow-y-auto space-y-5 flex-1">
              {activeTab === 'TRACKER' ? (
                /* TAB 1: LIVE 4-STEP TRACKER */
                <div className="space-y-5">
                  {/* Status Banner */}
                  <div className="bg-white p-4 rounded-2xl border border-stone-200 shadow-xs flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#005141] block">
                        Estado Actual
                      </span>
                      <h4 className="text-base font-black text-[#1c1917] mt-0.5">
                        {steps[currentStepIndex].label}
                      </h4>
                      <p className="text-xs text-[#57534e] mt-0.5">
                        {steps[currentStepIndex].description}
                      </p>
                    </div>
                    <Badge variant="jade" size="sm" className="font-mono font-bold shadow-2xs">
                      Paso {currentStepIndex + 1} de 4
                    </Badge>
                  </div>

                  {/* 4-Step Vertical Stepper */}
                  <div className="bg-white p-5 rounded-2xl border border-stone-200 shadow-xs space-y-4">
                    {steps.map((step, idx) => {
                      const isPast = idx < currentStepIndex;
                      const isCurrent = idx === currentStepIndex;

                      return (
                        <div key={step.id} className="relative flex items-start gap-4">
                          {/* Connector line */}
                          {idx < steps.length - 1 && (
                            <div
                              className={`absolute left-4 top-8 -bottom-4 w-0.5 transition-colors duration-300 ${
                                isPast ? 'bg-[#005141]' : 'bg-stone-200'
                              }`}
                            />
                          )}

                          {/* Step Icon Node */}
                          <div
                            className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 z-10 transition-all ${
                              isPast
                                ? 'bg-[#005141] text-white shadow-xs'
                                : isCurrent
                                ? 'bg-amber-100 text-amber-900 ring-4 ring-amber-100 shadow-md scale-110'
                                : 'bg-stone-100 text-stone-400 border border-stone-300'
                            }`}
                          >
                            {isPast ? <Check className="w-4 h-4 text-white" /> : step.icon}
                          </div>

                          {/* Step Text Info */}
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <h5
                                className={`text-xs sm:text-sm font-extrabold leading-snug ${
                                  isCurrent
                                    ? 'text-[#1c1917]'
                                    : isPast
                                    ? 'text-[#005141]'
                                    : 'text-[#78716c]'
                                }`}
                              >
                                {step.label}
                              </h5>
                              <span className="text-[10px] font-mono font-bold text-[#78716c]">
                                {step.estimatedTime}
                              </span>
                            </div>
                            <p className="text-[11px] text-[#57534e] mt-0.5 font-medium">
                              {step.sublabel}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Interactive Status Switcher for Demo / Testing */}
                  <div className="p-3.5 bg-stone-100/80 rounded-2xl border border-stone-200 space-y-2">
                    <div className="flex items-center justify-between text-[11px] font-bold text-stone-600">
                      <span>Simulador de estados para demostración:</span>
                    </div>
                    <div className="grid grid-cols-4 gap-1.5">
                      {stepOrder.map((stepKey, idx) => (
                        <button
                          key={stepKey}
                          type="button"
                          onClick={() => setCurrentStep(stepKey)}
                          className={`py-1.5 px-1 rounded-xl text-[10px] font-extrabold cursor-pointer transition-all text-center ${
                            currentStep === stepKey
                              ? 'bg-[#005141] text-white shadow-xs'
                              : 'bg-white hover:bg-stone-200 text-stone-700 border border-stone-200'
                          }`}
                        >
                          Paso {idx + 1}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              ) : (
                /* TAB 2: ARTISANAL BOARDING PASS DIGITAL RECEIPT */
                <div className="relative bg-white rounded-3xl border border-stone-300 shadow-xl overflow-hidden">
                  {/* Left & Right Perforation Cutout Notches */}
                  <div className="absolute top-[138px] -left-3.5 w-7 h-7 rounded-full bg-[#faf7f2] border-r border-stone-300 z-10" />
                  <div className="absolute top-[138px] -right-3.5 w-7 h-7 rounded-full bg-[#faf7f2] border-l border-stone-300 z-10" />

                  {/* Boarding Pass Header */}
                  <div className="p-5 bg-gradient-to-r from-stone-900 to-stone-800 text-white relative">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-black tracking-wider uppercase text-amber-400">
                          JaldiShop Boarding Pass
                        </span>
                        <span className="text-white/40">•</span>
                        <span className="text-[10px] text-stone-300 font-mono">
                          TICKET #{orderId}
                        </span>
                      </div>
                      <Badge variant="amber" size="sm" className="font-bold">
                        Válido Hoy
                      </Badge>
                    </div>

                    <div className="mt-4 flex items-center justify-between">
                      <div>
                        <div className="text-[10px] uppercase font-bold text-stone-400">Comercio</div>
                        <div className="text-base font-black text-white">{storeName}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-[10px] uppercase font-bold text-stone-400">Entrega</div>
                        <div className="text-xs font-black text-emerald-400">
                          {fulfillmentType === 'DELIVERY' ? '🛵 A Domicilio' : '🏪 Retiro Local'}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Slot Time Highlight Bar */}
                  <div className="p-3.5 bg-amber-50 border-b border-dashed border-amber-200 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <Clock className="w-4 h-4 text-[#ea580c]" />
                      <span className="font-bold text-[#78350f]">Franja Asignada:</span>
                      <strong className="font-mono font-black text-[#92400e]">
                        {selectedSlot.timeRange}
                      </strong>
                    </div>
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-200">
                      Cupo Verificado
                    </span>
                  </div>

                  {/* Perforation Line */}
                  <div className="relative py-2 border-b-2 border-dashed border-stone-200 mx-5" />

                  {/* Items Breakdown */}
                  <div className="p-5 space-y-4">
                    <div className="text-[11px] font-bold text-[#78716c] uppercase tracking-wider">
                      Detalle de Productos Frescos
                    </div>

                    <div className="space-y-2.5">
                      {cart.map((item) => (
                        <div
                          key={item.product.id}
                          className="flex items-start justify-between text-xs gap-3"
                        >
                          <div className="flex-1 min-w-0">
                            <span className="font-bold text-[#1c1917]">
                              {item.quantity}x {item.product.name}
                            </span>
                            {item.notes && (
                              <p className="text-[10px] text-stone-500 italic mt-0.5">
                                Nota: {item.notes}
                              </p>
                            )}
                          </div>
                          <span className="font-mono font-bold text-[#1c1917] shrink-0">
                            {formatCurrency(item.product.price * item.quantity)}
                          </span>
                        </div>
                      ))}
                    </div>

                    {/* Financial Summary */}
                    <div className="pt-3 border-t border-stone-100 space-y-1.5 text-xs text-[#57534e]">
                      <div className="flex justify-between">
                        <span>Subtotal</span>
                        <span className="font-mono font-bold text-[#1c1917]">
                          {formatCurrency(subtotal)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>
                          {fulfillmentType === 'DELIVERY' ? 'Envío coordinado' : 'Retiro en tienda'}
                        </span>
                        <span className="font-mono font-bold text-[#1c1917]">
                          {deliveryFee === 0 ? 'Gratis' : formatCurrency(deliveryFee)}
                        </span>
                      </div>
                      <div className="flex justify-between text-sm font-black text-[#1c1917] pt-2 border-t border-stone-200">
                        <span>Total Pagado</span>
                        <span className="font-mono text-base text-[#005141] font-black">
                          {formatCurrency(total)}
                        </span>
                      </div>
                    </div>

                    {/* Payment Method & Security Seal */}
                    <div className="p-3 bg-[#faf7f2] rounded-2xl border border-stone-200 flex items-center justify-between text-xs">
                      <div>
                        <div className="text-[10px] text-[#78716c] font-medium">Método de Pago:</div>
                        <div className="font-bold text-[#1c1917]">{getPaymentMethodLabel()}</div>
                      </div>
                      <div className="w-8 h-8 rounded-xl bg-white border border-stone-300 flex items-center justify-center font-bold text-xs text-[#005141] shadow-2xs">
                        ✓
                      </div>
                    </div>

                    {/* QR Code Security Mock */}
                    <div className="flex items-center gap-3 p-3 bg-stone-50 rounded-2xl border border-dashed border-stone-300">
                      <div className="w-14 h-14 bg-white rounded-xl border border-stone-200 p-1 flex items-center justify-center shrink-0 shadow-2xs">
                        {/* Vector QR Representation */}
                        <svg className="w-full h-full text-stone-900" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M2 2h8v8H2V2zm2 2v4h4V4H4zm10-2h8v8h-8V2zm2 2v4h4V4h-4zM2 14h8v8H2v-8zm2 2v4h4v-4H4zm8-2h2v2h-2v-2zm4 0h2v2h-2v-2zm-4 4h2v2h-2v-2zm4 0h2v2h-2v-2zm2-2h2v2h-2v-2zm2 2h2v4h-2v-4zm-4 2h2v2h-2v-2zm-6 0h2v2h-2v-2z" />
                        </svg>
                      </div>
                      <div className="text-left leading-tight">
                        <div className="text-[11px] font-bold text-[#1c1917] flex items-center gap-1">
                          <span>Código QR de Verificación</span>
                          <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
                        </div>
                        <p className="text-[10px] text-[#78716c] mt-0.5">
                          Presenta este recibo o escanea el código en mostrador al retirar tu pedido.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Bottom Modal Actions */}
            <div className="p-4 bg-white border-t border-stone-200 flex flex-col sm:flex-row items-center gap-2.5">
              <button
                type="button"
                onClick={handleCopyLink}
                className="w-full sm:w-auto px-4 py-3 rounded-2xl bg-stone-100 hover:bg-stone-200 text-[#1c1917] text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                {copiedLink ? <Check className="w-3.5 h-3.5 text-[#005141]" /> : <Share2 className="w-3.5 h-3.5" />}
                <span>{copiedLink ? 'Enlace copiado' : 'Compartir'}</span>
              </button>

              <Button
                type="button"
                variant="primary"
                size="md"
                onClick={handleOpenSupport}
                className="w-full sm:flex-1 py-3 rounded-2xl font-black text-xs sm:text-sm bg-[#005141] hover:bg-[#00382d] text-white border-0 shadow-lg shadow-[#005141]/20 cursor-pointer flex items-center justify-center gap-2"
              >
                <span>Consultar con el Local</span>
                <ExternalLink className="w-3.5 h-3.5 ml-0.5 opacity-80" />
              </Button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

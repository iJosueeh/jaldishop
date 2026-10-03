'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  CreditCard,
  QrCode,
  Copy,
  Check,
  Upload,
  ArrowRight,
  ShieldCheck,
  Clock,
  FileText,
  Building2,
  Lock,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';

export type PaymentMethod = 'yape' | 'plin' | 'transferencia';

export interface PaymentHubModalProps {
  isOpen: boolean;
  onClose: () => void;
  onProceedToWhatsApp: (paymentDetails: {
    method: PaymentMethod;
    receiptFile: string | null;
    total: number;
  }) => void;
  total: number;
  storeName?: string;
  slotTime?: string;
  countdownSeconds?: number;
}

export function PaymentHubModal({
  isOpen,
  onClose,
  onProceedToWhatsApp,
  total,
  storeName = 'Panadería Don Pepe',
  slotTime = '10:00 - 11:00 AM',
  countdownSeconds = 600,
}: PaymentHubModalProps) {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('yape');
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [receiptFile, setReceiptFile] = useState<string | null>(null);
  const [receiptFileName, setReceiptFileName] = useState<string | null>(null);

  const merchantPhone = '987 654 321';
  const merchantHolder = `${storeName} SAC`;
  const bcpAccount = '194-98765432-0-12';
  const bcpCci = '00219400987654320124';

  const handleCopy = (text: string, fieldName: string) => {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text);
    }
    setCopiedField(fieldName);
    toast.success(`¡${fieldName} copiado al portapapeles!`);
    setTimeout(() => setCopiedField(null), 2500);
  };

  const handleSimulateUpload = () => {
    setReceiptFile('/images/mock-yape-receipt.png');
    setReceiptFileName(`comprobante-${selectedMethod}-S${total.toFixed(2)}.jpg`);
    toast.success('¡Comprobante adjuntado con éxito!', {
      description: 'Tu pago está listo para ser validado por el comercio.',
    });
  };

  const handleRemoveReceipt = () => {
    setReceiptFile(null);
    setReceiptFileName(null);
  };

  const minutes = Math.floor(countdownSeconds / 60);
  const seconds = countdownSeconds % 60;
  const formattedCountdown = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

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
            className="fixed inset-0 bg-black/70 backdrop-blur-xs"
            aria-hidden="true"
          />

          {/* Modal Content */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[92vh]"
          >
            {/* Modal Header */}
            <div className="p-5 sm:p-6 pb-4 border-b border-stone-200 flex items-center justify-between bg-white sticky top-0 z-10">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-[#005141] text-white flex items-center justify-center shadow-xs">
                  <CreditCard className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-[#1c1917] tracking-tight">
                    Pagar y Confirmar Pedido
                  </h3>
                  <p className="text-xs text-[#57534e]">
                    Transfiere con tu billetera digital o banco preferido
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-xl bg-stone-100 hover:bg-stone-200 text-stone-500 hover:text-[#1c1917] cursor-pointer transition-colors"
                aria-label="Cerrar modal de pago"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Hold Guarantee & Amount Banner */}
            <div className="bg-[#faf7f2] px-6 py-3.5 border-b border-stone-200 flex items-center justify-between">
              <div>
                <span className="text-[11px] font-bold text-[#57534e] uppercase tracking-wider block">
                  Total a Transferir
                </span>
                <span className="text-xl sm:text-2xl font-black text-[#005141] font-mono leading-none">
                  {formatCurrency(total)}
                </span>
              </div>

              {/* Countdown Pill */}
              <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-stone-200 shadow-2xs">
                <Clock className="w-3.5 h-3.5 text-[#ea580c] animate-pulse" />
                <div className="text-right">
                  <div className="text-[9px] font-extrabold uppercase text-[#ea580c]">10 min para pagar</div>
                  <div className="font-mono font-black text-xs text-[#1c1917]">{formattedCountdown} restantes</div>
                </div>
              </div>
            </div>

            {/* Scrollable Body */}
            <div className="p-5 sm:p-6 overflow-y-auto space-y-5">
              {/* Payment Method Selector Tabs */}
              <div className="grid grid-cols-3 gap-2 p-1.5 bg-[#faf7f2] rounded-2xl border border-stone-200">
                {/* Yape */}
                <button
                  type="button"
                  onClick={() => setSelectedMethod('yape')}
                  className={`py-2.5 px-2 rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                    selectedMethod === 'yape'
                      ? 'bg-[#742284] text-white shadow-md scale-[1.01]'
                      : 'text-stone-600 hover:text-stone-900 hover:bg-white/60'
                  }`}
                >
                  <span className="w-2.5 h-2.5 rounded-full bg-[#00d4b8]" />
                  <span>Yape</span>
                </button>

                {/* Plin */}
                <button
                  type="button"
                  onClick={() => setSelectedMethod('plin')}
                  className={`py-2.5 px-2 rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                    selectedMethod === 'plin'
                      ? 'bg-[#00a4e4] text-white shadow-md scale-[1.01]'
                      : 'text-stone-600 hover:text-stone-900 hover:bg-white/60'
                  }`}
                >
                  <span className="w-2.5 h-2.5 rounded-full bg-[#002f6c]" />
                  <span>Plin</span>
                </button>

                {/* Transferencia */}
                <button
                  type="button"
                  onClick={() => setSelectedMethod('transferencia')}
                  className={`py-2.5 px-2 rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                    selectedMethod === 'transferencia'
                      ? 'bg-[#005141] text-white shadow-md scale-[1.01]'
                      : 'text-stone-600 hover:text-stone-900 hover:bg-white/60'
                  }`}
                >
                  <Building2 className="w-3.5 h-3.5" />
                  <span>Banco / CCI</span>
                </button>
              </div>

              {/* Dynamic Payment Details Area */}
              <div className="p-4 rounded-2xl bg-[#faf7f2] border border-stone-200/90 space-y-4">
                {selectedMethod === 'yape' && (
                  <div className="space-y-3.5">
                    <div className="flex items-center justify-between pb-2 border-b border-stone-200">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-black text-[#742284] bg-purple-50 px-2.5 py-0.5 rounded-md border border-purple-200">
                          Paga con Yape
                        </span>
                        <span className="text-xs text-stone-500 font-medium">Titular: <strong>{merchantHolder}</strong></span>
                      </div>
                      <ShieldCheck className="w-4 h-4 text-[#005141]" />
                    </div>

                    <div className="flex flex-col sm:flex-row items-center gap-4 bg-white p-3 rounded-2xl border border-stone-200">
                      {/* Stylized QR Code Placeholder */}
                      <div className="w-28 h-28 bg-purple-50 rounded-xl border border-purple-200 flex flex-col items-center justify-center p-2 text-center shrink-0">
                        <QrCode className="w-14 h-14 text-[#742284]" />
                        <span className="text-[9px] font-black text-[#742284] uppercase mt-1">Escanea QR</span>
                      </div>

                      {/* Phone & Copy Action */}
                      <div className="space-y-2 flex-1 w-full text-center sm:text-left">
                        <div className="text-xs text-stone-500 font-medium">Número para yapear:</div>
                        <div className="font-mono font-black text-xl text-[#1c1917] tracking-wider">
                          {merchantPhone}
                        </div>
                        <button
                          type="button"
                          onClick={() => handleCopy(merchantPhone.replace(/\s/g, ''), 'Número Yape')}
                          className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-100 hover:bg-purple-200 text-[#742284] font-bold text-xs cursor-pointer transition-colors"
                        >
                          {copiedField === 'Número Yape' ? (
                            <>
                              <Check className="w-3.5 h-3.5" /> Copiado
                            </>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5" /> Copiar número
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {selectedMethod === 'plin' && (
                  <div className="space-y-3.5">
                    <div className="flex items-center justify-between pb-2 border-b border-stone-200">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-black text-[#00a4e4] bg-sky-50 px-2.5 py-0.5 rounded-md border border-sky-200">
                          Paga con Plin
                        </span>
                        <span className="text-xs text-stone-500 font-medium">Titular: <strong>{merchantHolder}</strong></span>
                      </div>
                      <ShieldCheck className="w-4 h-4 text-[#005141]" />
                    </div>

                    <div className="flex flex-col sm:flex-row items-center gap-4 bg-white p-3 rounded-2xl border border-stone-200">
                      <div className="w-28 h-28 bg-sky-50 rounded-xl border border-sky-200 flex flex-col items-center justify-center p-2 text-center shrink-0">
                        <QrCode className="w-14 h-14 text-[#00a4e4]" />
                        <span className="text-[9px] font-black text-[#00a4e4] uppercase mt-1">Escanea QR</span>
                      </div>

                      <div className="space-y-2 flex-1 w-full text-center sm:text-left">
                        <div className="text-xs text-stone-500 font-medium">Número para Plin (Interbank/Scotiabank/BBVA):</div>
                        <div className="font-mono font-black text-xl text-[#1c1917] tracking-wider">
                          {merchantPhone}
                        </div>
                        <button
                          type="button"
                          onClick={() => handleCopy(merchantPhone.replace(/\s/g, ''), 'Número Plin')}
                          className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-100 hover:bg-sky-200 text-[#00a4e4] font-bold text-xs cursor-pointer transition-colors"
                        >
                          {copiedField === 'Número Plin' ? (
                            <>
                              <Check className="w-3.5 h-3.5" /> Copiado
                            </>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5" /> Copiar número
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {selectedMethod === 'transferencia' && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between pb-2 border-b border-stone-200">
                      <span className="text-xs font-black text-[#005141] bg-teal-50 px-2.5 py-0.5 rounded-md border border-teal-200">
                        Transferencia Bancaria BCP
                      </span>
                      <span className="text-xs text-stone-500 font-medium">Titular: <strong>{merchantHolder}</strong></span>
                    </div>

                    <div className="space-y-2.5 bg-white p-3.5 rounded-2xl border border-stone-200 text-xs">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-[10px] text-stone-500 font-bold block">Cuenta Corriente Soles BCP:</span>
                          <span className="font-mono font-black text-stone-800 text-sm">{bcpAccount}</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleCopy(bcpAccount.replace(/-/g, ''), 'Cuenta BCP')}
                          className="p-1.5 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-700 font-bold text-[11px] flex items-center gap-1 cursor-pointer"
                        >
                          {copiedField === 'Cuenta BCP' ? <Check className="w-3 h-3 text-[#005141]" /> : <Copy className="w-3 h-3" />}
                        </button>
                      </div>

                      <div className="flex items-center justify-between pt-2 border-t border-stone-100">
                        <div>
                          <span className="text-[10px] text-stone-500 font-bold block">Código Interbancario (CCI):</span>
                          <span className="font-mono font-black text-stone-800 text-sm">{bcpCci}</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleCopy(bcpCci, 'CCI')}
                          className="p-1.5 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-700 font-bold text-[11px] flex items-center gap-1 cursor-pointer"
                        >
                          {copiedField === 'CCI' ? <Check className="w-3 h-3 text-[#005141]" /> : <Copy className="w-3 h-3" />}
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Receipt Attachment / Simulation Area */}
              <div className="space-y-2.5">
                <label className="text-xs font-extrabold text-[#1c1917] flex items-center justify-between">
                  <span>Adjuntar Comprobante de Pago:</span>
                  <span className="text-[11px] font-semibold text-stone-500">Obligatorio para despacho</span>
                </label>

                {receiptFile ? (
                  <div className="p-3.5 rounded-2xl bg-[#f0fdfa] border border-teal-300 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-[#005141] text-white flex items-center justify-center">
                        <FileText className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="text-xs font-black text-[#1c1917] truncate max-w-[200px]">
                          {receiptFileName}
                        </div>
                        <div className="text-[10px] text-[#005141] font-bold flex items-center gap-1 mt-0.5">
                          <Check className="w-3 h-3" /> Comprobante adjuntado con éxito
                        </div>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={handleRemoveReceipt}
                      className="p-1.5 rounded-lg bg-white text-stone-400 hover:text-rose-600 border border-stone-200 transition-colors cursor-pointer"
                      title="Quitar comprobante"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div className="border-2 border-dashed border-stone-300 hover:border-[#005141] rounded-2xl p-5 text-center space-y-2 bg-[#faf7f2]/60 transition-colors">
                    <Upload className="w-7 h-7 text-[#005141] mx-auto opacity-80" />
                    <div className="text-xs text-stone-600">
                      Arrastra tu captura de pantalla o foto del comprobante aquí
                    </div>
                    <button
                      type="button"
                      onClick={handleSimulateUpload}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-[#005141] hover:bg-[#00382d] text-white rounded-xl text-xs font-bold shadow-xs cursor-pointer transition-colors"
                    >
                      <FileText className="w-3.5 h-3.5" />
                      <span>Simular Subida de Comprobante</span>
                    </button>
                  </div>
                )}
              </div>

              {/* Slot Protection Guarantee */}
              <div className="p-3 bg-[#f0fdfa] rounded-xl border border-teal-200/80 flex items-start gap-2.5 text-xs text-[#065f46]">
                <Lock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
                <div className="leading-relaxed">
                  Tu turno para la franja <strong className="font-bold text-[#005141]">{slotTime}</strong> está apartado. Al pulsar en continuar, se redactará el mensaje oficial para WhatsApp.
                </div>
              </div>
            </div>

            {/* Modal Sticky Footer CTA */}
            <div className="p-5 border-t border-stone-200 bg-white sticky bottom-0 z-10 flex flex-col sm:flex-row items-center gap-3">
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={onClose}
                className="w-full sm:w-auto text-xs font-bold rounded-2xl border-stone-300"
              >
                Volver al carrito
              </Button>

              <Button
                type="button"
                variant="terracotta"
                size="md"
                onClick={() => {
                  onProceedToWhatsApp({
                    method: selectedMethod,
                    receiptFile,
                    total,
                  });
                }}
                className="w-full sm:flex-1 py-3.5 rounded-2xl font-black text-xs shadow-lg shadow-[#ea580c]/30 cursor-pointer border-0 bg-[#ea580c] hover:bg-[#c2410c] flex items-center justify-center gap-2"
              >
                <span>Confirmar Pedido por WhatsApp</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

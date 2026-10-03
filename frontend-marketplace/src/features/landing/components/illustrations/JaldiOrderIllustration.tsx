import React from 'react';
import { ShieldCheck, CheckCircle2, Clock, MessageSquare } from 'lucide-react';

export function JaldiOrderIllustration() {
  return (
    <div className="relative w-full max-w-md mx-auto my-4 select-none">
      {/* Decorative emerald aura */}
      <div className="absolute inset-0 bg-[#005141]/10 rounded-3xl blur-xl" />

      {/* Main Mockup Card / Ticket Terminal */}
      <div className="relative bg-white rounded-3xl border-2 border-emerald-300 shadow-xl shadow-[#005141]/10 p-4 space-y-3.5 overflow-hidden">
        {/* Device Top Bar */}
        <div className="flex items-center justify-between pb-2 border-b border-emerald-100 text-xs">
          <div className="flex items-center gap-2 text-[#005141] font-black">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
            <span>Terminal JaldiShop Pro</span>
          </div>
          <span className="px-2 py-0.5 rounded-md bg-[#f0fdfa] text-[#005141] font-mono text-[10px] font-bold border border-teal-200">
            ✓ 0 Sobreventas
          </span>
        </div>

        {/* Capacity Timeline Slot Progress */}
        <div className="p-3 bg-[#faf7f2] rounded-2xl border border-stone-200 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-extrabold text-[#1c1917] flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-[#ea580c]" />
              Franja: 10:00 - 11:00 AM
            </span>
            <span className="font-mono font-black text-[#005141] text-[11px] bg-white px-2 py-0.5 rounded-lg border border-teal-100">
              8/10 cupos (80%)
            </span>
          </div>

          <div className="w-full h-2 bg-stone-200 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-[#005141] to-[#feae2c] w-4/5 rounded-full" />
          </div>

          <div className="flex items-center justify-between text-[10px] text-stone-500 font-medium">
            <span>2 cupos disponibles para horneado</span>
            <span className="text-emerald-700 font-bold">Bloqueo automático al 100%</span>
          </div>
        </div>

        {/* Structured Order Ticket with Perforations */}
        <div className="relative bg-[#f0fdfa] rounded-2xl border border-teal-200 p-3.5 space-y-2.5 overflow-hidden">
          {/* Ticket Notches */}
          <div className="absolute top-[48px] -left-2 w-4 h-4 rounded-full bg-white border-r border-teal-200" />
          <div className="absolute top-[48px] -right-2 w-4 h-4 rounded-full bg-white border-l border-teal-200" />

          {/* Ticket Header */}
          <div className="flex items-center justify-between text-xs pb-1.5 border-b border-teal-200/80">
            <div className="flex items-center gap-1.5 font-bold text-[#1c1917]">
              <span>#JALDI-1048</span>
              <span className="text-stone-300">•</span>
              <span className="text-[#005141]">María R. (Miraflores)</span>
            </div>
            <span className="text-[10px] font-bold text-amber-800 bg-amber-100 px-2 py-0.5 rounded-full border border-amber-200">
              10 min de reserva
            </span>
          </div>

          {/* Clean Order Breakdown */}
          <div className="space-y-1 text-xs">
            <div className="flex justify-between font-bold text-[#1c1917]">
              <span>2x Hogaza de Masa Madre 24h</span>
              <span className="font-mono">S/ 28.00</span>
            </div>
            <div className="flex justify-between font-bold text-[#1c1917]">
              <span>1x Croissant Francés de Mantequilla</span>
              <span className="font-mono">S/ 8.50</span>
            </div>
          </div>

          {/* Verified Payment Seal */}
          <div className="pt-2 border-t border-dashed border-teal-300 flex items-center justify-between text-xs">
            <div className="flex items-center gap-1.5 text-[#005141] font-extrabold text-[11px]">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>Yape / Mercado Pago • Acreditado</span>
            </div>
            <span className="font-mono font-black text-sm text-[#005141]">
              Total: S/ 36.50
            </span>
          </div>
        </div>

        {/* WhatsApp Bridge Structured Action */}
        <div className="p-2.5 bg-emerald-50 rounded-2xl border border-emerald-200 flex items-center justify-between text-xs text-emerald-800">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-xl bg-[#25d366] text-white flex items-center justify-center font-bold shadow-xs">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <strong className="block font-black text-[#005141] leading-tight">
                WhatsApp Order Bridge
              </strong>
              <span className="text-[10px] text-emerald-700">Comanda limpia enviada en 1 clic</span>
            </div>
          </div>
          <span className="text-[10px] font-bold text-[#005141] flex items-center gap-0.5">
            <span>Listo</span>
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          </span>
        </div>
      </div>
    </div>
  );
}

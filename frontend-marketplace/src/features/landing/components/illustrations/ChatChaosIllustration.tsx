import React from 'react';
import { Clock, Mic, Flame, HelpCircle } from 'lucide-react';

export function ChatChaosIllustration() {
  return (
    <div className="relative w-full max-w-md mx-auto my-4 select-none">
      {/* Decorative red warning aura */}
      <div className="absolute inset-0 bg-rose-500/10 rounded-3xl blur-xl" />

      {/* Main Mockup Phone / Chat Container */}
      <div className="relative bg-[#fcf8f8] rounded-3xl border-2 border-rose-200/90 p-4 shadow-xl shadow-rose-900/10 space-y-3 overflow-hidden transform rotate-[-0.5deg]">
        {/* Device Top Bar */}
        <div className="flex items-center justify-between pb-2 border-b border-rose-100 text-xs">
          <div className="flex items-center gap-2 text-rose-700 font-extrabold">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
            <span>Chat Desordenado (42 sin responder)</span>
          </div>
          <span className="px-2 py-0.5 rounded-md bg-rose-100 text-rose-700 font-mono text-[10px] font-bold">
            🚨 Saturado
          </span>
        </div>

        {/* Message 1: 3-Minute Voice Note */}
        <div className="flex items-start gap-2">
          <div className="w-7 h-7 rounded-full bg-stone-300 text-stone-700 flex items-center justify-center text-xs font-bold shrink-0">
            C1
          </div>
          <div className="bg-white rounded-2xl rounded-tl-xs p-3 shadow-xs border border-rose-100/90 text-xs space-y-1.5 flex-1 max-w-[85%]">
            <div className="flex items-center justify-between text-[10px] text-stone-400 font-medium">
              <span>Cliente #1</span>
              <span>10:14 AM</span>
            </div>
            {/* Audio Waveform Mockup */}
            <div className="flex items-center gap-2 bg-stone-50 p-2 rounded-xl border border-stone-200">
              <div className="w-6 h-6 rounded-full bg-rose-500 text-white flex items-center justify-center shrink-0">
                <Mic className="w-3 h-3" />
              </div>
              <div className="flex-1 flex items-center gap-0.5 h-4">
                {[40, 70, 90, 40, 80, 100, 60, 30, 80, 50, 90, 40, 70, 30].map((h, i) => (
                  <span
                    key={i}
                    className="w-1 bg-rose-400 rounded-full"
                    style={{ height: `${h}%` }}
                  />
                ))}
              </div>
              <span className="text-[10px] font-mono font-bold text-rose-600">03:42</span>
            </div>
            <p className="text-[10px] text-stone-500 italic">
              “...hola te mandé un audio larguito con los 15 croissants y 3 panes pero para ya...”
            </p>
          </div>
        </div>

        {/* Message 2: Questionable Payment Slip */}
        <div className="flex items-start gap-2 justify-end">
          <div className="bg-rose-50 rounded-2xl rounded-tr-xs p-3 border border-rose-200 text-xs space-y-1 flex-1 max-w-[82%]">
            <div className="flex items-center justify-between text-[10px] text-rose-600 font-bold">
              <span className="flex items-center gap-1">
                <HelpCircle className="w-3 h-3 text-rose-500" /> Comprobante borroso
              </span>
              <span>10:16 AM</span>
            </div>
            <div className="h-10 bg-white rounded-lg border border-dashed border-rose-300 flex items-center justify-center text-[10px] text-rose-700 font-medium">
              [Captura Yape S/ 45.00 sin fecha clara]
            </div>
            <p className="text-[10px] text-rose-800 font-semibold">
              ¿Este comprobante es de hoy o de ayer? 🤔
            </p>
          </div>
        </div>

        {/* Message 3: Stock Overbooking & Panic */}
        <div className="p-2.5 rounded-2xl bg-rose-100/90 border border-rose-300 flex items-center gap-2.5 text-xs text-rose-800">
          <Flame className="w-5 h-5 text-rose-600 shrink-0 animate-bounce" />
          <div className="leading-tight">
            <strong className="block font-black text-rose-900">¡Sobreventa detectada!</strong>
            <span className="text-[11px]">
              Se vendieron 18 croissants cuando solo quedaban 6 en el horno.
            </span>
          </div>
        </div>

        {/* Mini Capacity Gauge Overflow Warning */}
        <div className="pt-1 flex items-center justify-between text-[10px] text-rose-700 font-bold">
          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3 text-rose-500" /> Franja 10:00 - 11:00 AM
          </span>
          <span className="bg-rose-200 px-2 py-0.5 rounded-full font-mono">
            18/10 pedidos (+180% saturado)
          </span>
        </div>
      </div>
    </div>
  );
}

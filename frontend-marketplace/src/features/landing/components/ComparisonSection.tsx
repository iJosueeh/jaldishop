'use client';

import React, { useState } from 'react';
import { Container } from '@/shared/components/ui/Container';
import { Card } from '@/shared/components/ui/Card';
import {
  XCircle,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
  MessageCircle,
  ShieldCheck,
  SplitSquareVertical,
} from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';
import { ChatChaosIllustration } from './illustrations/ChatChaosIllustration';
import { JaldiOrderIllustration } from './illustrations/JaldiOrderIllustration';
import { motion } from 'framer-motion';

type ViewMode = 'BOTH' | 'CHAOS' | 'JALDI';

export function ComparisonSection() {
  const [viewMode, setViewMode] = useState<ViewMode>('BOTH');

  return (
    <section id="antes-despues" className="py-16 sm:py-24 bg-[#faf7f2] relative overflow-hidden scroll-mt-20 sm:scroll-mt-24">
      {/* Subtle Background Glow Accents */}
      <div className="absolute top-1/3 left-10 w-96 h-96 bg-[#ea580c]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-10 w-96 h-96 bg-[#005141]/5 rounded-full blur-3xl pointer-events-none" />

      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-10 sm:mb-12">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white border border-stone-200 shadow-xs text-xs font-bold text-[#ea580c]">
            <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
            La Transformación Operativa
          </div>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-[#1c1917] leading-tight sm:leading-snug">
            ¿Por qué vender por chat tradicional te cuesta dinero y clientes?
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-[#57534e] max-w-xl mx-auto">
            Compara el estrés diario de coordinar pedidos manualmente frente a la tranquilidad de un sistema con capacidad inteligente.
          </p>

          {/* Interactive Mode Switcher Tabs */}
          <div className="pt-3 flex items-center justify-center w-full px-2">
            <div className="inline-flex max-w-full overflow-x-auto no-scrollbar p-1.5 bg-stone-200/80 rounded-2xl border border-stone-300/80 shadow-inner gap-1">
              <button
                type="button"
                onClick={() => setViewMode('BOTH')}
                className={`px-2.5 sm:px-3.5 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'BOTH'
                    ? 'bg-white text-[#1c1917] shadow-sm'
                    : 'text-stone-600 hover:text-[#1c1917]'
                }`}
              >
                <SplitSquareVertical className="w-3.5 h-3.5 text-[#005141]" />
                <span><span className="hidden sm:inline">Comparar </span>Ambos</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('CHAOS')}
                className={`px-2.5 sm:px-3.5 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'CHAOS'
                    ? 'bg-rose-600 text-white shadow-sm'
                    : 'text-stone-600 hover:text-rose-700'
                }`}
              >
                <span>💥 <span className="hidden sm:inline">El </span>Caos</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('JALDI')}
                className={`px-2.5 sm:px-3.5 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'JALDI'
                    ? 'bg-[#005141] text-white shadow-sm'
                    : 'text-stone-600 hover:text-[#005141]'
                }`}
              >
                <span>✨ <span className="hidden sm:inline">El </span>Orden</span>
              </button>
            </div>
          </div>
        </div>

        {/* Comparison Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 items-stretch">
          {/* Left Card: The Chaos of WhatsApp */}
          {(viewMode === 'BOTH' || viewMode === 'CHAOS') && (
            <motion.div
              layout
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              transition={{ duration: 0.25 }}
              className={`h-full ${viewMode === 'CHAOS' ? 'lg:col-span-2 max-w-2xl mx-auto w-full' : ''}`}
            >
              <Card className="p-6 sm:p-8 h-full bg-white shadow-xl shadow-stone-900/5 relative overflow-hidden flex flex-col justify-between rounded-3xl border border-rose-200 hover:border-rose-300 transition-all duration-300">
                <div className="space-y-5">
                  {/* Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-rose-100">
                    <div className="flex items-center gap-3">
                      <div className="w-11 h-11 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold shrink-0 border border-rose-200/60 shadow-xs">
                        <AlertTriangle className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="font-display text-base sm:text-lg font-bold text-[#1c1917]">
                          Venta por Chat Desordenado
                        </h3>
                        <p className="text-xs text-rose-600 font-semibold">El cuello de botella de las MYPE</p>
                      </div>
                    </div>
                    <div className="self-start sm:self-auto">
                      <Badge variant="danger" size="sm">
                        Fricción Operativa
                      </Badge>
                    </div>
                  </div>

                  {/* CSS Craft Illustration: The Chaos */}
                  <ChatChaosIllustration />

                  {/* Bullet points */}
                  <ul className="space-y-3 text-xs sm:text-sm text-[#57534e]">
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-rose-50/50 border border-rose-100/80">
                      <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">Sobreventa en horas pico:</strong>
                        Aceptas 10 pedidos para la misma hora y tu cocina o taller no da abasto, provocando retrasos y quejas.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-rose-50/50 border border-rose-100/80">
                      <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">Pedidos traspapelados:</strong>
                        Comprobantes perdidos entre cientos de chats, notas en papel y audios que nadie encuentra al despachar.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-rose-50/50 border border-rose-100/80">
                      <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">Disputas de stock y turnos:</strong>
                        Dos clientes intentan asegurar el último cupo a la vez y tienes que cancelar a uno de ellos.
                      </div>
                    </li>
                  </ul>
                </div>

                <div className="mt-6 p-4 bg-rose-50 border border-rose-200/70 rounded-2xl text-xs text-rose-700 font-bold text-center leading-relaxed">
                  ❌ Pérdida promedio del 25% de clientes por mala experiencia y pedidos demorados
                </div>
              </Card>
            </motion.div>
          )}

          {/* Right Card: The Order with JaldiShop */}
          {(viewMode === 'BOTH' || viewMode === 'JALDI') && (
            <motion.div
              layout
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              transition={{ duration: 0.25 }}
              className={`h-full ${viewMode === 'JALDI' ? 'lg:col-span-2 max-w-2xl mx-auto w-full' : ''}`}
            >
              <Card className="p-6 sm:p-8 h-full bg-[#f0fdfa] shadow-xl shadow-[#005141]/10 relative overflow-hidden flex flex-col justify-between rounded-3xl border border-emerald-300 ring-2 ring-emerald-500/10 hover:border-[#005141] transition-all duration-300">
                <div className="space-y-5">
                  {/* Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-teal-100">
                    <div className="flex items-center gap-3">
                      <div className="w-11 h-11 rounded-2xl bg-[#005141] text-white flex items-center justify-center font-bold shadow-md shrink-0">
                        <Sparkles className="w-5 h-5 text-[#feae2c]" />
                      </div>
                      <div>
                        <h3 className="font-display text-base sm:text-lg font-bold text-[#1c1917]">
                          Capa de Orden JaldiShop Pro
                        </h3>
                        <p className="text-xs text-[#005141] font-bold">Control de Capacidad Inteligente</p>
                      </div>
                    </div>
                    <div className="self-start sm:self-auto">
                      <Badge variant="jade" size="sm">
                        Flujo Blindado
                      </Badge>
                    </div>
                  </div>

                  {/* CSS Craft Illustration: The Order */}
                  <JaldiOrderIllustration />

                  {/* Bullet points */}
                  <ul className="space-y-3 text-xs sm:text-sm text-[#57534e]">
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-white border border-teal-200/80 shadow-xs">
                      <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">Límites exactos por franja horaria:</strong>
                        Tu tienda solo acepta los pedidos que tu equipo puede preparar con calidad. Al llenarse, se bloquea sola.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-white border border-teal-200/80 shadow-xs">
                      <ShieldCheck className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">Tiempo de gracia de 10 minutos:</strong>
                        El cupo queda asegurado mientras el cliente realiza el pago, blindando tu tienda contra la doble venta.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-2xl bg-white border border-teal-200/80 shadow-xs">
                      <MessageCircle className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-[#1c1917] block font-bold mb-0.5">WhatsApp Order Bridge estructurado:</strong>
                        Los pedidos llegan limpios con producto, total, comprobante y franja de entrega lista para despachar.
                      </div>
                    </li>
                  </ul>
                </div>

                <div className="mt-6 p-4 bg-[#ccfbf1]/80 border border-teal-300/80 rounded-2xl text-xs text-[#005141] font-extrabold text-center leading-relaxed">
                  ✨ 99.8% de entregas puntuales y 3x más recomendaciones de clientes satisfechos
                </div>
              </Card>
            </motion.div>
          )}
        </div>
      </Container>
    </section>
  );
}

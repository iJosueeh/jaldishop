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
    <section id="antes-despues" className="pt-20 pb-12 sm:pt-28 sm:pb-16 bg-[#090b0c] text-white relative scroll-mt-20 sm:scroll-mt-24">
      {/* 1. Subtle Dot Blueprint Grid flowing through the section */}
      <div className="absolute inset-0 bg-grid-dark pointer-events-none opacity-35 [mask-image:radial-gradient(ellipse_at_center,black_45%,transparent_85%)]" />

      {/* 2. Unified Single Seam Light between HowItWorks and ComparisonSection */}
      <div className="absolute top-0 -translate-y-1/2 left-1/3 -translate-x-1/2 w-[600px] h-[450px] bg-[#feae2c]/12 rounded-full blur-[130px] pointer-events-none" />
      <div className="absolute top-1/3 -left-20 w-[450px] h-[450px] bg-[#ea580c]/12 rounded-full blur-[130px] pointer-events-none" />

      <Container size="lg" className="relative z-10">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-10 sm:mb-12">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#141a1d] border border-orange-500/25 text-xs font-semibold text-[#ea580c] shadow-lg shadow-orange-950/20 backdrop-blur-md">
            <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
            Más orden en tu día a día
          </div>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-light tracking-tight text-white leading-tight">
            ¿Por qué vender por chat tradicional te cuesta dinero y clientes?
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-[#a1a1aa] max-w-xl mx-auto">
            Compara el estrés diario de coordinar pedidos manualmente frente a la tranquilidad de una tienda con horarios y cupos definidos.
          </p>

          {/* Interactive Mode Switcher Tabs */}
          <div className="pt-3 flex items-center justify-center w-full px-2">
            <div className="inline-flex max-w-full overflow-x-auto no-scrollbar p-1.5 bg-[#12171a] rounded-full border border-white/10 shadow-xl gap-1">
              <button
                type="button"
                onClick={() => setViewMode('BOTH')}
                className={`px-4 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'BOTH'
                    ? 'bg-white text-[#02090a] shadow-md font-semibold scale-[1.02]'
                    : 'text-stone-400 hover:text-white'
                }`}
              >
                <SplitSquareVertical className="w-3.5 h-3.5" />
                <span><span className="hidden sm:inline">Comparar </span>Ambos</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('CHAOS')}
                className={`px-4 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'CHAOS'
                    ? 'bg-rose-950/90 border border-rose-500/50 text-rose-200 shadow-md font-semibold scale-[1.02]'
                    : 'text-stone-400 hover:text-rose-400'
                }`}
              >
                <span>💥 <span className="hidden sm:inline">El </span>Caos</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('JALDI')}
                className={`px-4 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                  viewMode === 'JALDI'
                    ? 'bg-emerald-950/90 border border-emerald-500/50 text-emerald-200 shadow-md font-semibold scale-[1.02]'
                    : 'text-stone-400 hover:text-emerald-400'
                }`}
              >
                <span>✨ <span className="hidden sm:inline">El </span>Orden</span>
              </button>
            </div>
          </div>
        </div>

        {/* Comparison Cards Grid - 12px border radius with atmospheric glow */}
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
              <Card className="p-6 sm:p-8 h-full bg-[#121619]/90 backdrop-blur-md relative overflow-hidden flex flex-col justify-between rounded-[12px] border border-rose-500/25 hover:border-rose-500/40 shadow-[0_10px_35px_rgba(244,63,94,0.06)] transition-all duration-300">
                <div className="space-y-5">
                  {/* Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-rose-950/40">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-[10px] bg-rose-950/40 text-rose-400 flex items-center justify-center font-bold shrink-0 border border-rose-800/30">
                        <AlertTriangle className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="font-display text-base sm:text-lg font-semibold text-white">
                          Venta por Chat Desordenado
                        </h3>
                        <p className="text-xs text-rose-400 font-medium">El cuello de botella de las MYPE</p>
                      </div>
                    </div>
                    <div className="self-start sm:self-auto">
                      <Badge variant="danger" size="sm">
                        Fricción Operativa
                      </Badge>
                    </div>
                  </div>

                  {/* CSS Craft Illustration: The Chaos */}
                  <div className="opacity-95">
                    <ChatChaosIllustration />
                  </div>

                  {/* Bullet points */}
                  <ul className="space-y-3 text-xs sm:text-sm text-stone-300">
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-rose-900/30">
                      <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">Sobreventa en horas pico:</strong>
                        Aceptas 10 pedidos para la misma hora y tu cocina o taller no da abasto, provocando retrasos y quejas.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-rose-900/30">
                      <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">Pedidos traspapelados:</strong>
                        Comprobantes perdidos entre cientos de chats, notas en papel y audios que nadie encuentra al despachar.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-rose-900/30">
                      <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">Disputas de stock y turnos:</strong>
                        Dos clientes intentan asegurar el último cupo a la vez y tienes que cancelar a uno de ellos.
                      </div>
                    </li>
                  </ul>
                </div>

                <div className="mt-6 p-4 bg-rose-950/30 border border-rose-900/40 rounded-[10px] text-xs text-rose-300 font-medium text-center leading-relaxed">
                  Más mensajes para coordinar, menos claridad para preparar y entregar.
                </div>
              </Card>
            </motion.div>
          )}

          {/* Right Card: The Order with JaldiShop (Illuminated Raised Panel) */}
          {(viewMode === 'BOTH' || viewMode === 'JALDI') && (
            <motion.div
              layout
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              transition={{ duration: 0.25 }}
              className={`h-full ${viewMode === 'JALDI' ? 'lg:col-span-2 max-w-2xl mx-auto w-full' : ''}`}
            >
              <Card className="p-6 sm:p-8 h-full bg-[#0d1614]/95 backdrop-blur-md relative overflow-hidden flex flex-col justify-between rounded-[12px] border border-emerald-500/40 hover:border-emerald-500/60 shadow-[0_10px_40px_rgba(16,185,129,0.12)] transition-all duration-300">
                {/* Subtle internal rim gradient */}
                <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/10 blur-2xl rounded-full pointer-events-none" />

                <div className="space-y-5 relative z-10">
                  {/* Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-emerald-900/30">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-[10px] bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 flex items-center justify-center font-bold shrink-0">
                        <Sparkles className="w-5 h-5 text-[#feae2c]" />
                      </div>
                      <div>
                        <h3 className="font-display text-base sm:text-lg font-semibold text-white">
                          Capa de Orden JaldiShop Pro
                        </h3>
                        <p className="text-xs text-emerald-400 font-medium">Tú decides cuántos pedidos aceptar</p>
                      </div>
                    </div>
                    <div className="self-start sm:self-auto">
                      <Badge variant="jade" size="sm">
                        Cupos bajo control
                      </Badge>
                    </div>
                  </div>

                  {/* CSS Craft Illustration: The Order */}
                  <JaldiOrderIllustration />

                  {/* Bullet points with multi-color functional accents */}
                  <ul className="space-y-3 text-xs sm:text-sm text-stone-300">
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-emerald-500/25">
                      <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">Un límite para cada horario:</strong>
                        Tu tienda solo acepta los pedidos que tu equipo puede preparar con calidad. Al llenarse, se bloquea sola.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-[#feae2c]/25">
                      <ShieldCheck className="w-5 h-5 text-[#feae2c] shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">10 minutos para iniciar el pago:</strong>
                        Al empezar la compra, reservamos el cupo durante 10 minutos para que el cliente pueda iniciar el pago.
                      </div>
                    </li>
                    <li className="flex items-start gap-3 p-3 rounded-[10px] bg-black/40 border border-emerald-500/25">
                      <MessageCircle className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                      <div className="leading-relaxed">
                        <strong className="text-white block font-semibold mb-0.5">Un resumen para compartir por WhatsApp:</strong>
                        Tu cliente puede enviarte los productos, el total y el horario elegido para coordinar el pedido.
                      </div>
                    </li>
                  </ul>
                </div>

                <div className="mt-6 p-4 bg-emerald-950/40 border border-emerald-500/35 rounded-[10px] text-xs text-emerald-300 font-medium text-center leading-relaxed relative z-10">
                  ✨ Productos, cantidades y horarios claros para organizar tus pedidos.
                </div>
              </Card>
            </motion.div>
          )}
        </div>
      </Container>
    </section>
  );
}

import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Clock, MessageCircle, BarChart3, Smartphone, Check, Zap } from 'lucide-react';

export function BentoFeatures() {
  return (
    <section id="caracteristicas" className="py-16 sm:py-24 bg-[#0a0a09] text-white relative overflow-hidden">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[700px] bg-[#005141]/15 blur-[160px] pointer-events-none" />

      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-10 sm:mb-16">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/10 text-emerald-400 text-xs font-bold backdrop-blur-sm shadow-sm">
            Pilares Tecnológicos
          </div>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
            Todo lo que tu negocio necesita para operar sin saturación
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-stone-300 max-w-xl mx-auto">
            Diseñado especialmente para MYPEs que preparan bajo pedido: reposterías, comida artesanal, regalos y talleres.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 sm:gap-6">
          {/* Bento Item 1: Large Featured Card - Capacidad Dinámica */}
          <MotionFade delay={0.1} className="md:col-span-2">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-[#141413] border border-white/10 shadow-2xl shadow-black/40 rounded-3xl flex flex-col justify-between group transition-all">
              <div className="space-y-3 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 text-emerald-400 flex items-center justify-center">
                  <Clock className="w-6 h-6" />
                </div>
                <h3 className="font-display text-xl sm:text-2xl font-extrabold text-white">
                  Motor de Capacidad Operativa por Franjas Horarias
                </h3>
                <p className="text-xs sm:text-sm lg:text-base text-stone-300 leading-relaxed max-w-xl font-normal">
                  Configura cuántos pedidos o productos puede procesar tu taller, cocina o equipo por bloque de tiempo. Cuando se llena el cupo, ese horario queda deshabilitado automáticamente, evitando saturaciones y retrasos.
                </p>
              </div>

              {/* Visual preview with responsive stacking */}
              <div className="mt-6 sm:mt-8 p-3.5 sm:p-4 rounded-2xl bg-white/5 border border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-2.5 sm:gap-3.5">
                <div className="p-3 bg-white/10 rounded-2xl flex sm:flex-col items-center justify-between sm:justify-center text-left sm:text-center shadow-xs border border-emerald-500/40">
                  <div className="text-xs font-bold text-stone-300">10:00 AM</div>
                  <div className="text-sm font-extrabold text-emerald-400 sm:mt-0.5">4 cupos libres</div>
                </div>
                <div className="p-3 bg-white/10 rounded-2xl flex sm:flex-col items-center justify-between sm:justify-center text-left sm:text-center shadow-xs border border-amber-500/40">
                  <div className="text-xs font-bold text-stone-300">11:00 AM</div>
                  <div className="text-sm font-extrabold text-[#feae2c] sm:mt-0.5">1 cupo (Último)</div>
                </div>
                <div className="p-3 bg-white/[0.03] rounded-2xl flex sm:flex-col items-center justify-between sm:justify-center text-left sm:text-center opacity-60 border border-white/5">
                  <div className="text-xs font-bold text-stone-500">12:00 PM</div>
                  <div className="text-sm font-extrabold text-rose-400 sm:mt-0.5">Agotado</div>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 2: Hold Transaccional 10m */}
          <MotionFade delay={0.2} className="md:col-span-1">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-gradient-to-br from-[#005141] to-[#00382d] text-white shadow-2xl shadow-[#005141]/30 flex flex-col justify-between rounded-3xl border border-emerald-500/30">
              <div className="space-y-3 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/15 text-white flex items-center justify-center backdrop-blur-sm">
                  <Zap className="w-6 h-6 text-[#feae2c]" />
                </div>
                <h3 className="font-display text-xl sm:text-2xl font-extrabold text-white">
                  Tiempo de Gracia de 10 Minutos
                </h3>
                <p className="text-xs sm:text-sm lg:text-base text-emerald-100 leading-relaxed font-normal">
                  Guarda el cupo del pedido automáticamente mientras el comprador realiza la transferencia o pago. Cero doble reserva.
                </p>
              </div>

              <div className="mt-6 sm:mt-8 pt-4 border-t border-white/15 flex items-center justify-between text-xs text-emerald-200">
                <span className="font-semibold">Reserva garantizada</span>
                <span className="font-display font-extrabold bg-white/20 px-3 py-1 rounded-xl text-[#feae2c]">
                  10:00 min
                </span>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 3: WhatsApp Bridge */}
          <MotionFade delay={0.3} className="md:col-span-1">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-[#141413] border border-white/10 shadow-2xl shadow-black/40 flex flex-col justify-between rounded-3xl transition-all">
              <div className="space-y-3 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 text-emerald-400 flex items-center justify-center">
                  <MessageCircle className="w-6 h-6" />
                </div>
                <h3 className="font-display text-lg sm:text-xl font-extrabold text-white">
                  WhatsApp Order Bridge
                </h3>
                <p className="text-xs sm:text-sm text-stone-300 leading-relaxed font-normal">
                  Genera el mensaje del pedido formateado y validado directamente al WhatsApp del comercio para resolver dudas o compartir comprobantes al instante.
                </p>
              </div>

              <div className="mt-5 sm:mt-6 flex items-center gap-2 text-xs font-bold text-emerald-400 bg-white/5 border border-white/10 p-2.5 rounded-xl">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Integración directa con WhatsApp
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 4: Mobile Experience */}
          <MotionFade delay={0.4} className="md:col-span-1">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-[#141413] border border-white/10 shadow-2xl shadow-black/40 flex flex-col justify-between rounded-3xl transition-all">
              <div className="space-y-3 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 text-orange-400 flex items-center justify-center">
                  <Smartphone className="w-6 h-6" />
                </div>
                <h3 className="font-display text-lg sm:text-xl font-extrabold text-white">
                  Experiencia Móvil Instantánea
                </h3>
                <p className="text-xs sm:text-sm text-stone-300 leading-relaxed font-normal">
                  Diseñado para abrir a toda velocidad desde un enlace en bio de Instagram o estados de WhatsApp, sin lag ni tiempos de espera.
                </p>
              </div>

              <div className="mt-5 sm:mt-6 flex items-center gap-2 text-xs font-bold text-orange-400 bg-white/5 border border-white/10 p-2.5 rounded-xl">
                <Check className="w-4 h-4 text-orange-400 shrink-0" /> Carga ultra rápida en celulares
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 5: Panel MYPE */}
          <MotionFade delay={0.5} className="md:col-span-1">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-[#141413] border border-white/10 shadow-2xl shadow-black/40 flex flex-col justify-between rounded-3xl transition-all">
              <div className="space-y-3 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 text-[#feae2c] flex items-center justify-center">
                  <BarChart3 className="w-6 h-6" />
                </div>
                <h3 className="font-display text-lg sm:text-xl font-extrabold text-white">
                  Sincronización con Panel MYPE
                </h3>
                <p className="text-xs sm:text-sm text-stone-300 leading-relaxed font-normal">
                  Tu equipo recibe los pedidos en tiempo real en la app de comerciante con notificaciones y control de estados de preparación y despacho.
                </p>
              </div>

              <div className="mt-5 sm:mt-6 flex items-center gap-2 text-xs font-bold text-[#feae2c] bg-white/5 border border-white/10 p-2.5 rounded-xl">
                <Check className="w-4 h-4 text-[#feae2c] shrink-0" /> Control de preparación y despacho
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}

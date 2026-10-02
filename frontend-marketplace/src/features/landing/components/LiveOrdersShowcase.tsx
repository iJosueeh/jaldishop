'use client';

import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { HeroShowcase } from './HeroShowcase';
import { ShieldCheck, Clock, Zap, Sparkles } from 'lucide-react';

export function LiveOrdersShowcase() {
  return (
    <section id="como-funciona" className="py-16 sm:py-24 lg:py-28 bg-[#faf7f2] relative overflow-hidden">

      {/* Soft decorative background circles */}
      <div className="absolute top-1/2 left-0 -translate-y-1/2 w-96 h-96 bg-[#005141]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/2 right-0 -translate-y-1/2 w-96 h-96 bg-[#ea580c]/5 rounded-full blur-3xl pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-center">
          {/* Left Column: Context & Explanations */}
          <div className="lg:col-span-6 space-y-5 sm:space-y-6 text-center lg:text-left">
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white shadow-xs text-[#005141] text-xs font-bold">
                <Sparkles className="w-3.5 h-3.5 text-[#005141]" />
                <span>Simulación en Vivo de Pedidos</span>
              </div>
            </MotionFade>

            <MotionFade delay={0.2}>
              <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-[#1c1917] leading-tight sm:leading-[1.15]">
                Tu negocio trabajando en{' '}
                <span className="text-[#005141] underline decoration-[#feae2c] decoration-wavy decoration-2">
                  máxima armonía
                </span>.
              </h2>
            </MotionFade>

            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-[#57534e] leading-relaxed font-medium">
                Visualiza cómo JaldiShop bloquea horarios automáticamente, sostiene reservas con 10 minutos de gracia y envía comprobantes limpios a WhatsApp sin confusiones.
              </p>
            </MotionFade>

            <MotionFade delay={0.4}>
              <div className="space-y-4 pt-2 text-left">
                <div className="flex items-start gap-3.5 p-4 rounded-2xl bg-white shadow-md border-0">
                  <div className="w-9 h-9 rounded-xl bg-[#f0fdfa] text-[#005141] flex items-center justify-center shrink-0 mt-0.5">
                    <Clock className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Cupos por Franja Horaria</h3>
                    <p className="text-xs text-[#57534e] mt-0.5 leading-relaxed">
                      Al completarse los cupos, el horario se bloquea al instante en la tienda de tus clientes.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-4 rounded-2xl bg-white shadow-md border-0">
                  <div className="w-9 h-9 rounded-xl bg-[#fffbeb] text-[#ea580c] flex items-center justify-center shrink-0 mt-0.5">
                    <Zap className="w-4 h-4 text-[#ea580c]" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Tiempo de Gracia para Pagar</h3>
                    <p className="text-xs text-[#57534e] mt-0.5 leading-relaxed">
                      10 minutos de gracia garantizados mientras el cliente transfiere o sube su comprobante.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-4 rounded-2xl bg-white shadow-md border-0">
                  <div className="w-9 h-9 rounded-xl bg-[#ecfdf5] text-[#005141] flex items-center justify-center shrink-0 mt-0.5">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Cero Doble Reserva</h3>
                    <p className="text-xs text-[#57534e] mt-0.5 leading-relaxed">
                      Protección estricta en base de datos para no saturar tu taller, cocina o equipo de trabajo.
                    </p>
                  </div>
                </div>
              </div>
            </MotionFade>

          </div>

          {/* Right Column: 3D Interactive Showcase */}
          <div className="lg:col-span-6 flex justify-center">
            <MotionFade delay={0.3} direction="left">
              <HeroShowcase />
            </MotionFade>
          </div>
        </div>
      </Container>
    </section>
  );
}

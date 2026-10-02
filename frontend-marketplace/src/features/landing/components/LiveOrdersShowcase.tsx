'use client';

import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { HeroShowcase } from './HeroShowcase';
import { ShieldCheck, Clock, Zap, Sparkles } from 'lucide-react';

export function LiveOrdersShowcase() {

  return (
    <section className="py-20 lg:py-28 bg-[#faf7f2] relative overflow-hidden border-b border-[#e7e0d6]">
      {/* Soft decorative background circles */}
      <div className="absolute top-1/2 left-0 -translate-y-1/2 w-96 h-96 bg-[#005141]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/2 right-0 -translate-y-1/2 w-96 h-96 bg-[#ea580c]/5 rounded-full blur-3xl pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">
          {/* Left Column: Context & Explanations */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#f0fdfa] border border-[#ccfbf1] text-[#005141] text-xs font-bold shadow-xs">
                <Sparkles className="w-3.5 h-3.5 text-[#005141]" />
                <span>Simulación en Vivo de Pedidos</span>
              </div>
            </MotionFade>

            <MotionFade delay={0.2}>
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-[#1c1917] leading-[1.15]">
                Tu cocina trabajando en{' '}
                <span className="text-[#005141] underline decoration-[#feae2c] decoration-wavy decoration-2">
                  máxima armonía
                </span>.
              </h2>
            </MotionFade>

            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-[#57534e] leading-relaxed font-medium">
                Visualiza cómo JaldiShop bloquea cupos automáticamente, sostiene reservas con Hold de 10 minutos y envía comprobantes limpios a WhatsApp sin confusiones.
              </p>
            </MotionFade>

            <MotionFade delay={0.4}>
              <div className="space-y-4 pt-2 text-left">
                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white border border-[#e7e0d6] shadow-xs">
                  <div className="w-8 h-8 rounded-xl bg-[#f0fdfa] text-[#005141] flex items-center justify-center shrink-0 mt-0.5">
                    <Clock className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Cupos por Franja Horaria</h3>
                    <p className="text-xs text-[#57534e] mt-0.5">
                      Al completarse los cupos, el horario se bloquea al instante en la tienda de tus clientes.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white border border-[#e7e0d6] shadow-xs">
                  <div className="w-8 h-8 rounded-xl bg-[#fffbeb] text-[#ea580c] flex items-center justify-center shrink-0 mt-0.5">
                    <Zap className="w-4 h-4 text-[#ea580c]" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Hold de Reserva Atómica</h3>
                    <p className="text-xs text-[#57534e] mt-0.5">
                      10 minutos de gracia garantizados mientras el cliente transfiere o sube su comprobante.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white border border-[#e7e0d6] shadow-xs">
                  <div className="w-8 h-8 rounded-xl bg-[#ecfdf5] text-[#005141] flex items-center justify-center shrink-0 mt-0.5">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-[#1c1917]">Cero Doble Reserva</h3>
                    <p className="text-xs text-[#57534e] mt-0.5">
                      Protección estricta en base de datos para no saturar hornos ni cocineros.
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

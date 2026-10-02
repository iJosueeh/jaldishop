import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { LiveDemoWidget } from './LiveDemoWidget';
import { ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { Button } from '@/shared/components/ui/Button';

export function CapacitySimulatorSection() {
  return (
    <section id="simulador" className="py-24 bg-[#0a0a09] text-white relative overflow-hidden">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-[#005141]/20 blur-[150px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-[#feae2c]/10 blur-[150px] pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left Explanation Column */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/10 text-emerald-400 text-xs font-bold backdrop-blur-sm shadow-sm">
                Laboratorio Interactivo
              </div>
            </MotionFade>

            <MotionFade delay={0.2}>
              <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
                Experimenta en vivo cómo funciona la reserva inteligente.
              </h2>
            </MotionFade>

            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-stone-300 leading-relaxed font-normal">
                Selecciona un rubro bajo pedido en el simulador y comprueba cómo el sistema bloquea automáticamente los cupos por franja para evitar que tu negocio o taller se sobrecargue.
              </p>
            </MotionFade>

            <MotionFade delay={0.4}>
              <div className="space-y-3.5 pt-2">
                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md text-sm text-stone-300 text-left">
                  <div className="w-7 h-7 rounded-xl bg-white/10 text-emerald-400 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">
                    1
                  </div>
                  <div>
                    <strong className="text-white block">Elige modalidad:</strong>
                    Delivery a domicilio o retiro en tienda según la disponibilidad de tu local.
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md text-sm text-stone-300 text-left">
                  <div className="w-7 h-7 rounded-xl bg-white/10 text-[#feae2c] flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">
                    2
                  </div>
                  <div>
                    <strong className="text-white block">Selecciona franja horaria:</strong>
                    Observa cómo los cupos restantes se actualizan en tiempo real.
                  </div>
                </div>

                <div className="flex items-start gap-3.5 p-3.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md text-sm text-stone-300 text-left">
                  <div className="w-7 h-7 rounded-xl bg-white/10 text-orange-400 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">
                    3
                  </div>
                  <div>
                    <strong className="text-white block">Simula el Hold 10 min:</strong>
                    El cupo queda blindado mientras se procesa el pago.
                  </div>
                </div>
              </div>
            </MotionFade>

            <MotionFade delay={0.5}>
              <div className="pt-4 flex justify-center lg:justify-start">
                <Link href="/tienda/panaderia-don-pepe">
                  <Button
                    variant="primary"
                    size="md"
                    className="bg-[#005141] hover:bg-[#00382d] text-white px-6 py-3 rounded-2xl border-0 shadow-lg shadow-black/30 font-bold"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    Probar en Tienda Real
                  </Button>
                </Link>
              </div>
            </MotionFade>
          </div>

          {/* Right Interactive Simulator Widget */}
          <div className="lg:col-span-6 flex justify-center">
            <MotionFade delay={0.2} direction="left" className="w-full max-w-md">
              <LiveDemoWidget />
            </MotionFade>
          </div>
        </div>
      </Container>
    </section>
  );
}


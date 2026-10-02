import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Badge } from '@/shared/components/ui/Badge';
import { LiveDemoWidget } from './LiveDemoWidget';
import { ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { Button } from '@/shared/components/ui/Button';

export function CapacitySimulatorSection() {
  return (
    <section id="simulador" className="py-24 bg-linear-to-b from-warm-background-primary via-warm-background-secondary to-warm-background-primary relative overflow-hidden">
      {/* Decorative Subtle Background Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-162.5 h-112.5 bg-warm-secondary-500/10 blur-[130px] rounded-full pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left Explanation Column */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            <MotionFade delay={0.1}>
              <Badge variant="jade" size="md">
                Laboratorio Interactivo
              </Badge>
            </MotionFade>

            <MotionFade delay={0.2}>
              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-warm-text-primary leading-tight">
                Experimenta en vivo cómo funciona la reserva inteligente.
              </h2>
            </MotionFade>

            <MotionFade delay={0.3}>
              <p className="text-base text-warm-text-muted leading-relaxed">
                Selecciona un rubro gastronómico en el simulador y comprueba cómo el sistema bloquea automáticamente los cupos por franja para evitar que tu cocina se sobrecargue.
              </p>
            </MotionFade>

            <MotionFade delay={0.4}>
              <div className="space-y-3.5 pt-2">
                <div className="flex items-start gap-3 text-sm text-warm-text-muted text-left">
                  <div className="w-6 h-6 rounded-full bg-warm-background-primary text-warm-primary-500 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5 border border-warm-border">
                    1
                  </div>
                  <div>
                    <strong className="text-warm-text-primary block">Elige modalidad:</strong>
                    Delivery a domicilio o retiro en tienda según la disponibilidad de tu local.
                  </div>
                </div>

                <div className="flex items-start gap-3 text-sm text-warm-text-muted text-left">
                  <div className="w-6 h-6 rounded-full bg-warm-background-primary text-warm-secondary-500 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5 border border-warm-border">
                    2
                  </div>
                  <div>
                    <strong className="text-warm-text-primary block">Selecciona franja horaria:</strong>
                    Observa cómo los cupos restantes se actualizan en tiempo real.
                  </div>
                </div>

                <div className="flex items-start gap-3 text-sm text-warm-text-muted text-left">
                  <div className="w-6 h-6 rounded-full bg-amber-subtle text-warm-secondary-500 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5 border border-warm-border">
                    3
                  </div>
                  <div>
                    <strong className="text-warm-text-primary block">Simula el Hold 10 min:</strong>
                    El cupo queda blindado mientras se procesa el pago.
                  </div>
                </div>
              </div>
            </MotionFade>

            <MotionFade delay={0.5}>
              <div className="pt-4 flex justify-center lg:justify-start">
                <Link href="/tienda/panaderia-don-pepe">
                  <Button variant="primary" size="md" rightIcon={<ArrowRight className="w-4 h-4" />}>
                    Probar en Tienda Real
                  </Button>
                </Link>
              </div>
            </MotionFade>
          </div>

          {/* Right Interactive Simulator Widget */}
          <div className="lg:col-span-6 flex justify-center">
            <MotionFade delay={0.2} direction="left">
              <LiveDemoWidget />
            </MotionFade>
          </div>
        </div>
      </Container>
    </section>
  );
}

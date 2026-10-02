import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, ShieldCheck, Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { HeroShowcase } from './HeroShowcase';
import { HeroVideoBackground } from './HeroVideoBackground';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function HeroSection() {
  return (
    <section className="relative pt-28 pb-20 lg:pt-36 lg:pb-32 overflow-hidden">
      {/* Optimized Responsive Video + Static Poster CDN Background */}
      <HeroVideoBackground />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Clean, high-impact Value Proposition */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            {/* Pill Tag with Live Animation */}
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-white/95 backdrop-blur-md border-2 border-[#e7e0d6] text-[#1c1917] text-xs font-extrabold shadow-md shadow-[#1c1917]/5">
                <span className="w-2.5 h-2.5 rounded-full bg-[#ea580c] animate-pulse" />
                <span>Control de Capacidad Inteligente para MYPE</span>
              </div>
            </MotionFade>

            {/* Headline */}
            <MotionFade delay={0.2}>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#1c1917] leading-[1.12]">
                Vende por WhatsApp e Instagram{' '}
                <span className="text-[#005141] underline decoration-[#feae2c] decoration-wavy decoration-2">
                  sin sobreventa
                </span>{' '}
                ni caos en tu cocina.
              </h1>
            </MotionFade>

            {/* Subtitle */}
            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-[#57534e] max-w-xl mx-auto lg:mx-0 leading-relaxed font-medium">
                JaldiShop sincroniza tus pedidos con la **capacidad real de tu cocina, taller o pastelería**. Bloquea franjas horarias al agotarse los cupos y asegura pedidos organizados con 10 minutos de reserva.
              </p>
            </MotionFade>

            {/* Action Buttons: Clean & Direct */}
            <MotionFade delay={0.4}>
              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-3.5 pt-2">
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:w-auto"
                >
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-full sm:w-auto text-base font-extrabold shadow-xl shadow-[#005141]/25 cursor-pointer"
                    leftIcon={<Store className="w-5 h-5" />}
                  >
                    Crear mi Tienda Gratis
                  </Button>
                </a>

                <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                  <Button
                    variant="outline"
                    size="lg"
                    className="w-full sm:w-auto font-extrabold cursor-pointer"
                    rightIcon={<ArrowRight className="w-5 h-5 text-[#57534e]" />}
                  >
                    Explorar Tiendas
                  </Button>
                </Link>
              </div>
            </MotionFade>

            {/* Value Guarantees Pill Badges */}
            <MotionFade delay={0.5}>
              <div className="pt-6 border-t border-[#e7e0d6]/80 grid grid-cols-3 gap-2 text-left">
                <div className="flex items-center gap-2 text-xs font-bold text-[#1c1917]">
                  <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0" />
                  <span>Cero sobreventa</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-bold text-[#1c1917]">
                  <ShieldCheck className="w-4 h-4 text-[#ea580c] shrink-0" />
                  <span>Hold seguro 10m</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-bold text-[#1c1917]">
                  <Sparkles className="w-4 h-4 text-[#feae2c] shrink-0" />
                  <span>Listo en 5 min</span>
                </div>
              </div>
            </MotionFade>
          </div>

          {/* Right Column: Dynamic 3D Hero Showcase Mockup with Mouse Tilt */}
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

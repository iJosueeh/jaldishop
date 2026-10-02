import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, ShieldCheck, Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { HeroShowcase } from './HeroShowcase';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function HeroSection() {
  return (
    <section className="relative pt-28 pb-20 lg:pt-36 lg:pb-28 overflow-hidden bg-gradient-to-b from-[#faf7f2] via-[#faf7f2] to-[#f7f3ec] bg-grid-warm">
      {/* Warm Ambient Glows (Shopify / Warm Craft Aura) */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[500px] bg-[#feae2c]/12 blur-[140px] rounded-full pointer-events-none -z-10" />
      <div className="absolute top-10 right-10 w-[450px] h-[450px] bg-[#005141]/8 blur-[120px] rounded-full pointer-events-none -z-10" />
      <div className="absolute bottom-0 left-10 w-[400px] h-[400px] bg-[#ea580c]/8 blur-[130px] rounded-full pointer-events-none -z-10" />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: SaaS Value Proposition */}
          <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
            {/* Pill Tag */}
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white border-2 border-[#e7e0d6] text-[#1c1917] text-xs font-bold shadow-xs">
                <span className="w-2 h-2 rounded-full bg-[#ea580c]" />
                <span>La plataforma de pedidos con control de capacidad para MYPE</span>
              </div>
            </MotionFade>

            {/* Headline */}
            <MotionFade delay={0.2}>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#1c1917] leading-[1.12]">
                Vende por WhatsApp e Instagram{' '}
                <span className="text-[#005141] underline decoration-[#feae2c] decoration-wavy decoration-2">
                  sin sobreventa
                </span>{' '}
                ni estrés operativo.
              </h1>
            </MotionFade>

            {/* Subtitle */}
            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-[#57534e] max-w-xl mx-auto lg:mx-0 leading-relaxed font-normal">
                JaldiShop sincroniza tus pedidos con la **capacidad real de tu cocina, taller o pastelería**. Bloquea franjas horarias al agotarse, reserva cupos por 10 minutos y ofrece una tienda online que transmite confianza y profesionalismo.
              </p>
            </MotionFade>

            {/* Action Buttons */}
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
                    className="w-full sm:w-auto text-base font-bold shadow-lg shadow-[#005141]/20 cursor-pointer"
                    leftIcon={<Store className="w-4 h-4" />}
                  >
                    Crear mi Tienda Gratis
                  </Button>
                </a>

                <Link href="/tienda/panaderia-don-pepe" className="w-full sm:w-auto">
                  <Button
                    variant="outline"
                    size="lg"
                    className="w-full sm:w-auto font-bold cursor-pointer"
                    rightIcon={<ArrowRight className="w-4 h-4 text-[#57534e]" />}
                  >
                    Ver Tienda Demo
                  </Button>
                </Link>
              </div>
            </MotionFade>

            {/* Value Guarantees Pill Badges */}
            <MotionFade delay={0.5}>
              <div className="pt-6 border-t border-[#e7e0d6] grid grid-cols-3 gap-2 text-left">
                <div className="flex items-center gap-2 text-xs font-semibold text-[#57534e]">
                  <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0" />
                  <span>Cero sobreventa</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-semibold text-[#57534e]">
                  <ShieldCheck className="w-4 h-4 text-[#ea580c] shrink-0" />
                  <span>Hold seguro 10m</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-semibold text-[#57534e]">
                  <Sparkles className="w-4 h-4 text-[#feae2c] shrink-0" />
                  <span>Listo en 5 min</span>
                </div>
              </div>
            </MotionFade>
          </div>

          {/* Right Column: Dynamic Hero Showcase Mockup (Option A) */}
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

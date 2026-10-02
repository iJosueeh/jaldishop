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
    <section className="relative pt-24 pb-20 lg:pt-32 lg:pb-28 overflow-hidden">
      {/* Video Background from Mixkit CDN */}
      <HeroVideoBackground />

      <Container size="lg">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-center">
          {/* Left Column: Clean, Short & Concise Value Proposition */}
          <div className="lg:col-span-6 space-y-5 text-center lg:text-left">
            {/* Pill Tag */}
            <MotionFade delay={0.1}>
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/95 backdrop-blur-md border border-[#e7e0d6] text-[#1c1917] text-xs font-bold shadow-sm">
                <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-pulse" />
                <span>Capacidad Inteligente para MYPE</span>
              </div>
            </MotionFade>

            {/* Concise Headline */}
            <MotionFade delay={0.2}>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#1c1917] leading-[1.12]">
                Vende por WhatsApp e Instagram{' '}
                <span className="text-[#005141] underline decoration-[#feae2c] decoration-wavy decoration-2">
                  sin sobreventa
                </span>.
              </h1>
            </MotionFade>

            {/* Short Subtitle */}
            <MotionFade delay={0.3}>
              <p className="text-base sm:text-lg text-[#57534e] max-w-lg mx-auto lg:mx-0 leading-relaxed font-medium">
                Sincroniza tus pedidos con la capacidad real de tu cocina o taller. Franjas horarias automáticas y reserva de 10 minutos para atender siempre a tiempo.
              </p>
            </MotionFade>

            {/* Clean Action Buttons */}
            <MotionFade delay={0.4}>
              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-3 pt-2">
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:w-auto"
                >
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-full sm:w-auto text-base font-extrabold shadow-lg shadow-[#005141]/20 cursor-pointer"
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

            {/* 3 Value Guarantees */}
            <MotionFade delay={0.5}>
              <div className="pt-5 border-t border-[#e7e0d6]/80 grid grid-cols-3 gap-2 text-left">
                <div className="flex items-center gap-1.5 text-xs font-bold text-[#1c1917]">
                  <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0" />
                  <span>Cero sobreventa</span>
                </div>
                <div className="flex items-center gap-1.5 text-xs font-bold text-[#1c1917]">
                  <ShieldCheck className="w-4 h-4 text-[#ea580c] shrink-0" />
                  <span>Hold 10m seguro</span>
                </div>
                <div className="flex items-center gap-1.5 text-xs font-bold text-[#1c1917]">
                  <Sparkles className="w-4 h-4 text-[#feae2c] shrink-0" />
                  <span>Sin comisiones</span>
                </div>
              </div>
            </MotionFade>
          </div>

          {/* Right Column: 3D Hero Showcase Mockup */}
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

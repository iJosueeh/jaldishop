import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, ShieldCheck, Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { HeroVideoBackground } from './HeroVideoBackground';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function HeroSection() {

  return (
    <section className="relative min-h-[88vh] lg:min-h-[92vh] flex items-end pb-12 sm:pb-18 lg:pb-22 pt-32 sm:pt-36 overflow-hidden">
      {/* 1. Full-bleed local video loop */}
      <HeroVideoBackground />

      {/* 2. Bottom-Left Cinematic HUD Container */}
      <Container size="lg" className="relative z-10 w-full">
        <div className="max-w-2xl text-left space-y-5 sm:space-y-6">
          {/* Pill Tag */}
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/60 backdrop-blur-md border border-white/20 text-stone-200 text-xs sm:text-sm font-semibold shadow-2xl">
              <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-pulse" />
              <span>Capacidad Inteligente para Gastronomía y Talleres MYPE</span>
            </div>
          </MotionFade>

          {/* Distinctive Display Headline */}
          <MotionFade delay={0.2}>
            <h1 className="font-display text-4xl sm:text-5xl lg:text-[3.5rem] font-black tracking-tight text-white leading-[1.08] drop-shadow-lg">
              Vende por WhatsApp e Instagram{' '}
              <span className="text-[#feae2c] underline decoration-[#ea580c] decoration-wavy decoration-2">
                sin sobreventa
              </span>.
            </h1>
          </MotionFade>

          {/* Concise Subtitle */}
          <MotionFade delay={0.3}>
            <p className="text-stone-200/90 text-sm sm:text-base lg:text-lg font-medium leading-relaxed max-w-xl drop-shadow-md">
              Sincroniza tus pedidos con la capacidad real de tu cocina. Franjas horarias automáticas y reserva atómica de 10 minutos para atender siempre a tiempo.
            </p>
          </MotionFade>

          {/* Action Buttons */}
          <MotionFade delay={0.4}>
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 pt-1">
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto"
              >
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold shadow-2xl shadow-black/60 cursor-pointer bg-[#005141] hover:bg-[#00382d] text-white border border-white/15 px-6 py-3.5 rounded-2xl transition-all hover:scale-[1.02]"
                  leftIcon={<Store className="w-5 h-5" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>

              <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold cursor-pointer bg-black/40 hover:bg-black/60 text-white border-white/30 backdrop-blur-md px-6 py-3.5 rounded-2xl transition-all hover:scale-[1.02]"
                  rightIcon={<ArrowRight className="w-5 h-5 text-stone-200" />}
                >
                  Explorar Tiendas
                </Button>
              </Link>
            </div>
          </MotionFade>

          {/* 3 Value Guarantees HUD Strip */}
          <MotionFade delay={0.5}>
            <div className="pt-2 flex flex-wrap items-center gap-2.5 sm:gap-3">
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-200 shadow-xl">
                <CheckCircle2 className="w-4 h-4 text-[#34d399] shrink-0" />
                <span>Cero sobreventa</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-200 shadow-xl">
                <ShieldCheck className="w-4 h-4 text-[#fb923c] shrink-0" />
                <span>Hold 10m seguro</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-200 shadow-xl">
                <Sparkles className="w-4 h-4 text-[#feae2c] shrink-0" />
                <span>Sin comisiones abusivas</span>
              </div>
            </div>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}




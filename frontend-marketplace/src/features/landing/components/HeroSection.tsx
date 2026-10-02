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
    <section className="relative min-h-[85vh] lg:min-h-[90vh] flex items-center justify-center pt-28 pb-20 lg:pt-36 lg:pb-32 overflow-hidden">
      {/* Video Background from local high-performance source */}
      <HeroVideoBackground />

      <Container size="lg" className="relative z-10">
        <div className="max-w-4xl mx-auto text-center space-y-7">
          {/* Pill Tag */}
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-stone-950/60 backdrop-blur-md border border-white/20 text-stone-100 text-xs sm:text-sm font-medium shadow-xl">
              <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-pulse" />
              <span>Capacidad Inteligente para Gastronomía y Talleres MYPE</span>
            </div>
          </MotionFade>

          {/* Concise High-Impact Headline */}
          <MotionFade delay={0.2}>
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black tracking-tight text-white leading-[1.08] drop-shadow-md">
              Vende por WhatsApp e Instagram{' '}
              <span className="text-[#feae2c] underline decoration-[#ea580c] decoration-wavy decoration-2">
                sin sobreventa
              </span>.
            </h1>
          </MotionFade>

          {/* Short Subtitle */}
          <MotionFade delay={0.3}>
            <p className="text-base sm:text-xl text-stone-100/90 max-w-2xl mx-auto leading-relaxed font-normal drop-shadow-sm">
              Sincroniza tus pedidos con la capacidad real de tu cocina. Franjas horarias automáticas y reserva atómica de 10 minutos para atender siempre a tiempo.
            </p>
          </MotionFade>

          {/* Clean Action Buttons */}
          <MotionFade delay={0.4}>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto"
              >
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full sm:w-auto text-base font-extrabold shadow-xl shadow-black/40 cursor-pointer bg-[#005141] hover:bg-[#00382d] text-white border border-white/15 px-7 py-3.5 rounded-2xl transition-transform hover:scale-[1.02]"
                  leftIcon={<Store className="w-5 h-5" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>

              <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto font-extrabold cursor-pointer bg-white/10 hover:bg-white/20 text-white border-white/30 backdrop-blur-md px-7 py-3.5 rounded-2xl transition-transform hover:scale-[1.02]"
                  rightIcon={<ArrowRight className="w-5 h-5 text-stone-200" />}
                >
                  Explorar Tiendas
                </Button>
              </Link>
            </div>
          </MotionFade>

          {/* 3 Value Guarantees with frosted dark glass style */}
          <MotionFade delay={0.5}>
            <div className="pt-8 flex flex-wrap items-center justify-center gap-3 sm:gap-6">
              <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-stone-950/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-100 shadow-lg">
                <CheckCircle2 className="w-4 h-4 text-[#34d399] shrink-0" />
                <span>Cero sobreventa</span>
              </div>
              <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-stone-950/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-100 shadow-lg">
                <ShieldCheck className="w-4 h-4 text-[#fb923c] shrink-0" />
                <span>Hold 10m seguro</span>
              </div>
              <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-stone-950/60 backdrop-blur-md border border-white/15 text-xs sm:text-sm font-semibold text-stone-100 shadow-lg">
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



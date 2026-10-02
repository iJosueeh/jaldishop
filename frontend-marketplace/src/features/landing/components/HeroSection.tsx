import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, ShieldCheck, Store } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { HeroVideoBackground } from './HeroVideoBackground';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function HeroSection() {
  return (
    <section className="relative min-h-[88vh] lg:min-h-[92vh] flex items-end pb-14 sm:pb-20 lg:pb-24 pt-32 sm:pt-36 overflow-hidden">
      {/* 1. Full-bleed local video loop */}
      <HeroVideoBackground />

      {/* 2. Left-aligned Cinematic HUD Container (Punto intermedio) */}
      <div className="relative z-10 w-full px-6 sm:px-10 md:px-14 lg:px-16 xl:px-20">
        <div className="max-w-xl lg:max-w-2xl text-left space-y-4 sm:space-y-5">


          {/* Pill Tag */}
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/50 backdrop-blur-md text-stone-200 text-xs sm:text-sm font-semibold shadow-lg">
              <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-pulse" />
              <span>Vende por WhatsApp sin saturar tu capacidad</span>
            </div>
          </MotionFade>

          {/* Headline */}
          <MotionFade delay={0.2}>
            <h1 className="font-display text-4xl sm:text-5xl lg:text-[3.6rem] font-black tracking-tight text-white leading-[1.06] drop-shadow-lg">
              Vende por WhatsApp e Instagram{' '}
              <span className="text-[#feae2c] underline decoration-[#ea580c] decoration-wavy decoration-2">
                sin sobreventa
              </span>.
            </h1>
          </MotionFade>

          {/* Subtitle in plain, clear language */}
          <MotionFade delay={0.3}>
            <p className="text-stone-200/90 text-sm sm:text-base lg:text-lg font-normal leading-relaxed max-w-lg lg:max-w-xl drop-shadow-md">
              Recibe pedidos según la capacidad real de tu negocio o taller. Los horarios se cierran automáticamente cuando te llenas de pedidos y tus clientes tienen 10 minutos para pagar sin perder su cupo.
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
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold shadow-xl shadow-black/40 cursor-pointer bg-[#005141] hover:bg-[#00382d] text-white px-7 py-3.5 rounded-2xl transition-all hover:scale-[1.02] border-0"
                  leftIcon={<Store className="w-5 h-5" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>

              <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold cursor-pointer bg-white/15 hover:bg-white/25 text-white backdrop-blur-md px-7 py-3.5 rounded-2xl transition-all hover:scale-[1.02] border-0"
                  rightIcon={<ArrowRight className="w-5 h-5 text-stone-200" />}
                >
                  Explorar Tiendas
                </Button>
              </Link>
            </div>
          </MotionFade>

          {/* 3 Clear Merchant Value Guarantees */}
          <MotionFade delay={0.5}>
            <div className="pt-2 flex flex-wrap items-center gap-2 sm:gap-3">
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/45 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <CheckCircle2 className="w-4 h-4 text-[#34d399] shrink-0" />
                <span>Cero pedidos de más</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/45 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <ShieldCheck className="w-4 h-4 text-[#fb923c] shrink-0" />
                <span>10 min para pagar</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/45 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <Sparkles className="w-4 h-4 text-[#feae2c] shrink-0" />
                <span>Sin comisiones fijas</span>
              </div>
            </div>
          </MotionFade>
        </div>
      </div>
    </section>
  );
}






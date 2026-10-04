import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, Store, Flame, Clock } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { HeroVideoBackground } from './HeroVideoBackground';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function HeroSection() {
  return (
    <section className="relative min-h-[90svh] lg:min-h-[94vh] flex items-end pb-14 sm:pb-20 lg:pb-24 pt-32 sm:pt-36 overflow-hidden">
      {/* 1. Full-bleed background video loop with dark glass overlay */}
      <HeroVideoBackground />

      {/* 2. Left-aligned Cinematic HUD Container */}
      <div className="relative z-10 w-full px-6 sm:px-10 md:px-14 lg:px-16 xl:px-20">
        <div className="max-w-xl lg:max-w-2xl text-left space-y-5 sm:space-y-6">
          {/* Pill Tag with Live Status indicator */}
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-black/60 border border-white/15 backdrop-blur-xl text-stone-200 text-xs sm:text-sm font-semibold shadow-2xl">
              <span className="relative flex h-2.5 w-2.5 shrink-0">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#ea580c] opacity-75" />
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-[#ea580c]" />
              </span>
              <span className="text-stone-100 font-medium tracking-tight">
                Tu catálogo para WhatsApp e Instagram
              </span>
              <span className="hidden sm:inline-block text-white/30">•</span>
              <span className="hidden sm:inline-flex items-center gap-1 text-[#feae2c] font-bold text-xs">
                <Sparkles className="w-3 h-3 text-[#feae2c]" /> Pedidos organizados
              </span>
            </div>
          </MotionFade>

          {/* Headline */}
          <MotionFade delay={0.2}>
            <h1 className="font-display text-4xl sm:text-5xl lg:text-[3.75rem] font-black tracking-tight text-white leading-[1.12] sm:leading-[1.08] text-balance drop-shadow-xl">
              Vende por WhatsApp{' '}
              <span className="text-[#feae2c] underline underline-offset-[6px] decoration-[#ea580c] decoration-wavy decoration-2">
                sin recibir
              </span>{' '}
              pedidos de más.
            </h1>
          </MotionFade>

          {/* Subtitle in clear merchant language */}
          <MotionFade delay={0.3}>
            <p className="text-stone-100/95 text-base lg:text-lg font-normal leading-relaxed max-w-lg lg:max-w-xl drop-shadow-md">
              Comparte tu catálogo digital por WhatsApp e Instagram. Tus clientes eligen sus productos y un horario de entrega o recojo según los cupos de tu negocio.
            </p>
          </MotionFade>

          {/* Action Buttons */}
          <MotionFade delay={0.4}>
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 pt-1.5">
              <a
                href={new URL('/register', env.merchantUrl).href}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto"
              >
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold shadow-2xl shadow-black/60 cursor-pointer bg-[#005141] hover:bg-[#00382d] text-white px-7 py-3.5 rounded-2xl transition-all hover:scale-[1.02] will-change-transform border border-emerald-400/30 ring-4 ring-[#005141]/20"
                  leftIcon={<Store className="w-5 h-5 text-emerald-300" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>

              <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-extrabold cursor-pointer bg-white/15 hover:bg-white/25 text-white backdrop-blur-xl px-7 py-3.5 rounded-2xl transition-all hover:scale-[1.02] will-change-transform border border-white/20 shadow-lg"
                  rightIcon={<ArrowRight className="w-5 h-5 text-stone-200" />}
                >
                  Explorar Tiendas
                </Button>
              </Link>
            </div>
          </MotionFade>

          {/* 3 Clear Merchant Value Guarantees with Glass Badges */}
          <MotionFade delay={0.5}>
            <div className="pt-2 flex flex-wrap items-center gap-2 sm:gap-2.5">
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/55 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <CheckCircle2 className="w-4 h-4 text-[#34d399] shrink-0" />
                <span>Cero pedidos de más</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/55 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <Clock className="w-4 h-4 text-[#feae2c] shrink-0" />
                <span>Cupo reservado 10 min</span>
              </div>
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-black/55 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-semibold text-stone-100 shadow-md">
                <Flame className="w-4 h-4 text-[#ea580c] shrink-0" />
                <span>Sin comisiones fijas</span>
              </div>
            </div>
          </MotionFade>
        </div>
      </div>
    </section>
  );
}

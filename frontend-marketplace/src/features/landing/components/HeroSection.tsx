import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2, Store, Clock, Flame } from 'lucide-react';
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
          {/* Pill Tag with Live Status indicator — Warm & Tech Balance */}
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-[#0b0f10]/85 border border-white/15 backdrop-blur-xl text-stone-200 text-xs sm:text-sm font-medium shadow-2xl">
              <span className="relative flex h-2 w-2 shrink-0">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#ea580c] opacity-75" />
                <span className="relative inline-flex rounded-full h-2 w-2 bg-[#ea580c]" />
              </span>
              <span className="text-stone-100 font-medium tracking-tight">
                Tu catálogo para WhatsApp e Instagram
              </span>
              <span className="hidden sm:inline-block text-white/20">•</span>
              <span className="hidden sm:inline-flex items-center gap-1 text-[#feae2c] font-semibold text-xs">
                <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" /> Pedidos organizados
              </span>
            </div>
          </MotionFade>

          {/* Headline — Display Typography (DESIGN.md weight 330-400) + Warm Amber Accent */}
          <MotionFade delay={0.2}>
            <h1 className="font-display text-4xl sm:text-5xl lg:text-[3.75rem] font-light tracking-tight text-white leading-[1.08] text-balance drop-shadow-lg">
              Vende por WhatsApp{' '}
              <span className="font-normal text-transparent bg-clip-text bg-gradient-to-r from-[#feae2c] via-[#fbbf24] to-[#ea580c]">
                sin recibir
              </span>{' '}
              pedidos de más.
            </h1>
          </MotionFade>

          {/* Subtitle in clear merchant language */}
          <MotionFade delay={0.3}>
            <p className="text-[#a1a1aa] text-base lg:text-lg font-normal leading-relaxed max-w-lg lg:max-w-xl">
              Comparte tu catálogo digital por WhatsApp e Instagram. Tus clientes eligen sus productos y un horario de entrega o recojo según los cupos de tu negocio.
            </p>
          </MotionFade>

          {/* Action Buttons — Pill buttons (9999px radius) */}
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
                  className="w-full sm:w-auto text-sm sm:text-base font-semibold shadow-xl cursor-pointer bg-white hover:bg-stone-100 text-[#02090a] px-8 py-3.5 rounded-full transition-all hover:scale-[1.02] will-change-transform border border-white"
                  leftIcon={<Store className="w-4 h-4 text-[#02090a]" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>

              <Link href="#tiendas-destacadas" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto text-sm sm:text-base font-medium cursor-pointer bg-white/10 hover:bg-white/15 text-white backdrop-blur-xl px-7 py-3.5 rounded-full transition-all hover:scale-[1.02] will-change-transform border border-white/20 shadow-lg"
                  rightIcon={<ArrowRight className="w-4 h-4 text-stone-300" />}
                >
                  Explorar Tiendas
                </Button>
              </Link>
            </div>
          </MotionFade>

          {/* 3 Clear Merchant Value Guarantees with Distinct Multi-Color Accents */}
          <MotionFade delay={0.5}>
            <div className="pt-2 flex flex-wrap items-center gap-2 sm:gap-2.5">
              {/* Green / Mint: Operational safety */}
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#0b0f10]/80 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-normal text-stone-200 shadow-sm">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#34d399] shrink-0" />
                <span>Cero pedidos de más</span>
              </div>
              {/* Warm Amber: 10 min hold */}
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#0b0f10]/80 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-normal text-stone-200 shadow-sm">
                <Clock className="w-3.5 h-3.5 text-[#feae2c] shrink-0" />
                <span>Cupo reservado 10 min</span>
              </div>
              {/* Warm Terracotta: Zero fixed fees */}
              <div className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#0b0f10]/80 border border-white/10 backdrop-blur-md text-xs sm:text-sm font-normal text-stone-200 shadow-sm">
                <Flame className="w-3.5 h-3.5 text-[#ea580c] shrink-0" />
                <span>Sin comisiones fijas</span>
              </div>
            </div>
          </MotionFade>
        </div>
      </div>
    </section>
  );
}

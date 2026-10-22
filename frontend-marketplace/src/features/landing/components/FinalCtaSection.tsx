import React from 'react';
import Link from 'next/link';
import { ArrowRight, Store, CheckCircle2, ShieldCheck, Zap, Rocket } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function FinalCtaSection() {
  return (
    <section id="beneficios" className="py-24 sm:py-32 relative overflow-hidden bg-gradient-to-b from-[#101416] via-[#090b0c] to-[#040506] text-white scroll-mt-20 sm:scroll-mt-24">
      {/* Background Soft Glows: Warm Amber + Subtle Emerald */}
      <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#feae2c]/10 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-[#34d399]/8 blur-3xl rounded-full pointer-events-none" />

      <Container size="md">
        <MotionFade className="text-center space-y-6 relative z-10">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#161b1e] border border-white/10 text-[#feae2c] text-xs sm:text-sm font-semibold backdrop-blur-md shadow-sm">
            <Rocket className="w-3.5 h-3.5 text-[#feae2c]" />
            Tu catálogo digital, listo para compartir
          </div>

          <h2 className="font-display text-3xl sm:text-5xl lg:text-6xl font-light tracking-tight leading-tight text-balance max-w-3xl mx-auto">
            Tu próximo pedido,{' '}
            <span className="font-normal text-transparent bg-clip-text bg-gradient-to-r from-[#feae2c] via-[#fbbf24] to-[#ea580c]">
              con más orden.
            </span>
          </h2>

          <p className="text-stone-300 text-base sm:text-lg max-w-xl mx-auto leading-relaxed font-normal">
            Crea tu tienda, añade tus productos y define los horarios que puedes atender. Después, comparte tu catálogo por WhatsApp e Instagram.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
            <a
              href={new URL('/register', env.merchantUrl).href}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex w-full sm:w-auto items-center justify-center gap-2.5 text-base font-bold px-9 py-4 shadow-xl cursor-pointer rounded-full border border-orange-400/30 hover:scale-[1.02] transition-all bg-gradient-to-r from-[#ea580c] to-[#f97316] text-white hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              <Store aria-hidden="true" className="w-5 h-5 shrink-0 text-white" />
              Crear mi tienda gratis
            </a>

            <Link href="#tiendas-destacadas" className="inline-flex w-full sm:w-auto items-center justify-center gap-2.5 px-7 py-4 text-sm text-white bg-white/10 hover:bg-white/15 border border-white/20 backdrop-blur-md font-medium cursor-pointer rounded-full hover:scale-[1.02] transition-all focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white">
              Explorar tiendas
              <ArrowRight aria-hidden="true" className="w-4 h-4 shrink-0 text-stone-300" />
            </Link>
          </div>

          <div className="pt-4 flex flex-wrap items-center justify-center gap-x-6 gap-y-3 text-sm text-stone-300">
            <span className="flex items-center gap-1.5 font-medium">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Sin comisiones fijas
            </span>
            <span className="flex items-center gap-1.5 font-medium">
              <ShieldCheck className="w-4 h-4 text-[#feae2c]" /> Tú defines los cupos
            </span>
            <span className="flex items-center gap-1.5 font-medium">
              <Zap className="w-4 h-4 text-[#ea580c]" /> Cupo reservado 10 min
            </span>
          </div>
        </MotionFade>
      </Container>
    </section>
  );
}

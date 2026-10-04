import React from 'react';
import Link from 'next/link';
import { ArrowRight, Store, CheckCircle2, ShieldCheck, Zap, Rocket } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { env } from '@/core/config/env';

export function FinalCtaSection() {
  return (
    <section id="beneficios" className="py-20 sm:py-28 relative overflow-hidden bg-gradient-to-br from-[#005141] via-[#00382d] to-[#002820] text-white scroll-mt-20 sm:scroll-mt-24">
      {/* Background Accent Warm Glows */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-[#feae2c]/15 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-80 h-80 bg-[#ea580c]/15 blur-3xl rounded-full pointer-events-none" />

      <Container size="md">
        <MotionFade className="text-center space-y-6 relative z-10">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/15 border border-white/20 text-[#ccfbf1] text-xs sm:text-sm font-semibold backdrop-blur-md shadow-lg">
            <Rocket className="w-3.5 h-3.5 text-[#feae2c]" />
            Tu catálogo digital, listo para compartir
          </div>

          <h2 className="font-display text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-tight text-balance max-w-3xl mx-auto drop-shadow-md">
            Tu próximo pedido, con más orden.
          </h2>

          <p className="text-[#ccfbf1] text-base sm:text-lg max-w-xl mx-auto leading-relaxed font-normal">
            Crea tu tienda, añade tus productos y define los horarios que puedes atender. Después, comparte tu catálogo por WhatsApp e Instagram.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
            <a
              href={new URL('/register', env.merchantUrl).href}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex w-full sm:w-auto items-center justify-center gap-2.5 text-base font-extrabold px-9 py-4 shadow-2xl shadow-black/50 cursor-pointer rounded-2xl border border-orange-300/30 hover:scale-[1.02] transition-all bg-[#ea580c] hover:bg-[#c2410c] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              <Store aria-hidden="true" className="w-5 h-5 shrink-0 text-white" />
              Crear mi tienda gratis
            </a>

            <Link href="#tiendas-destacadas" className="inline-flex w-full sm:w-auto items-center justify-center gap-2.5 px-6 py-4 text-sm text-white bg-white/10 hover:bg-white/20 border border-white/25 backdrop-blur-md font-semibold cursor-pointer rounded-2xl hover:scale-[1.02] transition-all focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white">
              Explorar tiendas
              <ArrowRight aria-hidden="true" className="w-4 h-4 shrink-0 text-emerald-200" />
            </Link>
          </div>

          <div className="pt-4 flex flex-wrap items-center justify-center gap-x-6 gap-y-3 text-sm text-[#ccfbf1]/90">
            <span className="flex items-center gap-1.5 font-semibold">
              <CheckCircle2 className="w-4 h-4 text-[#feae2c]" /> Sin comisiones fijas
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <ShieldCheck className="w-4 h-4 text-[#feae2c]" /> Tú defines los cupos
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <Zap className="w-4 h-4 text-[#feae2c]" /> Cupo reservado 10 min
            </span>
          </div>
        </MotionFade>
      </Container>
    </section>
  );
}

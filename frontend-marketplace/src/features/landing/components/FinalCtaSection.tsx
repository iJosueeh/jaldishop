import React from 'react';
import Link from 'next/link';
import { ArrowRight, Store, CheckCircle2, ShieldCheck, Zap, Rocket } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Button } from '@/shared/components/ui/Button';
import { env } from '@/core/config/env';

export function FinalCtaSection() {
  return (
    <section id="beneficios" className="py-24 sm:py-32 relative overflow-hidden bg-gradient-to-br from-[#005141] via-[#00382d] to-[#002820] text-white scroll-mt-20 sm:scroll-mt-24">
      {/* Background Accent Warm Glows */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-[#feae2c]/15 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-80 h-80 bg-[#ea580c]/15 blur-3xl rounded-full pointer-events-none" />

      <Container size="md">
        <MotionFade className="text-center space-y-6 relative z-10">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/15 border border-white/20 text-[#ccfbf1] text-xs sm:text-sm font-semibold backdrop-blur-md shadow-lg">
            <Rocket className="w-3.5 h-3.5 text-[#feae2c]" />
            Tu catálogo listo en menos de 3 minutos
          </div>

          <h2 className="font-display text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-tight max-w-3xl mx-auto drop-shadow-md">
            Dale a tu negocio y a tus clientes la tranquilidad que merecen.
          </h2>

          <p className="text-[#ccfbf1] text-base sm:text-lg max-w-xl mx-auto leading-relaxed font-normal">
            Únete a los negocios gastronómicos, reposteros, creadores de detalles y talleres que ya eliminaron la saturación y multiplican sus pedidos diarios con JaldiShop.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <a
              href={env.merchantUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full sm:w-auto"
            >
              <Button
                variant="terracotta"
                size="lg"
                className="w-full sm:w-auto text-base font-extrabold px-9 py-4 shadow-2xl shadow-black/50 cursor-pointer rounded-2xl border border-orange-300/30 hover:scale-[1.02] transition-all bg-[#ea580c] hover:bg-[#c2410c]"
                leftIcon={<Store className="w-5 h-5 text-white" />}
              >
                Crear Mi Tienda Gratis
              </Button>
            </a>

            <Link href="/tienda/panaderia-don-pepe" className="w-full sm:w-auto">
              <Button
                variant="outline"
                size="lg"
                className="w-full sm:w-auto text-white bg-white/15 hover:bg-white/25 border border-white/25 backdrop-blur-md font-bold cursor-pointer rounded-2xl hover:scale-[1.02] transition-all"
                rightIcon={<ArrowRight className="w-5 h-5 text-emerald-200" />}
              >
                Probar Experiencia de Compra
              </Button>
            </Link>
          </div>

          <div className="pt-8 flex flex-wrap items-center justify-center gap-6 text-xs text-[#ccfbf1]/90">
            <span className="flex items-center gap-1.5 font-semibold">
              <CheckCircle2 className="w-4 h-4 text-[#feae2c]" /> Sin comisiones fijas
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <ShieldCheck className="w-4 h-4 text-[#feae2c]" /> Control de capacidad inteligente
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <Zap className="w-4 h-4 text-[#feae2c]" /> 10 min de reserva para pagar
            </span>
          </div>
        </MotionFade>
      </Container>
    </section>
  );
}

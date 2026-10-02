import React from 'react';
import Link from 'next/link';
import { ArrowRight, Store, Sparkles, CheckCircle2, ShieldCheck, Zap } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Button } from '@/shared/components/ui/Button';
import { env } from '@/core/config/env';

export function FinalCtaSection() {
  return (
    <section id="beneficios" className="py-24 relative overflow-hidden bg-[#005141] text-white">
      {/* Background Accent Warm Glows */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-[#feae2c]/15 blur-[140px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-[#ea580c]/15 blur-[140px] pointer-events-none" />

      <Container size="md">
        <MotionFade className="text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/15 text-[#ccfbf1] text-xs sm:text-sm font-semibold backdrop-blur-sm shadow-md">
            <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
            Empieza a vender sin sobreventa hoy mismo
          </div>

          <h2 className="font-display text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-tight max-w-3xl mx-auto">
            Dale a tu negocio y a tus clientes la tranquilidad que merecen.
          </h2>

          <p className="text-[#ccfbf1] text-base sm:text-lg max-w-xl mx-auto leading-relaxed">
            Únete a los negocios gastronómicos, reposteros, creadores de detalles y talleres que ya eliminaron la saturación y multiplican sus ventas con JaldiShop.
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
                className="w-full sm:w-auto text-base font-extrabold px-9 py-4 shadow-xl shadow-[#ea580c]/30 cursor-pointer rounded-2xl border-0"
                leftIcon={<Store className="w-5 h-5 text-white" />}
              >
                Crear Mi Tienda Gratis
              </Button>
            </a>

            <Link href="/tienda/panaderia-don-pepe" className="w-full sm:w-auto">
              <Button
                variant="outline"
                size="lg"
                className="w-full sm:w-auto text-white bg-white/15 hover:bg-white/25 backdrop-blur-md font-bold cursor-pointer rounded-2xl border-0"
                rightIcon={<ArrowRight className="w-5 h-5" />}
              >
                Probar Experiencia de Compra
              </Button>
            </Link>
          </div>


          <div className="pt-8 flex flex-wrap items-center justify-center gap-6 text-xs text-[#ccfbf1]/80">
            <span className="flex items-center gap-1.5 font-semibold">
              <CheckCircle2 className="w-4 h-4 text-[#feae2c]" /> Sin costos ocultos
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <ShieldCheck className="w-4 h-4 text-[#feae2c]" /> Control de capacidad inteligente
            </span>
            <span className="flex items-center gap-1.5 font-semibold">
              <Zap className="w-4 h-4 text-[#feae2c]" /> Hold 10 min asegurado
            </span>
          </div>
        </MotionFade>
      </Container>
    </section>
  );
}

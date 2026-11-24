import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { ArrowRight, Sparkles } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { HowItWorksSection } from '@/features/landing/components/HowItWorksSection';
import { ComparisonSection } from '@/features/landing/components/ComparisonSection';
import { Footer } from '@/shared/components/layout/Footer';

export const metadata: Metadata = {
  title: '¿Cómo funciona? | JaldiShop',
  description:
    'Conoce cómo funciona JaldiShop para comprar con franjas horarias reales y sin sobreventa.',
};

export default function ComoFuncionaPage() {
  return (
    <div className="min-h-screen bg-[#02090a] text-white flex flex-col">
      <section className="pt-28 sm:pt-32 pb-10 bg-gradient-to-b from-[#072720]/70 to-[#090b0c] border-b border-[#1e2c31]">
        <Container size="lg">
          <div className="max-w-3xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Experiencia JaldiShop</span>
            </div>
            <h1 className="font-display text-3xl sm:text-5xl font-light tracking-tight text-white">
              Pedidos organizados por franjas horarias reales
            </h1>
            <p className="text-base text-[#99b3ad] leading-relaxed">
              Cada negocio define cuántos pedidos puede preparar por turno. Así compras con horario asegurado y coordinación directa por WhatsApp.
            </p>
            <div className="pt-2 flex flex-wrap gap-3">
              <Link href="/tiendas">
                <Button
                  variant="primary"
                  size="lg"
                  className="rounded-full bg-white text-[#02090a] hover:bg-stone-100 font-semibold"
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Explorar tiendas
                </Button>
              </Link>
              <Link href="/productos">
                <Button
                  variant="outline"
                  size="lg"
                  className="rounded-full border-white/20 text-white hover:bg-white/10"
                >
                  Ver productos
                </Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>

      <HowItWorksSection />
      <ComparisonSection />
      <Footer />
    </div>
  );
}

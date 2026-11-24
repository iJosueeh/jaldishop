import React from 'react';
import type { Metadata } from 'next';
import { Store, ArrowUpRight } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { TrustMetrics } from '@/features/landing/components/TrustMetrics';
import { BentoFeatures } from '@/features/landing/components/BentoFeatures';
import { Footer } from '@/shared/components/layout/Footer';
import { env } from '@/core/config/env';

export const metadata: Metadata = {
  title: 'Para Comercios MYPE | JaldiShop',
  description:
    'Herramientas para panaderías, pastelerías y negocios locales: control de cupos, catálogo digital y cobros con Mercado Pago.',
};

export default function ParaComerciosPage() {
  return (
    <div className="min-h-screen bg-[#02090a] text-white flex flex-col">
      <section className="pt-28 sm:pt-32 pb-10 bg-gradient-to-b from-[#072720]/70 to-[#090b0c] border-b border-[#1e2c31]">
        <Container size="lg">
          <div className="max-w-3xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider">
              <Store className="w-3.5 h-3.5" />
              <span>Herramientas para tu negocio</span>
            </div>
            <h1 className="font-display text-3xl sm:text-5xl font-light tracking-tight text-white">
              Organiza tus pedidos sin saturar tu cocina ni tu WhatsApp
            </h1>
            <p className="text-base text-[#99b3ad] leading-relaxed">
              Configura tu catálogo, limita tus cupos por horario y recibe pagos verificados con Mercado Pago, Yape o Plin.
            </p>
            <div className="pt-2">
              <a
                href={new URL('/register', env.merchantUrl).href}
                target="_blank"
                rel="noopener noreferrer"
              >
                <Button
                  variant="primary"
                  size="lg"
                  className="rounded-full bg-white text-[#02090a] hover:bg-stone-100 font-semibold"
                  rightIcon={<ArrowUpRight className="w-4 h-4" />}
                >
                  Crear mi Tienda Gratis
                </Button>
              </a>
            </div>
          </div>
        </Container>
      </section>

      <TrustMetrics />
      <BentoFeatures />
      <Footer />
    </div>
  );
}

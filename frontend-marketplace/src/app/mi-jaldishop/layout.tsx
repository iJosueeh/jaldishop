import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { Sparkles, ArrowLeft, ShieldCheck } from 'lucide-react';
import { CustomerNav } from '@/features/customer/components/CustomerNav';

export const metadata: Metadata = {
  title: 'Mi JaldiShop | Área Personal del Comprador',
  description: 'Gestiona tus pedidos programados, estadísticas, notificaciones y datos de contacto en JaldiShop.',
};

export default function CustomerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col">
      {/* 1. Header Banner Editorial (Midnight Greenhouse) */}
      <section className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] text-white pt-24 sm:pt-28 pb-7 sm:pb-9 border-b border-[#1e2c31] relative overflow-hidden">
        <div
          className="absolute -top-24 right-1/4 w-96 h-96 bg-[#005141]/30 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />

        <div className="max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 relative z-10 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Espacio Personal de Compras</span>
            </div>

            <div className="flex items-center gap-3 text-xs text-[#99b3ad]">
              <div className="flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
                <span>Capacidad garantizada sin sobreventa</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
            <div>
              <h1 className="font-display text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white tracking-tight">
                Mi JaldiShop
              </h1>
              <p className="text-sm sm:text-base text-[#99b3ad] mt-1 max-w-2xl leading-relaxed">
                Revisa tus compras programadas, estadísticas, notificaciones y mantén al día tus datos de entrega.
              </p>
            </div>

            <div className="sm:hidden pt-1">
              <Link
                href="/"
                className="inline-flex items-center gap-1.5 text-xs text-[#99b3ad] hover:text-[#36f4a4] font-medium"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Explorar comercios locales</span>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Marketplace Workspace: Responsive Sidebar + Main Content */}
      <div className="flex-1 max-w-[1280px] w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-10 lg:grid lg:grid-cols-[268px_minmax(0,1fr)] lg:gap-8 items-start">
        <CustomerNav />
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}

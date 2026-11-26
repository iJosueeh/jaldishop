import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { ArrowLeft, ShieldCheck } from 'lucide-react';
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
    <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] flex flex-col selection:bg-[#005141]/15 selection:text-[#005141]">
      {/* 1. Compact Account Context Bar (Midnight Greenhouse) */}
      <div className="bg-[#072720] text-white pt-20 sm:pt-24 pb-4 sm:pb-5 border-b border-[#133b32]">
        <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 xl:px-10 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <span className="font-display text-sm sm:text-base font-bold tracking-tight text-white">
              Mi JaldiShop
            </span>
            <span className="hidden sm:inline text-[#133b32]" aria-hidden="true">
              /
            </span>
            <span className="hidden sm:inline text-xs sm:text-sm text-[#99b3ad] truncate">
              Área personal del comprador
            </span>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <div className="hidden md:flex items-center gap-1.5 text-[#99b3ad]">
              <ShieldCheck className="w-4 h-4 text-[#36f4a4] shrink-0" />
              <span>Capacidad garantizada sin sobreventa</span>
            </div>
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#36f4a4] hover:underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#36f4a4] rounded"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Explorar comercios</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 2. Marketplace Workspace: Responsive Sidebar Rail + Main Content */}
      <div className="flex-1 max-w-[1440px] w-full mx-auto px-4 sm:px-6 lg:px-8 xl:px-10 py-6 sm:py-8 lg:grid lg:grid-cols-[236px_minmax(0,1fr)] xl:grid-cols-[248px_minmax(0,1fr)] lg:gap-8 items-start">
        <CustomerNav />
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}

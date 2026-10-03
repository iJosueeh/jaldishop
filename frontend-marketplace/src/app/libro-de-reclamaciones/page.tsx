import React from 'react';
import { Metadata } from 'next';
import Link from 'next/link';
import { ArrowLeft, BookOpen, Scale, ShieldCheck } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { ClaimsBookForm } from '@/features/legal/components/claims-book/ClaimsBookForm';
import { Footer } from '@/shared/components/layout/Footer';
import { LEGAL_META } from '@/features/legal/constants/legalMeta';

export const metadata: Metadata = {
  title: 'Libro de Reclamaciones Virtual | JaldiShop Perú',
  description:
    'Formulario oficial del Libro de Reclamaciones Virtual de JaldiShop conforme a la Ley N° 29571 y D.S. 011-2011-PCM en la República del Perú.',
  openGraph: {
    title: 'Libro de Reclamaciones Virtual | JaldiShop Perú',
    description:
      'Registra tu queja o reclamo conforme a las normas de INDECOPI en JaldiShop Plataformas Digitales S.A.C.',
    type: 'website',
    locale: 'es_PE',
  },
  alternates: {
    canonical: '/libro-de-reclamaciones',
  },
};

export default function LibroReclamacionesPage() {
  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col pt-16 sm:pt-20">
      {/* 1. Header / Hero Section (Dark Editorial) */}
      <section className="bg-[#0a0a09] text-white py-12 sm:py-16 relative overflow-hidden border-b border-white/10">
        {/* Subtle Ambient Craft Lighting */}
        <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#005141]/25 blur-3xl rounded-full pointer-events-none" />
        <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-[#feae2c]/10 blur-3xl rounded-full pointer-events-none" />

        <Container size="lg" className="relative z-10 space-y-5">
          {/* Breadcrumb & Legal Badge */}
          <div className="flex items-center justify-between">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-stone-400 hover:text-white transition-colors cursor-pointer group"
            >
              <ArrowLeft className="w-3.5 h-3.5 group-hover:-translate-x-0.5 transition-transform" />
              <span>Volver a JaldiShop</span>
            </Link>

            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-emerald-300 text-xs font-bold border border-white/10">
              <Scale className="w-3.5 h-3.5 text-[#feae2c]" />
              <span>Conforme a D.S. 011-2011-PCM</span>
            </span>
          </div>

          {/* Title & Description with ample line-height preventing text cramping */}
          <div className="space-y-3 max-w-3xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-xl bg-white/5 border border-white/10 text-xs font-bold text-amber-300">
              <BookOpen className="w-4 h-4 text-[#feae2c]" />
              <span>Hoja de Reclamación Virtual Oficial</span>
            </div>
            <h1 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
              Libro de Reclamaciones Virtual
            </h1>
            <p className="text-sm sm:text-base lg:text-lg text-stone-300 leading-relaxed font-normal">
              En cumplimiento del Código de Protección y Defensa del Consumidor (Ley N° 29571), ponemos a tu disposición este canal oficial para registrar cualquier queja o reclamo sobre tu experiencia.
            </p>
          </div>

          {/* Company Legal Notice Bar */}
          <div className="pt-4 border-t border-white/10 flex flex-wrap items-center justify-between gap-4 text-xs text-stone-400">
            <div className="flex flex-wrap items-center gap-4 sm:gap-6 font-medium">
              <span>{LEGAL_META.legalEntity}</span>
              <span>•</span>
              <span>Domicilio Legal: {LEGAL_META.city}</span>
              <span>•</span>
              <span className="flex items-center gap-1 text-emerald-300 font-bold">
                <ShieldCheck className="w-3.5 h-3.5" />
                Respuesta en máx. 15 días hábiles
              </span>
            </div>
          </div>
        </Container>
      </section>

      {/* 2. Main Form Section */}
      <section className="py-8 sm:py-12 flex-1">
        <Container size="md">
          <ClaimsBookForm />
        </Container>
      </section>

      {/* Global Footer */}
      <Footer />
    </div>
  );
}

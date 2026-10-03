import React from 'react';
import Link from 'next/link';
import { ArrowLeft, Sparkles, Calendar, Mail } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';

interface CompanyLayoutProps {
  title: string;
  subtitle: string;
  badgeText: string;
  badgeIcon?: React.ReactNode;
  lastUpdated?: string;
  children: React.ReactNode;
}

export function CompanyLayout({
  title,
  subtitle,
  badgeText,
  badgeIcon,
  lastUpdated = 'Octubre 2026',
  children,
}: CompanyLayoutProps) {
  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col pt-16 sm:pt-20">
      {/* 1. Header / Hero Section (Dark Editorial Styling matching Landing & Legal) */}
      <section className="bg-[#0a0a09] text-white py-12 sm:py-16 relative overflow-hidden border-b border-white/10">
        {/* Subtle Ambient Craft Lighting */}
        <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#005141]/25 blur-3xl rounded-full pointer-events-none" />
        <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-[#ea580c]/15 blur-3xl rounded-full pointer-events-none" />

        <Container size="lg" className="relative z-10 space-y-6">
          {/* Breadcrumb & Back Link */}
          <div className="flex items-center justify-between">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-stone-400 hover:text-white transition-colors cursor-pointer group"
            >
              <ArrowLeft className="w-3.5 h-3.5 group-hover:-translate-x-0.5 transition-transform" />
              <span>Volver a JaldiShop</span>
            </Link>

            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-emerald-300 text-xs font-bold border border-white/10">
              {badgeIcon || <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />}
              <span>{badgeText}</span>
            </span>
          </div>

          {/* Title and Subtitle with ample line-height preventing text cramping */}
          <div className="space-y-3 max-w-3xl">
            <h1 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
              {title}
            </h1>
            <p className="text-sm sm:text-base lg:text-lg text-stone-300 leading-relaxed font-normal">
              {subtitle}
            </p>
          </div>

          {/* Meta Info Bar */}
          <div className="pt-4 border-t border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs text-stone-400">
            <div className="flex items-center gap-1.5 font-medium">
              <Calendar className="w-3.5 h-3.5 text-[#feae2c]" />
              <span>Última actualización: {lastUpdated}</span>
            </div>

            <div className="flex items-center gap-1.5 font-medium text-stone-400">
              <Mail className="w-3.5 h-3.5 text-emerald-400" />
              <span>JaldiShop Plataformas Digitales S.A.C. · Lima, Perú</span>
            </div>
          </div>
        </Container>
      </section>

      {/* 2. Main Content Body */}
      <main className="flex-1 py-12 sm:py-16">
        <Container size="lg">
          <div className="max-w-4xl mx-auto space-y-10 text-stone-800">
            {children}
          </div>
        </Container>
      </main>
    </div>
  );
}

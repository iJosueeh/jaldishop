'use client';

import React, { useState, useRef } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Scale,
  Calendar,
  MapPin,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  BookOpen,
  FileText,
  ArrowUp,
} from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { LegalDocument } from '../types/legal.types';
import { LegalTableOfContents } from './LegalTableOfContents';
import { LegalSection } from './LegalSection';
import { LegalPrintButton } from './LegalPrintButton';

interface LegalLayoutProps {
  document: LegalDocument;
}

export function LegalLayout({ document }: LegalLayoutProps) {
  const [activeClauseIndex, setActiveClauseIndex] = useState<number>(0);
  const [viewMode, setViewMode] = useState<'tabs' | 'continuous'>('tabs');
  const mainContentRef = useRef<HTMLDivElement>(null);

  const totalClauses = document.clauses.length;
  const currentClause = document.clauses[activeClauseIndex] || document.clauses[0];

  const handleSelectClause = (index: number) => {
    setActiveClauseIndex(index);
    if (viewMode === 'tabs') {
      if (mainContentRef.current) {
        const topOffset = mainContentRef.current.getBoundingClientRect().top + window.scrollY - 110;
        window.scrollTo({ top: Math.max(0, topOffset), behavior: 'smooth' });
      }
    } else {
      const targetClause = document.clauses[index];
      if (targetClause && typeof window !== 'undefined') {
        const el = window.document.getElementById(targetClause.id);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }
    }
  };

  const handlePrevious = () => {
    if (activeClauseIndex > 0) {
      handleSelectClause(activeClauseIndex - 1);
    }
  };

  const handleNext = () => {
    if (activeClauseIndex < totalClauses - 1) {
      handleSelectClause(activeClauseIndex + 1);
    }
  };

  const scrollToTop = () => {
    if (mainContentRef.current) {
      const topOffset = mainContentRef.current.getBoundingClientRect().top + window.scrollY - 110;
      window.scrollTo({ top: Math.max(0, topOffset), behavior: 'smooth' });
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col pt-16 sm:pt-20">
      {/* 1. Header / Hero Section (Editorial Craft Styling) */}
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
              <Scale className="w-3.5 h-3.5 text-[#feae2c]" />
              <span>{document.badgeText}</span>
            </span>
          </div>

          {/* Title and Subtitle with ample line-height preventing text cramping */}
          <div className="space-y-3 max-w-3xl">
            <h1 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
              {document.title}
            </h1>
            <p className="text-sm sm:text-base lg:text-lg text-stone-300 leading-relaxed font-normal">
              {document.subtitle}
            </p>
          </div>

          {/* Meta Info Bar (Last update, Jurisdiction, Print Action) */}
          <div className="pt-4 border-t border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs text-stone-400">
            <div className="flex flex-wrap items-center gap-4 sm:gap-6 font-medium">
              <div className="flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-[#feae2c]" />
                <span>Actualizado: {document.lastUpdated}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                <span>{document.jurisdiction}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Versión {document.version}</span>
              </div>
            </div>

            <LegalPrintButton />
          </div>
        </Container>
      </section>

      {/* 2. Main Content Grid (1 col Mobile, 4 cols Desktop) */}
      <section className="py-8 sm:py-12 flex-1">
        <Container size="lg">
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 lg:gap-12 items-start">
            {/* Left Sidebar: Table of Contents / Tabs Controller */}
            <div className="lg:col-span-1">
              <LegalTableOfContents
                clauses={document.clauses}
                activeClauseIndex={activeClauseIndex}
                onSelectClause={handleSelectClause}
                viewMode={viewMode}
              />
            </div>

            {/* Right Main Body: Active Clause or Full Document */}
            <main
              ref={mainContentRef}
              className="lg:col-span-3 bg-white rounded-3xl border border-stone-200/90 shadow-sm p-5 sm:p-8 md:p-10"
            >
              {/* Document Header Overview for Print */}
              <div className="hidden print:block pb-6 mb-6 border-b border-stone-200">
                <h2 className="text-2xl font-black text-black">{document.title}</h2>
                <p className="text-xs text-stone-600 mt-1">
                  Documento Oficial de JaldiShop Perú • Vigencia desde: {document.effectiveDate}
                </p>
              </div>

              {/* Interactive Toolbar: Mode Switcher & Progress Indicator (Screen Only) */}
              <div className="print:hidden pb-6 mb-6 border-b border-stone-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-black uppercase tracking-wider text-[#005141] bg-emerald-50 px-2.5 py-1 rounded-md border border-emerald-200">
                      {viewMode === 'tabs'
                        ? `Cláusula ${activeClauseIndex + 1} de ${totalClauses}`
                        : 'Documento Completo'}
                    </span>
                    {viewMode === 'tabs' && (
                      <span className="text-xs font-medium text-stone-500">
                        ({Math.round(((activeClauseIndex + 1) / totalClauses) * 100)}% leído)
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-stone-500">
                    {viewMode === 'tabs'
                      ? 'Lectura enfocada: visualiza una cláusula a la vez sin fatiga de scroll.'
                      : 'Lectura corrida: desplázate por todo el documento de corrido.'}
                  </p>
                </div>

                {/* View Mode Switcher Pills */}
                <div className="inline-flex items-center p-1 bg-stone-100 rounded-xl border border-stone-200/80 shrink-0 self-start sm:self-auto">
                  <button
                    type="button"
                    onClick={() => setViewMode('tabs')}
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                      viewMode === 'tabs'
                        ? 'bg-white text-[#005141] shadow-2xs font-extrabold'
                        : 'text-stone-600 hover:text-[#1c1917]'
                    }`}
                    aria-label="Ver por cláusulas (modo pestañas)"
                  >
                    <BookOpen className="w-3.5 h-3.5" />
                    <span>Por Cláusulas</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setViewMode('continuous')}
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                      viewMode === 'continuous'
                        ? 'bg-white text-[#005141] shadow-2xs font-extrabold'
                        : 'text-stone-600 hover:text-[#1c1917]'
                    }`}
                    aria-label="Ver documento completo continuo"
                  >
                    <FileText className="w-3.5 h-3.5" />
                    <span>Ver Completo</span>
                  </button>
                </div>
              </div>

              {/* Progress Bar (Tabs Mode Only) */}
              {viewMode === 'tabs' && (
                <div className="print:hidden w-full bg-stone-100 h-1.5 rounded-full overflow-hidden mb-6">
                  <div
                    className="bg-[#005141] h-full transition-all duration-300 ease-out rounded-full"
                    style={{ width: `${((activeClauseIndex + 1) / totalClauses) * 100}%` }}
                  />
                </div>
              )}

              {/* SCREEN CONTENT AREA */}
              <div className="print:hidden">
                {viewMode === 'tabs' ? (
                  <div>
                    {/* Render Only the Active Clause */}
                    <LegalSection clause={currentClause} index={activeClauseIndex} />

                    {/* Sequential Navigation Toolbar */}
                    <div className="mt-8 pt-6 border-t border-stone-200/80 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
                      <button
                        type="button"
                        onClick={handlePrevious}
                        disabled={activeClauseIndex === 0}
                        className={`inline-flex items-center justify-center sm:justify-start gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
                          activeClauseIndex === 0
                            ? 'opacity-40 cursor-not-allowed bg-stone-50 text-stone-400 border-stone-200'
                            : 'bg-white text-stone-700 border-stone-200 hover:bg-stone-50 hover:border-stone-300 shadow-2xs'
                        }`}
                      >
                        <ChevronLeft className="w-4 h-4" />
                        <div className="text-left">
                          <span className="block text-[10px] text-stone-500 uppercase font-semibold">Anterior</span>
                          <span className="truncate max-w-[160px] block">
                            {activeClauseIndex > 0
                              ? document.clauses[activeClauseIndex - 1].shortTitle || `Cláusula ${activeClauseIndex}`
                              : 'Inicio'}
                          </span>
                        </div>
                      </button>

                      <div className="text-center hidden md:block">
                        <span className="text-xs font-bold text-stone-500">
                          Cláusula {activeClauseIndex + 1} de {totalClauses}
                        </span>
                      </div>

                      <button
                        type="button"
                        onClick={handleNext}
                        disabled={activeClauseIndex === totalClauses - 1}
                        className={`inline-flex items-center justify-center sm:justify-end gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
                          activeClauseIndex === totalClauses - 1
                            ? 'opacity-40 cursor-not-allowed bg-stone-50 text-stone-400 border-stone-200'
                            : 'bg-[#005141] text-white border-[#005141] hover:bg-[#003d31] shadow-2xs'
                        }`}
                      >
                        <div className="text-right">
                          <span className="block text-[10px] text-emerald-200 uppercase font-semibold">Siguiente</span>
                          <span className="truncate max-w-[160px] block">
                            {activeClauseIndex < totalClauses - 1
                              ? document.clauses[activeClauseIndex + 1].shortTitle || `Cláusula ${activeClauseIndex + 2}`
                              : 'Final'}
                          </span>
                        </div>
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="divide-y divide-stone-100">
                    {/* Render All Clauses Sequentially */}
                    {document.clauses.map((clause, index) => (
                      <LegalSection key={clause.id} clause={clause} index={index} />
                    ))}

                    <div className="pt-6 flex justify-end">
                      <button
                        type="button"
                        onClick={scrollToTop}
                        className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold text-stone-600 bg-stone-100 hover:bg-stone-200 transition-colors cursor-pointer"
                      >
                        <ArrowUp className="w-3.5 h-3.5" />
                        <span>Volver al inicio del documento</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* PRINT VIEW (Always renders every single clause sequentially) */}
              <div className="hidden print:block divide-y divide-stone-200">
                {document.clauses.map((clause, index) => (
                  <LegalSection key={clause.id} clause={clause} index={index} />
                ))}
              </div>

              {/* Document Footer Signoff */}
              <div className="pt-8 mt-8 border-t border-stone-200/80 text-xs text-stone-600 space-y-2">
                <p className="leading-relaxed">
                  El presente documento entra en vigor de forma inmediata tras su publicación.
                  Cualquier actualización sustancial será notificada en la Plataforma con debida antelación.
                </p>
                <p className="font-semibold text-stone-700">
                  JaldiShop Plataformas Digitales S.A.C. — Lima, República del Perú.
                </p>
              </div>
            </main>
          </div>
        </Container>
      </section>
    </div>
  );
}

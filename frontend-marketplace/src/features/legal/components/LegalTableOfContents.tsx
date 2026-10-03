'use client';

import React, { useEffect, useState } from 'react';
import { LegalClause } from '../types/legal.types';
import { ChevronRight, Bookmark } from 'lucide-react';

interface LegalTableOfContentsProps {
  clauses: LegalClause[];
  activeClauseIndex?: number;
  onSelectClause?: (index: number) => void;
  viewMode?: 'tabs' | 'continuous';
}

export function LegalTableOfContents({
  clauses,
  activeClauseIndex = 0,
  onSelectClause,
  viewMode = 'tabs',
}: LegalTableOfContentsProps) {
  const [internalActiveId, setInternalActiveId] = useState<string>(
    clauses[activeClauseIndex]?.id || clauses[0]?.id || ''
  );

  // Sync internal active id when activeClauseIndex changes
  useEffect(() => {
    if (clauses[activeClauseIndex]) {
      setInternalActiveId(clauses[activeClauseIndex].id);
    }
  }, [activeClauseIndex, clauses]);

  // In continuous mode, observe scroll intersection
  useEffect(() => {
    if (viewMode !== 'continuous') return;
    if (typeof window === 'undefined' || !window.IntersectionObserver || !clauses.length) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setInternalActiveId(entry.target.id);
          }
        });
      },
      {
        rootMargin: '-80px 0px -60% 0px',
        threshold: 0,
      }
    );

    clauses.forEach((c) => {
      const el = document.getElementById(c.id);
      if (el) observer.observe(el);
    });

    return () => observer.disconnect();
  }, [clauses, viewMode]);

  const handleClauseClick = (index: number, clauseId: string) => {
    setInternalActiveId(clauseId);
    if (onSelectClause) {
      onSelectClause(index);
    } else {
      const element = document.getElementById(clauseId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  };

  const currentActiveId =
    viewMode === 'tabs' ? clauses[activeClauseIndex]?.id || internalActiveId : internalActiveId;

  return (
    <>
      {/* 1. Mobile & Tablet Sticky Quick-Navigation Bar (< 1024px) */}
      <div className="lg:hidden sticky top-16 sm:top-20 z-30 -mx-4 px-4 sm:-mx-6 sm:px-6 py-2.5 bg-[#faf7f2]/95 backdrop-blur-md border-b border-stone-200/90 shadow-2xs print:hidden">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
          <span className="text-[11px] font-bold text-stone-600 uppercase tracking-wider shrink-0 flex items-center gap-1 pl-1">
            <Bookmark className="w-3 h-3 text-[#005141]" />
            <span>Índice:</span>
          </span>
          {clauses.map((clause, idx) => {
            const isActive = currentActiveId === clause.id;
            return (
              <button
                key={clause.id}
                type="button"
                onClick={() => handleClauseClick(idx, clause.id)}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer flex items-center gap-1.5 border ${
                  isActive
                    ? 'bg-[#005141] text-white border-[#005141] shadow-xs scale-[1.02]'
                    : 'bg-white text-stone-600 border-stone-200 hover:border-stone-300 hover:bg-stone-50'
                }`}
              >
                <span className="text-[10px] opacity-75 font-mono">
                  {(idx + 1).toString().padStart(2, '0')}
                </span>
                <span>{clause.shortTitle || clause.title}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Desktop Sticky Sidebar Navigation (>= 1024px) */}
      <aside className="hidden lg:block w-full sticky top-28 space-y-4 print:hidden">
        <div className="p-5 rounded-3xl bg-white border border-stone-200/90 shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-stone-100">
            <div className="w-7 h-7 rounded-xl bg-emerald-50 text-[#005141] flex items-center justify-center font-bold">
              <Bookmark className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-black uppercase tracking-wider text-[#1c1917]">
                Tabla de Contenidos
              </h3>
              <p className="text-[10px] text-stone-600 font-medium">
                {viewMode === 'tabs' ? 'Navegación por pestañas' : 'Navegación rápida de lectura'}
              </p>
            </div>
          </div>

          <nav className="space-y-1">
            {clauses.map((clause, idx) => {
              const isActive = currentActiveId === clause.id;
              return (
                <button
                  key={clause.id}
                  type="button"
                  onClick={() => handleClauseClick(idx, clause.id)}
                  className={`w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer flex items-center justify-between group border ${
                    isActive
                      ? 'bg-emerald-50/90 text-[#005141] font-black border-emerald-200/90 shadow-2xs'
                      : 'text-stone-600 hover:text-[#1c1917] hover:bg-stone-50 border-transparent'
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0 pr-2">
                    <span
                      className={`text-[10px] font-mono px-1.5 py-0.5 rounded-md shrink-0 transition-colors ${
                        isActive
                          ? 'bg-[#005141] text-white font-bold'
                          : 'bg-stone-100 text-stone-600 group-hover:bg-stone-200'
                      }`}
                    >
                      {(idx + 1).toString().padStart(2, '0')}
                    </span>
                    <span className="truncate leading-relaxed">
                      {clause.shortTitle || clause.title}
                    </span>
                  </div>
                  <ChevronRight
                    className={`w-3.5 h-3.5 shrink-0 transition-transform ${
                      isActive
                        ? 'text-[#005141] translate-x-0.5'
                        : 'text-stone-300 group-hover:text-stone-500'
                    }`}
                  />
                </button>
              );
            })}
          </nav>
        </div>
      </aside>
    </>
  );
}

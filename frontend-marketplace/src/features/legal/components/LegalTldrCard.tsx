import React from 'react';
import { Sparkles, Check } from 'lucide-react';
import { LegalTldr } from '../types/legal.types';

interface LegalTldrCardProps {
  tldr: LegalTldr;
}

export function LegalTldrCard({ tldr }: LegalTldrCardProps) {
  return (
    <div className="my-5 p-4 sm:p-5 md:p-6 rounded-2xl sm:rounded-3xl bg-gradient-to-br from-[#f0fdfa] via-white to-[#faf7f2] border border-teal-200/80 shadow-xs space-y-3.5">
      {/* Header Tag */}
      <div className="flex items-center gap-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#005141] text-white text-[11px] sm:text-xs font-black tracking-wide shadow-2xs">
          <Sparkles className="w-3 h-3 text-[#feae2c]" />
          <span>En pocas palabras (Resumen)</span>
        </span>
      </div>

      {/* Main Title & Summary with comfortable leading and responsive sizing */}
      <div className="space-y-1.5">
        <h4 className="text-sm sm:text-base font-bold text-[#1c1917] leading-snug">
          {tldr.title}
        </h4>
        <p className="text-xs sm:text-sm text-[#44403c] leading-relaxed">
          {tldr.summary}
        </p>
      </div>

      {/* Key Takeaways Points with generous padding avoiding text squeeze */}
      {tldr.keyPoints && tldr.keyPoints.length > 0 && (
        <div className="pt-2 border-t border-teal-100/90 grid grid-cols-1 gap-2">
          {tldr.keyPoints.map((point, index) => (
            <div key={index} className="flex items-start gap-2.5 text-xs sm:text-sm text-[#292524]">
              <div className="w-4 h-4 rounded-full bg-emerald-100 text-[#005141] flex items-center justify-center shrink-0 mt-0.5">
                <Check className="w-2.5 h-2.5 stroke-[3]" />
              </div>
              <span className="leading-relaxed flex-1">{point}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

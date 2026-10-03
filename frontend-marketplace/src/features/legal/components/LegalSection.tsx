import React from 'react';
import { ShieldCheck, Clock, AlertTriangle, Info, CheckCircle2 } from 'lucide-react';
import { LegalClause, LegalCalloutVariant } from '../types/legal.types';
import { LegalTldrCard } from './LegalTldrCard';

interface LegalSectionProps {
  clause: LegalClause;
  index: number;
}

function CalloutIcon({ variant }: { variant: LegalCalloutVariant }) {
  switch (variant) {
    case 'shield':
      return <ShieldCheck className="w-5 h-5 text-[#005141] shrink-0" />;
    case 'clock':
      return <Clock className="w-5 h-5 text-[#ea580c] shrink-0" />;
    case 'warning':
      return <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />;
    case 'info':
    default:
      return <Info className="w-5 h-5 text-sky-600 shrink-0" />;
  }
}

function getCalloutStyles(variant: LegalCalloutVariant) {
  switch (variant) {
    case 'shield':
      return 'bg-emerald-50/70 border-emerald-200/90 text-emerald-950';
    case 'clock':
      return 'bg-orange-50/70 border-orange-200/90 text-orange-950';
    case 'warning':
      return 'bg-amber-50/80 border-amber-200/90 text-amber-950';
    case 'info':
    default:
      return 'bg-sky-50/70 border-sky-200/90 text-sky-950';
  }
}

export function LegalSection({ clause }: LegalSectionProps) {
  return (
    <section
      id={clause.id}
      className="scroll-mt-28 sm:scroll-mt-32 pt-8 sm:pt-10 pb-8 sm:pb-10 border-b border-stone-200/80 last:border-b-0 space-y-5"
    >
      {/* Section Header with Anchor Link & Badge */}
      <div className="space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          {clause.badge && (
            <span className="text-[11px] font-extrabold uppercase tracking-wider px-2.5 py-0.5 rounded-md bg-stone-100 text-stone-700 border border-stone-200/80">
              {clause.badge}
            </span>
          )}
        </div>

        <h2 className="font-display text-xl sm:text-2xl lg:text-3xl font-extrabold text-[#1c1917] tracking-tight leading-tight sm:leading-snug">
          <a
            href={`#${clause.id}`}
            className="hover:text-[#005141] transition-colors group inline-flex items-center gap-2"
          >
            <span>{clause.title}</span>
            <span className="opacity-0 group-hover:opacity-100 text-stone-400 text-base sm:text-lg font-normal transition-opacity">
              #
            </span>
          </a>
        </h2>
      </div>

      {/* Human-Readable TLDR Callout Card */}
      {clause.tldr && <LegalTldrCard tldr={clause.tldr} />}

      {/* Main Clause Body Items */}
      <div className="space-y-4 sm:space-y-5 text-sm sm:text-base text-[#44403c] leading-relaxed sm:leading-loose">
        {clause.content.map((item, idx) => {
          if (item.type === 'paragraph') {
            return (
              <p key={idx} className="text-[#3b3835]">
                {item.text}
              </p>
            );
          }

          if (item.type === 'list') {
            return (
              <ul key={idx} className="space-y-3 pl-1 sm:pl-2">
                {item.items.map((listItem, listIdx) => (
                  <li key={listIdx} className="flex items-start gap-3 text-sm sm:text-base">
                    <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0 mt-1 sm:mt-1.5" />
                    <span className="leading-relaxed flex-1 text-[#292524]">{listItem}</span>
                  </li>
                ))}
              </ul>
            );
          }

          if (item.type === 'callout') {
            return (
              <div
                key={idx}
                className={`p-4 sm:p-5 rounded-2xl border ${getCalloutStyles(
                  item.variant
                )} flex items-start gap-3.5 my-4`}
              >
                <CalloutIcon variant={item.variant} />
                <div className="space-y-1 flex-1">
                  <h5 className="font-bold text-sm sm:text-base leading-snug">
                    {item.title}
                  </h5>
                  <p className="text-xs sm:text-sm leading-relaxed opacity-90">
                    {item.text}
                  </p>
                </div>
              </div>
            );
          }

          if (item.type === 'table') {
            return (
              <div
                key={idx}
                className="my-5 -mx-4 px-4 sm:mx-0 sm:px-0 overflow-x-auto no-scrollbar"
              >
                <div className="inline-block min-w-full align-middle">
                  <div className="overflow-hidden rounded-2xl border border-stone-200 shadow-2xs">
                    <table className="min-w-full divide-y divide-stone-200 text-left">
                      <thead className="bg-[#faf7f2]">
                        <tr>
                          {item.headers.map((header, hIdx) => (
                            <th
                              key={hIdx}
                              scope="col"
                              className="px-4 sm:px-5 py-3 text-xs sm:text-sm font-black text-[#1c1917] tracking-wide"
                            >
                              {header}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-stone-100 bg-white">
                        {item.rows.map((row, rIdx) => (
                          <tr key={rIdx} className="hover:bg-stone-50/70 transition-colors">
                            {row.map((cell, cIdx) => (
                              <td
                                key={cIdx}
                                className={`px-4 sm:px-5 py-3.5 text-xs sm:text-sm leading-relaxed ${
                                  cIdx === 0
                                    ? 'font-bold text-[#1c1917] whitespace-nowrap'
                                    : 'text-[#57534e]'
                                }`}
                              >
                                {cell}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            );
          }

          return null;
        })}
      </div>
    </section>
  );
}

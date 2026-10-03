'use client';

import React from 'react';
import { Printer } from 'lucide-react';

interface LegalPrintButtonProps {
  className?: string;
}

export function LegalPrintButton({ className = '' }: LegalPrintButtonProps) {
  const handlePrint = () => {
    if (typeof window !== 'undefined') {
      window.print();
    }
  };

  return (
    <button
      type="button"
      onClick={handlePrint}
      aria-label="Imprimir o guardar en PDF este documento legal"
      className={`inline-flex items-center justify-center gap-2 px-3.5 py-2 rounded-xl text-xs font-bold text-stone-700 bg-white border border-stone-200 shadow-xs hover:border-[#005141] hover:text-[#005141] hover:bg-stone-50 transition-all duration-200 cursor-pointer print:hidden shrink-0 ${className}`}
    >
      <Printer className="w-3.5 h-3.5 text-[#005141]" />
      <span>Descargar / Imprimir PDF</span>
    </button>
  );
}

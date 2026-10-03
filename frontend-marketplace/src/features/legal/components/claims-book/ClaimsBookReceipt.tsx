'use client';

import { Printer, CheckCircle2, ShieldCheck, RotateCcw } from 'lucide-react';
import { ClaimSubmissionResult } from '../../types/legal.types';
import { LEGAL_META } from '../../constants/legalMeta';

interface ClaimsBookReceiptProps {
  receipt: ClaimSubmissionResult;
  onReset: () => void;
}

export function ClaimsBookReceipt({ receipt, onReset }: ClaimsBookReceiptProps) {
  const { claimCode, submittedAt, maxResponseDate, data } = receipt;

  const handlePrint = () => {
    if (typeof window !== 'undefined') {
      window.print();
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Success Banner (Hidden on Print) */}
      <div className="p-5 sm:p-6 rounded-3xl bg-emerald-50 border border-emerald-200 text-emerald-950 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 print:hidden shadow-xs">
        <div className="flex items-start gap-3.5">
          <div className="w-10 h-10 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shrink-0 mt-0.5 shadow-xs">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h3 className="font-display font-extrabold text-lg text-emerald-950 leading-snug">
              ¡Hoja de Reclamación Registrada con Éxito!
            </h3>
            <p className="text-xs sm:text-sm text-emerald-800 leading-relaxed">
              Hemos enviado una copia fiel a <strong>{data.email}</strong>. Conforme al Art. 24 de la Ley N° 29571, daremos respuesta en un plazo no mayor a 15 días hábiles.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto shrink-0">
          <button
            type="button"
            onClick={handlePrint}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#005141] text-white text-xs font-bold hover:bg-[#00382d] transition-all cursor-pointer shadow-xs"
          >
            <Printer className="w-4 h-4" />
            <span>Imprimir Hoja</span>
          </button>
        </div>
      </div>

      {/* Official Sheet (Hoja de Reclamación del Libro de Reclamaciones Virtual) */}
      <div className="bg-white rounded-3xl border-2 border-stone-300 p-6 sm:p-8 md:p-10 space-y-6 shadow-sm print:border-none print:shadow-none print:p-0">
        {/* Sheet Header */}
        <div className="pb-6 border-b-2 border-stone-200 flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div className="space-y-1.5">
            <span className="text-[11px] font-black uppercase tracking-widest text-[#005141] bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-200">
              Formato Oficial Conforme a D.S. 011-2011-PCM
            </span>
            <h2 className="font-display text-xl sm:text-2xl font-black text-[#1c1917] tracking-tight">
              LIBRO DE RECLAMACIONES VIRTUAL
            </h2>
            <p className="text-xs text-stone-600">
              {LEGAL_META.legalEntity} • {LEGAL_META.city}
            </p>
          </div>

          {/* Tracking Box */}
          <div className="bg-stone-50 border-2 border-stone-300 rounded-2xl p-4 text-center sm:text-right shrink-0 space-y-1">
            <span className="text-[10px] font-black uppercase tracking-wider text-stone-500 block">
              Código Único de Registro
            </span>
            <span className="font-mono text-base sm:text-lg font-black text-[#005141] block">
              {claimCode}
            </span>
            <span className="text-[10px] text-stone-500 block">Fecha: {submittedAt}</span>
          </div>
        </div>

        {/* Section 1: Consumidor */}
        <div className="space-y-3">
          <h4 className="text-xs font-black uppercase tracking-wider text-stone-500 bg-stone-100 px-3 py-1 rounded-lg">
            1. Identificación del Consumidor Reclamante
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5 text-xs">
            <div>
              <span className="text-stone-400 block font-medium">Nombre completo:</span>
              <strong className="text-[#1c1917] text-sm">{data.fullName}</strong>
            </div>
            <div>
              <span className="text-stone-400 block font-medium">Documento ({data.documentType}):</span>
              <strong className="text-[#1c1917] text-sm font-mono">{data.documentNumber}</strong>
            </div>
            <div>
              <span className="text-stone-400 block font-medium">Teléfono de contacto:</span>
              <strong className="text-[#1c1917] text-sm">{data.phone}</strong>
            </div>
            <div className="sm:col-span-2">
              <span className="text-stone-400 block font-medium">Correo electrónico:</span>
              <strong className="text-[#1c1917] text-sm">{data.email}</strong>
            </div>
            <div className="sm:col-span-2 lg:col-span-3">
              <span className="text-stone-400 block font-medium">Domicilio:</span>
              <span className="text-[#1c1917] text-xs">
                {data.address} — {data.district}, {data.province}, {data.department}
              </span>
            </div>
            {data.isMinor && data.parentName && (
              <div className="sm:col-span-2 lg:col-span-3 bg-amber-50/70 p-2.5 rounded-xl border border-amber-200">
                <span className="text-amber-800 text-[11px] block font-semibold">
                  Representante legal (Padre/Madre/Tutor): {data.parentName}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Section 2: Bien Contratado */}
        <div className="space-y-3 pt-2">
          <h4 className="text-xs font-black uppercase tracking-wider text-stone-500 bg-stone-100 px-3 py-1 rounded-lg">
            2. Identificación del Bien Contratado
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5 text-xs">
            <div>
              <span className="text-stone-400 block font-medium">Naturaleza:</span>
              <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-extrabold bg-[#faf7f2] border border-stone-200 text-[#1c1917] mt-0.5">
                {data.goodType}
              </span>
            </div>
            <div>
              <span className="text-stone-400 block font-medium">Monto reclamado:</span>
              <strong className="text-[#1c1917] text-sm font-mono">
                S/ {Number(data.claimedAmount).toFixed(2)}
              </strong>
            </div>
            <div>
              <span className="text-stone-400 block font-medium">N° de Pedido / Tienda:</span>
              <span className="text-[#1c1917] text-xs font-mono">
                {data.orderNumber || data.storeName || 'No especificado'}
              </span>
            </div>
            <div className="sm:col-span-3">
              <span className="text-stone-400 block font-medium">Descripción del bien o servicio:</span>
              <p className="text-[#292524] text-xs leading-relaxed bg-[#faf7f2] p-2.5 rounded-xl border border-stone-200/80 mt-1">
                {data.goodDescription}
              </p>
            </div>
          </div>
        </div>

        {/* Section 3: Detalle de la Reclamación */}
        <div className="space-y-3 pt-2">
          <h4 className="text-xs font-black uppercase tracking-wider text-stone-500 bg-stone-100 px-3 py-1 rounded-lg">
            3. Detalle de la Reclamación y Pedido del Consumidor
          </h4>
          <div className="space-y-3 text-xs">
            <div className="flex items-center gap-2">
              <span className="text-stone-400 font-medium">Tipo de Reclamación:</span>
              <span
                className={`px-3 py-1 rounded-full font-black text-xs ${
                  data.claimType === 'RECLAMO'
                    ? 'bg-rose-100 text-rose-800 border border-rose-200'
                    : 'bg-amber-100 text-amber-800 border border-amber-200'
                }`}
              >
                {data.claimType}
              </span>
              <span className="text-[11px] text-stone-500 hidden sm:inline">
                ({data.claimType === 'RECLAMO' ? 'Disconformidad con el producto o servicio' : 'Malestar en la atención'})
              </span>
            </div>

            <div>
              <span className="text-stone-400 block font-medium">Detalle de los hechos:</span>
              <div className="text-[#292524] text-xs leading-relaxed bg-stone-50 p-3.5 rounded-xl border border-stone-200 mt-1 whitespace-pre-line">
                {data.claimDetail}
              </div>
            </div>

            <div>
              <span className="text-stone-400 block font-medium">Pedido concreto del consumidor:</span>
              <div className="text-[#292524] text-xs leading-relaxed bg-[#f0fdfa] p-3.5 rounded-xl border border-teal-200 mt-1 font-medium whitespace-pre-line">
                {data.consumerRequest}
              </div>
            </div>
          </div>
        </div>

        {/* Legal Disclaimer Box */}
        <div className="pt-4 border-t border-stone-200 space-y-2 text-[11px] text-stone-500 leading-relaxed bg-stone-50/70 p-4 rounded-2xl">
          <div className="flex items-center gap-1.5 font-bold text-stone-700">
            <ShieldCheck className="w-4 h-4 text-[#005141]" />
            <span>Garantía de Plazo Legal — Ley N° 29571</span>
          </div>
          <p>
            La formulación del reclamo no impide acudir a otras vías de solución de controversias ni es requisito previo para interponer una denuncia ante el INDECOPI. El proveedor deberá dar respuesta al reclamo en un plazo no mayor de quince (15) días hábiles improrrogables, fecha límite estimada: <strong>{maxResponseDate}</strong>.
          </p>
        </div>
      </div>

      {/* Bottom Action Footer (Hidden on Print) */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2 print:hidden">
        <button
          type="button"
          onClick={onReset}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-stone-600 hover:text-[#005141] transition-colors cursor-pointer"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Registrar otra reclamación</span>
        </button>

        <button
          type="button"
          onClick={handlePrint}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-[#005141] text-white text-xs font-bold hover:bg-[#00382d] transition-all cursor-pointer shadow-xs"
        >
          <Printer className="w-4 h-4" />
          <span>Descargar o Imprimir Copia Legal</span>
        </button>
      </div>
    </div>
  );
}

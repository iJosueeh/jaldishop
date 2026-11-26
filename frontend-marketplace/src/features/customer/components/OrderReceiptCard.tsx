'use client';

import React, { useEffect, useState } from 'react';
import { Printer, Receipt } from 'lucide-react';

export interface CustomerFiscalReceipt {
  id: string;
  orderId: string;
  documentTypeCode: '01' | '03' | '00';
  documentTypeName: string;
  series: string;
  correlative: number;
  fullNumber: string;
  emitterTaxIdType: string;
  emitterTaxIdNumber?: string;
  emitterLegalName: string;
  emitterTaxRegime: string;
  customerDocType: string;
  customerDocNumber?: string;
  customerName: string;
  taxableAmount: number;
  igvRate: number;
  igvAmount: number;
  totalAmount: number;
  currency: string;
  paymentProvider?: string;
  providerPaymentId?: string;
  issuedAt: string;
}

interface OrderReceiptCardProps {
  orderId: string;
}

export const OrderReceiptCard: React.FC<OrderReceiptCardProps> = ({ orderId }) => {
  const [receipt, setReceipt] = useState<CustomerFiscalReceipt | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    async function loadReceipt() {
      try {
        const res = await fetch(`/api/customer/orders/${encodeURIComponent(orderId)}/receipt`, {
          cache: 'no-store',
        });
        if (!res.ok) {
          if (active) setReceipt(null);
          return;
        }
        const data = (await res.json()) as CustomerFiscalReceipt;
        if (active) setReceipt(data);
      } catch {
        if (active) setReceipt(null);
      } finally {
        if (active) setIsLoading(false);
      }
    }
    if (orderId) {
      void loadReceipt();
    }
    return () => {
      active = false;
    };
  }, [orderId]);

  const handlePrint = () => {
    if (!receipt || typeof window === 'undefined') return;
    const win = window.open('', '_blank', 'width=420,height=680');
    if (!win) return;

    const escapeHtml = (val: string | undefined | null) =>
      (val ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');

    const issuedDate = new Date(receipt.issuedAt).toLocaleString('es-PE');
    win.document.write(`<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(receipt.documentTypeName)} ${escapeHtml(receipt.fullNumber)}</title>
  <style>
    body { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; margin: 24px; color: #1c1917; font-size: 12px; }
    .center { text-align: center; }
    .title { font-size: 14px; font-weight: 700; margin-bottom: 4px; }
    .divider { border-top: 1px dashed #a8a29e; margin: 12px 0; }
    .row { display: flex; justify-content: space-between; margin: 4px 0; }
    .total { font-size: 14px; font-weight: 700; }
    @media print { body { margin: 8px; } }
  </style>
</head>
<body>
  <div class="center">
    <div class="title">${escapeHtml(receipt.emitterLegalName)}</div>
    <div>${escapeHtml(receipt.emitterTaxIdType)}: ${escapeHtml(receipt.emitterTaxIdNumber || 'En formalización')} · Régimen ${escapeHtml(receipt.emitterTaxRegime)}</div>
    <div class="divider"></div>
    <div class="title">${escapeHtml(receipt.documentTypeName.toUpperCase())} (Cód. ${escapeHtml(receipt.documentTypeCode)})</div>
    <div><strong>${escapeHtml(receipt.fullNumber)}</strong></div>
  </div>
  <div class="divider"></div>
  <div class="row"><span>Fecha emisión:</span><span>${escapeHtml(issuedDate)}</span></div>
  <div class="row"><span>Cliente:</span><span>${escapeHtml(receipt.customerName)}</span></div>
  <div class="row"><span>Doc. Cliente (${escapeHtml(receipt.customerDocType)}):</span><span>${escapeHtml(receipt.customerDocNumber || '—')}</span></div>
  <div class="divider"></div>
  <div class="row"><span>Op. Gravada / Base:</span><span>S/ ${Number(receipt.taxableAmount).toFixed(2)}</span></div>
  <div class="row"><span>IGV (${Number(receipt.igvRate).toFixed(0)}%):</span><span>S/ ${Number(receipt.igvAmount).toFixed(2)}</span></div>
  <div class="row total"><span>TOTAL:</span><span>S/ ${Number(receipt.totalAmount).toFixed(2)}</span></div>
  <div class="divider"></div>
  <div class="row"><span>Medio de pago:</span><span>${escapeHtml(receipt.paymentProvider || 'MERCADO_PAGO')}</span></div>
  ${receipt.providerPaymentId ? `<div class="row"><span>Ref. Pago:</span><span>${escapeHtml(receipt.providerPaymentId)}</span></div>` : ''}
  <script>window.onload = function() { window.print(); };</script>
</body>
</html>`);
    win.document.close();
  };

  if (isLoading || !receipt) {
    return null;
  }

  return (
    <section className="bg-white rounded-xl border border-[#e7e0d6] p-5 sm:p-6 space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e] flex items-center gap-2">
          <Receipt className="w-4 h-4 text-[#005141]" />
          <span>Comprobante SUNAT</span>
        </h2>
        <span className="px-2 py-0.5 rounded-full text-[11px] font-bold font-mono bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]">
          Cód. {receipt.documentTypeCode}
        </span>
      </div>

      <div className="space-y-1 text-xs text-[#57534e]">
        <p className="text-sm font-bold text-[#1c1917] font-mono">
          {receipt.documentTypeName} · {receipt.fullNumber}
        </p>
        <p>Emisor: {receipt.emitterLegalName}</p>
        <p className="font-mono tabular-nums">
          IGV ({Number(receipt.igvRate).toFixed(0)}%): S/ {Number(receipt.igvAmount).toFixed(2)} · Total: S/{' '}
          {Number(receipt.totalAmount).toFixed(2)}
        </p>
      </div>

      <button
        type="button"
        onClick={handlePrint}
        className="min-h-[40px] w-full inline-flex items-center justify-center gap-2 px-4 rounded-lg border border-[#e7e0d6] bg-[#faf7f2] hover:bg-[#f0eae0] text-xs font-semibold text-[#1c1917] transition-colors cursor-pointer"
      >
        <Printer className="w-4 h-4 text-[#005141]" />
        <span>Imprimir / Guardar PDF</span>
      </button>
    </section>
  );
};

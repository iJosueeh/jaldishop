import { Component, inject } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matReceiptLongOutline, matPrintOutline } from '@ng-icons/material-symbols/outline';
import { BillingService } from '../../../../core/services/billing.service';
import { FiscalReceiptResponse } from '../../../../core/models/billing.models';

@Component({
  selector: 'app-fiscal-receipts-table',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, NgIcon],
  providers: [
    provideIcons({
      matReceiptLongOutline,
      matPrintOutline,
    }),
  ],
  templateUrl: './fiscal-receipts-table.html',
})
export class FiscalReceiptsTable {
  readonly billingService = inject(BillingService);

  getBadgeClass(code: string): string {
    switch (code) {
      case '01':
        return 'bg-emerald-50 text-[#005141] border-emerald-200';
      case '03':
        return 'bg-sky-50 text-sky-800 border-sky-200';
      default:
        return 'bg-stone-100 text-stone-700 border-stone-200';
    }
  }

  printReceipt(receipt: FiscalReceiptResponse): void {
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
  }
}

import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matPrintOutline,
  matContentCopyOutline,
  matCheckOutline,
  matReceiptLongOutline,
} from '@ng-icons/material-symbols/outline';
import { ToastService } from '../../../core/services/toast.service';
import { StoreService } from '../../../core/services/store.service';
import { FiscalReceiptResponse } from '../../../core/models/billing.models';
import { environment } from '../../../../environments/environment';
import {
  generateAsciiTicket,
  generateBatchAsciiTickets,
  mapOrderToThermalData,
  printThermalText,
  ThermalTicketOrderData,
} from '../../../core/utils/thermal-ticket.utils';

@Component({
  selector: 'app-thermal-ticket-modal',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matPrintOutline,
      matContentCopyOutline,
      matCheckOutline,
      matReceiptLongOutline,
    }),
  ],
  templateUrl: './thermal-ticket-modal.html',
  styleUrl: './thermal-ticket-modal.css',
})
export class ThermalTicketModal {
  private readonly toastService = inject(ToastService);
  private readonly storeService = inject(StoreService, { optional: true });
  private readonly http = inject(HttpClient, { optional: true });

  readonly isOpen = input<boolean>(false);
  readonly order = input<any | null>(null);
  readonly orders = input<any[]>([]);
  readonly storeName = input<string>('MI COMERCIO');

  readonly closeModal = output<void>();

  readonly paperWidth = signal<80 | 58>(80);
  readonly copied = signal<boolean>(false);
  readonly activeReceipt = signal<FiscalReceiptResponse | null>(null);

  constructor() {
    effect(() => {
      const open = this.isOpen();
      const single = this.order();
      if (!open || !single?.id || !this.http) {
        this.activeReceipt.set(null);
        return;
      }
      this.http
        .get<FiscalReceiptResponse>(`${environment.apiUrl}/orders/${encodeURIComponent(single.id)}/receipt`)
        .subscribe({
          next: (receipt) => this.activeReceipt.set(receipt),
          error: () => this.activeReceipt.set(null),
        });
    });
  }

  private enrichWithFiscalData(data: ThermalTicketOrderData, receipt: FiscalReceiptResponse | null): ThermalTicketOrderData {
    const store = this.storeService?.currentStore();
    return {
      ...data,
      paymentMethod: receipt?.paymentProvider === 'MERCADO_PAGO' ? 'MERCADO PAGO' : data.paymentMethod,
      emitterLegalName: receipt?.emitterLegalName || store?.legalBusinessName || undefined,
      emitterTaxIdType: receipt?.emitterTaxIdType || store?.taxIdType,
      emitterTaxIdNumber: receipt?.emitterTaxIdNumber || store?.taxIdNumber,
      emitterTaxRegime: receipt?.emitterTaxRegime || store?.taxRegime,
      receiptTypeCode: receipt?.documentTypeCode,
      receiptTypeName: receipt?.documentTypeName,
      receiptFullNumber: receipt?.fullNumber,
      customerDocType: receipt?.customerDocType,
      customerDocNumber: receipt?.customerDocNumber,
      taxableAmount: receipt?.taxableAmount,
      igvRate: receipt?.igvRate ?? (store?.taxRegime === 'RUS' || store?.taxRegime === 'NONE' ? 0 : store?.taxRate),
      igvAmount: receipt?.igvAmount,
    };
  }

  readonly asciiContent = computed<string>(() => {
    const wMm = this.paperWidth();
    const cols = wMm === 80 ? 42 : 32;
    const sName = this.storeName() || 'MI COMERCIO';

    const single = this.order();
    if (single) {
      const data = this.enrichWithFiscalData(mapOrderToThermalData(single), this.activeReceipt());
      return generateAsciiTicket(data, sName, cols);
    }

    const batch = this.orders();
    if (batch && batch.length > 0) {
      const dataList = batch.map((o) => this.enrichWithFiscalData(mapOrderToThermalData(o), null));
      return generateBatchAsciiTickets(dataList, sName, cols);
    }

    return 'No hay información de pedido para generar la comanda.';
  });

  setPaperWidth(width: 80 | 58): void {
    this.paperWidth.set(width);
  }

  copyToClipboard(): void {
    const text = this.asciiContent();
    if (!text || text.startsWith('No hay')) return;

    if (
      typeof navigator !== 'undefined' &&
      navigator.clipboard &&
      typeof navigator.clipboard.writeText === 'function'
    ) {
      navigator.clipboard
        .writeText(text)
        .then(() => {
          this.copied.set(true);
          this.toastService.success('Comanda ASCII copiada al portapapeles.');
          setTimeout(() => this.copied.set(false), 2000);
        })
        .catch(() => {
          this.fallbackCopy(text);
        });
    } else {
      this.fallbackCopy(text);
    }
  }

  private fallbackCopy(text: string): void {
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      this.copied.set(true);
      this.toastService.success('Comanda ASCII copiada al portapapeles.');
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.toastService.info('Comanda generada en pantalla.');
    }
  }

  printTicket(): void {
    const text = this.asciiContent();
    if (!text || text.startsWith('No hay')) return;
    printThermalText(text, this.paperWidth());
    this.toastService.info('Enviando comanda a la impresora térmica...');
  }

  readonly isClosing = signal<boolean>(false);

  handleClose(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.onClose();
    }, 200);
  }

  onClose(): void {
    this.closeModal.emit();
  }
}

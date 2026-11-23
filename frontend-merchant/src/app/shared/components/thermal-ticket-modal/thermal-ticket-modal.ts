import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matPrintOutline,
  matContentCopyOutline,
  matCheckOutline,
  matReceiptLongOutline,
} from '@ng-icons/material-symbols/outline';
import { ToastService } from '../../../core/services/toast.service';
import {
  generateAsciiTicket,
  generateBatchAsciiTickets,
  mapOrderToThermalData,
  printThermalText,
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

  readonly isOpen = input<boolean>(false);
  readonly order = input<any | null>(null);
  readonly orders = input<any[]>([]);
  readonly storeName = input<string>('MI COMERCIO');

  readonly closeModal = output<void>();

  readonly paperWidth = signal<80 | 58>(80);
  readonly copied = signal<boolean>(false);

  readonly asciiContent = computed<string>(() => {
    const wMm = this.paperWidth();
    const cols = wMm === 80 ? 42 : 32;
    const sName = this.storeName() || 'MI COMERCIO';

    const single = this.order();
    if (single) {
      const data = mapOrderToThermalData(single);
      return generateAsciiTicket(data, sName, cols);
    }

    const batch = this.orders();
    if (batch && batch.length > 0) {
      const dataList = batch.map(mapOrderToThermalData);
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

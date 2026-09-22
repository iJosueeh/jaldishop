import { Component, computed, HostListener, inject, input, output, signal } from '@angular/core';
import { ToastService } from '../../../../core/services/toast.service';
import { DashboardPriorityOrder, OrderStatus } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matChatOutline,
  matScheduleOutline,
  matCheckCircleOutline,
  matDoneAllOutline,
  matSendOutline,
  matStorefrontOutline,
  matReceiptLongOutline,
  matLocationOnOutline,
  matPersonOutline,
  matContentCopyOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matChatOutline,
      matScheduleOutline,
      matCheckCircleOutline,
      matDoneAllOutline,
      matSendOutline,
      matStorefrontOutline,
      matReceiptLongOutline,
      matLocationOnOutline,
      matPersonOutline,
      matContentCopyOutline,
    }),
  ],
  selector: 'app-order-details-drawer',
  styleUrl: './order-details-drawer.css',
  templateUrl: './order-details-drawer.html',
})
export class OrderDetailsDrawer {
  private readonly toastService = inject(ToastService);

  readonly order = input<DashboardPriorityOrder | null>(null);
  readonly isOpen = input<boolean>(false);

  readonly closeDrawer = output<void>();
  readonly statusChange = output<{ order: DashboardPriorityOrder; newStatus: OrderStatus }>();
  readonly notifyCustomer = output<DashboardPriorityOrder>();

  readonly isClosing = signal<boolean>(false);

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event?: Event): void {
    if (this.isOpen() && !this.isClosing()) {
      event?.preventDefault();
      this.handleClose();
    }
  }

  handleClose(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.closeDrawer.emit();
      this.isClosing.set(false);
    }, 240);
  }

  readonly formattedTotal = computed(() => {
    const total = this.order()?.totalAmount || 0;
    return `S/ ${total.toFixed(2)}`;
  });

  readonly statusLabel = computed(() => {
    const status = this.order()?.status;
    switch (status) {
      case 'CONFIRMED':
        return 'CONFIRMADO';
      case 'IN_PREPARATION':
        return 'EN PREPARACIÓN';
      case 'READY':
        return 'LISTO';
      case 'OUT_FOR_DELIVERY':
        return 'EN CAMINO';
      case 'COMPLETED':
        return 'COMPLETADO';
      case 'CANCELLED':
        return 'CANCELADO';
      default:
        return status || '';
    }
  });

  onAdvanceStatus(): void {
    const currentOrder = this.order();
    if (!currentOrder) return;

    let nextStatus: OrderStatus = 'IN_PREPARATION';
    if (currentOrder.status === 'CONFIRMED') {
      nextStatus = 'IN_PREPARATION';
    } else if (currentOrder.status === 'IN_PREPARATION') {
      nextStatus = 'READY';
    } else if (currentOrder.status === 'READY') {
      nextStatus = 'COMPLETED';
    }

    this.statusChange.emit({ order: currentOrder, newStatus: nextStatus });
  }

  openWhatsApp(): void {
    const currentOrder = this.order();
    if (!currentOrder || !currentOrder.customerPhone) {
      this.toastService.info('No hay teléfono registrado para este pedido.');
      return;
    }

    const cleanPhone = currentOrder.customerPhone.replace(/\D/g, '');
    const message = encodeURIComponent(
      `¡Hola ${currentOrder.customerName}! Te escribimos de JaldiShop sobre tu pedido ${
        currentOrder.orderNumber
      } (${currentOrder.itemSummary}).`,
    );
    window.open(`https://wa.me/51${cleanPhone}?text=${message}`, '_blank');
  }

  copyOrderNumber(): void {
    const num = this.order()?.orderNumber;
    if (num && navigator?.clipboard) {
      navigator.clipboard.writeText(num);
      this.toastService.success(`Código ${num} copiado`);
    }
  }
}

import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardPriorityOrder, OrderStatus } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matArrowForwardOutline,
  matRestaurantOutline,
  matChatOutline,
  matScheduleOutline,
  matCheckCircleOutline,
  matDoneAllOutline,
  matSendOutline,
  matVisibilityOutline,
  matTaskAltOutline,
} from '@ng-icons/material-symbols/outline';
import { ToastService } from '../../../../core/services/toast.service';

import { OrderDetailsDrawer } from '../order-details-drawer/order-details-drawer';

@Component({
  imports: [RouterLink, NgIcon, OrderDetailsDrawer],
  providers: [
    provideIcons({
      matArrowForwardOutline,
      matRestaurantOutline,
      matChatOutline,
      matScheduleOutline,
      matCheckCircleOutline,
      matDoneAllOutline,
      matSendOutline,
      matVisibilityOutline,
      matTaskAltOutline,
    }),
  ],
  selector: 'app-priority-orders',
  styleUrl: './priority-orders.css',
  templateUrl: './priority-orders.html',
})
export class PriorityOrders {
  private readonly toastService = inject(ToastService);

  readonly selectedOrder = signal<DashboardPriorityOrder | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

  readonly orders = signal<DashboardPriorityOrder[]>([
    {
      id: 'ord-1',
      orderNumber: '#ORD-0842',
      customerName: 'María Fernanda Ruiz',
      customerPhone: '987654321',
      channel: 'WHATSAPP',
      channelLabel: 'WhatsApp',
      channelIcon: 'matChatOutline',
      icon: 'matRestaurantOutline',
      itemSummary: '1x Torta Selva Negra (Grande)',
      itemDetails: 'Dedicatoria: ¡Feliz Cumpleaños Mamá!',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. Dos de Mayo 1420, Dpto 501, San Isidro',
      deliveryReference: 'Frente al parque Olivar',
      scheduledTime: '10:00:00',
      deliveryTimeLabel: '10:00 - 11:30',
      isUrgent: true,
      status: 'IN_PREPARATION',
      totalAmount: 65.0,
      notes: 'Por favor incluir 6 velitas y tarjeta con dedicatoria.',
      items: [
        {
          name: 'Torta Selva Negra',
          variant: 'Presentación Grande (12 porciones)',
          quantity: 1,
          unitPrice: 65.0,
          totalPrice: 65.0,
        },
      ],
    },
  ]);

  openDrawer(order: DashboardPriorityOrder): void {
    this.selectedOrder.set(order);
    this.isDrawerOpen.set(true);
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedOrder.set(null);
  }

  onStatusChange(event: { order: DashboardPriorityOrder; newStatus: OrderStatus }): void {
    this.orders.update((list) =>
      list.map((o) => (o.id === event.order.id ? { ...o, status: event.newStatus } : o)),
    );

    if (this.selectedOrder()?.id === event.order.id) {
      this.selectedOrder.update((o) => (o ? { ...o, status: event.newStatus } : null));
    }

    const label =
      event.newStatus === 'IN_PREPARATION'
        ? 'en preparación'
        : event.newStatus === 'READY'
          ? 'listo para entrega'
          : 'completado';
    this.toastService.success(`Pedido ${event.order.orderNumber} marcado como ${label}.`);
  }

  onMarkAsReady(order: DashboardPriorityOrder, event?: Event): void {
    event?.stopPropagation();
    this.onStatusChange({ order, newStatus: 'READY' });
  }

  onNotifyCustomer(order: DashboardPriorityOrder, event?: Event): void {
    event?.stopPropagation();
    const cleanPhone = order.customerPhone?.replace(/\D/g, '') || '51987654321';
    const message = encodeURIComponent(
      `¡Hola ${order.customerName}! Tu pedido ${order.orderNumber} ya está LISTO para ser retirado / enviado.
¡Gracias por tu compra!`,
    );
    window.open(`https://wa.me/51${cleanPhone}?text=${message}`, '_blank');
    this.toastService.success(`Notificación enviada a ${order.customerName}.`);
  }
}

import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardPriorityOrder, OrderStatus } from '../../../../core/models/dashboard.models';
import { MerchantOrder } from '../../../../core/models/order.models';
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
import { OrderService } from '../../../../core/services/order.service';
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
export class PriorityOrders implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly selectedOrder = signal<DashboardPriorityOrder | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

  readonly orders = computed<DashboardPriorityOrder[]>(() => {
    return this.orderService
      .orders()
      .filter((o) => o.status !== 'COMPLETED' && o.status !== 'CANCELLED')
      .sort((a, b) => (b.isUrgent ? 1 : 0) - (a.isUrgent ? 1 : 0))
      .map((o) => this.mapToDashboardOrder(o));
  });

  ngOnInit(): void {
    if (this.orderService.orders().length === 0) {
      this.orderService.loadOrders().subscribe();
    }
  }

  openDrawer(order: DashboardPriorityOrder): void {
    this.selectedOrder.set(order);
    this.isDrawerOpen.set(true);
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedOrder.set(null);
  }

  onStatusChange(event: { order: DashboardPriorityOrder; newStatus: OrderStatus }): void {
    this.orderService.updateOrderStatus(event.order.id, event.newStatus);

    if (this.selectedOrder()?.id === event.order.id) {
      this.selectedOrder.update((o) => (o ? { ...o, status: event.newStatus } : null));
    }
  }

  onMarkAsReady(order: DashboardPriorityOrder, event?: Event): void {
    event?.stopPropagation();
    this.onStatusChange({ order, newStatus: 'READY' });
  }

  onNotifyCustomer(order: DashboardPriorityOrder, event?: Event): void {
    event?.stopPropagation();
    const rawOrder = this.orderService.orders().find((o) => o.id === order.id);
    if (rawOrder) {
      this.orderService.notifyViaWhatsApp(rawOrder);
    }
  }

  private mapToDashboardOrder(o: MerchantOrder): DashboardPriorityOrder {
    const itemSummary =
      o.items && o.items.length > 0
        ? o.items.map((i) => `${i.quantity}x ${i.name}`).join(', ')
        : 'Pedido sin detalle de ítems';

    const itemDetails =
      o.items && o.items[0]?.variant
        ? o.items[0].variant
        : o.notes
          ? `Nota: ${o.notes}`
          : '';

    return {
      id: o.id,
      orderNumber: o.orderNumber,
      customerName: o.customerName,
      customerPhone: o.customerPhone,
      channel: o.channel,
      channelLabel: o.channelLabel || 'WhatsApp',
      channelIcon: 'matChatOutline',
      icon: 'matRestaurantOutline',
      itemSummary,
      itemDetails,
      deliveryMode: o.deliveryMode,
      scheduledTime: o.scheduledTime || '10:00:00',
      deliveryTimeLabel: o.deliveryTimeLabel || '10:00 - 11:30',
      isUrgent: o.isUrgent,
      status: o.status,
      totalAmount: o.totalAmount,
      notes: o.notes,
      items: o.items,
      deliveryAddress: o.deliveryAddress,
      deliveryReference: o.deliveryReference,
    };
  }
}

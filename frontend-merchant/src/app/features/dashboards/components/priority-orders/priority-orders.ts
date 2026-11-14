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
import { ConfirmModal } from '../../../../shared/components/confirm-modal/confirm-modal';

@Component({
  imports: [RouterLink, NgIcon, OrderDetailsDrawer, ConfirmModal],
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
  readonly orderService = inject(OrderService);

  readonly isDrawerOpen = computed(() => this.orderService.isDrawerOpen());
  readonly selectedOrder = computed<DashboardPriorityOrder | null>(() => {
    const detail = this.orderService.selectedOrderDetail();
    if (detail) return detail;
    const selected = this.orderService.selectedOrder();
    return selected ? this.mapToDashboardOrder(selected) : null;
  });

  readonly isCancelModalOpen = signal<boolean>(false);
  readonly orderToCancel = signal<DashboardPriorityOrder | null>(null);

  readonly isDrawerUpdating = computed(() =>
    this.selectedOrder() ? this.orderService.isUpdating(this.selectedOrder()!.id) : false,
  );
  readonly isDrawerPendingVerification = computed(() =>
    this.selectedOrder()
      ? this.orderService.isPendingVerification(this.selectedOrder()!.id)
      : false,
  );

  isOrderUpdating(orderId: string): boolean {
    return this.orderService.isUpdating(orderId);
  }

  isOrderPendingVerification(orderId: string): boolean {
    return this.orderService.isPendingVerification(orderId);
  }

  onVerifyStatus(order: DashboardPriorityOrder): void {
    this.orderService.verifyOrderStatus(order.id).subscribe();
  }

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
    const rawOrder = this.orderService.orders().find((o) => o.id === order.id);
    if (rawOrder) {
      this.orderService.openDrawer(rawOrder);
    } else {
      const fallback: MerchantOrder = {
        id: order.id,
        orderNumber: order.orderNumber,
        customerName: order.customerName,
        customerPhone: order.customerPhone,
        channel: order.channel,
        channelLabel: order.channelLabel,
        deliveryMode: order.deliveryMode,
        deliveryAddress: order.deliveryAddress,
        deliveryReference: order.deliveryReference,
        scheduledTime: order.scheduledTime,
        deliveryTimeLabel: order.deliveryTimeLabel,
        isUrgent: order.isUrgent,
        status: order.status,
        totalAmount: order.totalAmount ?? 0,
        notes: order.notes,
        items: order.items || [],
        createdAt: new Date().toISOString(),
      };
      this.orderService.openDrawer(fallback);
    }
  }

  closeDrawer(): void {
    this.orderService.closeDrawer();
  }

  onCancelOrder(order: DashboardPriorityOrder): void {
    if (order.status !== 'CONFIRMED' && order.status !== 'IN_PREPARATION') {
      return;
    }
    if (this.orderService.isUpdating(order.id) || this.orderService.isPendingVerification(order.id)) {
      return;
    }
    this.orderToCancel.set(order);
    this.isCancelModalOpen.set(true);
  }

  readonly cancelModalMessage = computed(() => {
    const order = this.orderToCancel();
    if (!order) return '';
    if (order.status === 'CONFIRMED') {
      return `¿Estás seguro de cancelar el pedido ${order.orderNumber}? Se liberará el cupo de capacidad y se restituirá el inventario de los productos según RN-ORD-13. Si contaba con pago aprobado, se registrará la solicitud de reembolso en estado PENDING (sujeto a procesamiento, no inmediato).`;
    }
    if (order.status === 'IN_PREPARATION') {
      return `¿Estás seguro de cancelar el pedido ${order.orderNumber}? Al encontrarse en preparación, NO se restituirá la capacidad del turno ni el inventario consumido en cocina (RN-ORD-13). Si contaba con pago aprobado, se registrará la solicitud de reembolso en estado PENDING (sujeto a procesamiento, no inmediato).`;
    }
    return `¿Estás seguro de cancelar el pedido ${order.orderNumber}? Si contaba con pago aprobado, se registrará la solicitud de reembolso en estado PENDING (sujeto a procesamiento, no inmediato).`;
  });

  onConfirmCancel(): void {
    const order = this.orderToCancel();
    this.isCancelModalOpen.set(false);
    this.orderToCancel.set(null);
    if (
      order &&
      (order.status === 'CONFIRMED' || order.status === 'IN_PREPARATION') &&
      !this.orderService.isUpdating(order.id) &&
      !this.orderService.isPendingVerification(order.id)
    ) {
      this.orderService.updateOrderStatus(order.id, 'CANCELLED').subscribe();
    }
  }

  onDismissCancel(): void {
    this.isCancelModalOpen.set(false);
    this.orderToCancel.set(null);
  }

  onStatusChange(event: { order: DashboardPriorityOrder; newStatus: OrderStatus }): void {
    this.orderService.updateOrderStatus(event.order.id, event.newStatus).subscribe();
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
      channelLabel: o.channelLabel,
      channelIcon: o.channelLabel ? 'matChatOutline' : undefined,
      icon: 'matRestaurantOutline',
      itemSummary,
      itemDetails,
      deliveryMode: o.deliveryMode,
      scheduledTime: o.scheduledTime || '00:00',
      deliveryTimeLabel: o.deliveryTimeLabel,
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

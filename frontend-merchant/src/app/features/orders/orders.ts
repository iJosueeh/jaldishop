import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { OrderService } from '../../core/services/order.service';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';
import { MerchantOrder, OrderFilterTab, OrderStatus } from '../../core/models/order.models';
import { OrdersHeader } from './components/orders-header/orders-header';
import { OrdersAlertBanner } from './components/orders-alert-banner/orders-alert-banner';
import { OrdersKpis } from './components/orders-kpis/orders-kpis';
import { OrdersFilterBar } from './components/orders-filter-bar/orders-filter-bar';
import { OrdersGrid } from './components/orders-grid/orders-grid';
import { Pagination } from '../../shared/components/pagination/pagination';
import { OrderDetailsDrawer } from '../dashboards/components/order-details-drawer/order-details-drawer';

@Component({
  imports: [
    OrdersHeader,
    OrdersAlertBanner,
    OrdersKpis,
    OrdersFilterBar,
    OrdersGrid,
    Pagination,
    OrderDetailsDrawer,
  ],
  selector: 'app-orders',
  styleUrl: './orders.css',
  templateUrl: './orders.html',
})
export class Orders implements OnInit {
  readonly orderService = inject(OrderService);
  readonly capacityService = inject(CapacityService);
  private readonly toastService = inject(ToastService);

  readonly orders = computed(() => this.orderService.filteredOrders());
  readonly activeTab = computed(() => this.orderService.activeTab());
  readonly urgentOrder = computed(() => this.orderService.urgentOrder());

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  readonly paginatedOrders = computed(() => {
    const all = this.orders();
    const start = (this.currentPage() - 1) * this.pageSize();
    return all.slice(start, start + this.pageSize());
  });

  // Adaptador para el Drawer existente
  readonly drawerOrder = computed(() => {
    const selected = this.orderService.selectedOrder();
    if (!selected) return null;
    return {
      id: selected.id,
      orderNumber: selected.orderNumber,
      customerName: selected.customerName,
      customerPhone: selected.customerPhone,
      channel: selected.channel,
      channelLabel: selected.channelLabel,
      channelIcon: 'matChatOutline',
      icon: 'matRestaurantOutline',
      itemSummary: selected.items.map((i) => `${i.quantity}x ${i.name}`).join(', '),
      deliveryMode: selected.deliveryMode,
      deliveryAddress: selected.deliveryAddress,
      deliveryReference: selected.deliveryReference,
      scheduledTime: selected.scheduledTime || '16:00:00',
      deliveryTimeLabel: selected.deliveryTimeLabel,
      isUrgent: selected.isUrgent,
      status: selected.status,
      totalAmount: selected.totalAmount,
      notes: selected.notes,
      items: selected.items.map((i) => ({
        name: i.name,
        variant: i.variant,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        totalPrice: i.totalPrice,
      })),
    };
  });

  ngOnInit(): void {
    this.orderService.loadOrders().subscribe();
  }

  onTabChange(tab: OrderFilterTab): void {
    this.orderService.setActiveTab(tab);
    this.currentPage.set(1);
  }

  onSearchChange(query: string): void {
    this.orderService.setSearchQuery(query);
    this.currentPage.set(1);
  }

  onChannelChange(channel: string): void {
    this.orderService.setChannelFilter(channel);
    this.currentPage.set(1);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onStatusAdvance(order: MerchantOrder): void {
    const nextStatusMap: Partial<Record<OrderStatus, OrderStatus>> = {
      CONFIRMED: 'IN_PREPARATION',
      IN_PREPARATION: 'READY',
      READY: 'COMPLETED',
    };

    const next = nextStatusMap[order.status];
    if (next) {
      this.orderService.updateOrderStatus(order.id, next);
    }
  }

  onWhatsAppClick(order: MerchantOrder): void {
    this.orderService.notifyViaWhatsApp(order);
  }

  onOpenDetails(order: MerchantOrder): void {
    this.orderService.openDrawer(order);
  }

  onCloseDrawer(): void {
    this.orderService.closeDrawer();
  }

  onDrawerStatusChange(event: { order: any; newStatus: OrderStatus }): void {
    this.orderService.updateOrderStatus(event.order.id, event.newStatus);
  }

  onNewOrder(): void {
    this.orderService.openCreateModal();
  }

  onAdjustPace(): void {
    this.toastService.info('Ajuste de ritmo de cocina disponible en Próximas Mejoras.');
  }

  onPrintOrders(): void {
    window.print();
  }
}

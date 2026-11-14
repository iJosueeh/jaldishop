import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { OrderService } from '../../core/services/order.service';
import { CapacityService } from '../../core/services/capacity.service';
import { StoreService } from '../../core/services/store.service';
import { StompService } from '../../core/services/stomp.service';
import { ToastService } from '../../core/services/toast.service';
import { MerchantOrder, OrderFilterTab, OrderStatus } from '../../core/models/order.models';
import { OrdersHeader } from './components/orders-header/orders-header';
import { OrdersAlertBanner } from './components/orders-alert-banner/orders-alert-banner';
import { OrdersKpis } from './components/orders-kpis/orders-kpis';
import { OrdersFilterBar } from './components/orders-filter-bar/orders-filter-bar';
import { OrdersGrid } from './components/orders-grid/orders-grid';
import { Pagination } from '../../shared/components/pagination/pagination';
import { OrderDetailsDrawer } from '../dashboards/components/order-details-drawer/order-details-drawer';
import { ConfirmModal } from '../../shared/components/confirm-modal/confirm-modal';
import { DashboardPriorityOrder } from '../../core/models/dashboard.models';

@Component({
  imports: [
    OrdersHeader,
    OrdersAlertBanner,
    OrdersKpis,
    OrdersFilterBar,
    OrdersGrid,
    Pagination,
    OrderDetailsDrawer,
    ConfirmModal,
  ],
  selector: 'app-orders',
  styleUrl: './orders.css',
  templateUrl: './orders.html',
})
export class Orders implements OnInit {
  readonly orderService = inject(OrderService);
  readonly capacityService = inject(CapacityService);
  readonly storeService = inject(StoreService);
  readonly stompService = inject(StompService);
  private readonly toastService = inject(ToastService);

  readonly isCancelModalOpen = signal<boolean>(false);
  readonly orderToCancel = signal<MerchantOrder | DashboardPriorityOrder | null>(null);

  readonly orders = computed(() => this.orderService.filteredOrders());
  readonly activeTab = computed(() => this.orderService.activeTab());
  readonly urgentOrder = computed(() => this.orderService.urgentOrder());

  readonly currentPageUI = computed(() => this.orderService.currentPage() + 1);
  readonly currentPage = computed(() => this.currentPageUI());
  readonly pageSize = computed(() => this.orderService.pageSize());

  // Eliminada segunda paginación mediante slice(): el backend provee la página exacta
  readonly paginatedOrders = computed(() => this.orders());

  // Adaptador para el Drawer existente (soporta detalle cargado o fallback del resumen)
  readonly drawerOrder = computed(() => {
    const detail = this.orderService.selectedOrderDetail();
    if (detail) return detail;

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
      scheduledTime: selected.scheduledTime || '00:00',
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
    if (this.capacityService.configurations().length === 0) {
      this.capacityService.getConfigurations().subscribe();
    }
    if (this.capacityService.exceptions().length === 0) {
      this.capacityService.getExceptions().subscribe();
    }

    const store = this.storeService.currentStore();
    if (store?.id) {
      this.stompService.connect(store.id);
      this.orderService
        .loadOrders(0, this.pageSize(), this.activeTab(), store.id)
        .subscribe();
    } else {
      this.storeService.getMyStore().subscribe({
        next: (s) => {
          if (s?.id) {
            this.stompService.connect(s.id);
            this.orderService
              .loadOrders(0, this.pageSize(), this.activeTab(), s.id)
              .subscribe();
          }
        },
      });
    }
  }

  onCancelOrder(order: MerchantOrder | DashboardPriorityOrder): void {
    if (order.status !== 'CONFIRMED' && order.status !== 'IN_PREPARATION') {
      this.toastService.error(`No se puede cancelar un pedido en estado ${order.status}`);
      return;
    }
    this.orderToCancel.set(order);
    this.isCancelModalOpen.set(true);
  }

  onConfirmCancel(): void {
    const order = this.orderToCancel();
    this.isCancelModalOpen.set(false);
    this.orderToCancel.set(null);
    if (order) {
      this.orderService.updateOrderStatus(order.id, 'CANCELLED').subscribe();
    }
  }

  onDismissCancel(): void {
    this.isCancelModalOpen.set(false);
    this.orderToCancel.set(null);
  }

  onVerifyStatus(order: { id: string }): void {
    this.orderService.verifyOrderStatus(order.id).subscribe();
  }

  onTabChange(tab: OrderFilterTab): void {
    this.orderService.setActiveTab(tab);
  }

  onSearchChange(query: string): void {
    this.orderService.setSearchQuery(query);
  }

  onChannelChange(channel: string): void {
    this.orderService.setChannelFilter(channel);
  }

  onPageChange(page: number): void {
    this.orderService.setPage(page - 1);
  }

  onStatusAdvance(order: MerchantOrder): void {
    let nextStatus: OrderStatus | null = null;
    if (order.status === 'CONFIRMED') {
      nextStatus = 'IN_PREPARATION';
    } else if (order.status === 'IN_PREPARATION') {
      nextStatus = 'READY';
    } else if (order.status === 'READY') {
      nextStatus = order.deliveryMode === 'DELIVERY' ? 'OUT_FOR_DELIVERY' : 'COMPLETED';
    } else if (order.status === 'OUT_FOR_DELIVERY') {
      nextStatus = 'COMPLETED';
    }

    if (nextStatus) {
      this.orderService.updateOrderStatus(order.id, nextStatus).subscribe();
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
    this.orderService.updateOrderStatus(event.order.id, event.newStatus).subscribe();
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

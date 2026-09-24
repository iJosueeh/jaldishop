import { computed, inject, Injectable, signal } from '@angular/core';
import { MerchantOrder, OrderFilterTab, OrderStatus } from '../models/order.models';
import { ToastService } from './toast.service';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly toastService = inject(ToastService);

  readonly orders = signal<MerchantOrder[]>([]);
  readonly isLoading = signal<boolean>(false);
  private readonly isLoaded = signal<boolean>(false);

  readonly activeTab = signal<OrderFilterTab>('ALL');
  readonly searchQuery = signal<string>('');
  readonly channelFilter = signal<string>('ALL');
  readonly selectedOrder = signal<MerchantOrder | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);
  readonly isCreateModalOpen = signal<boolean>(false);

  // KPIs
  readonly totalOrdersCount = computed(() => this.orders().length);
  readonly confirmedCount = computed(
    () => this.orders().filter((o) => o.status === 'CONFIRMED').length,
  );
  readonly inPrepCount = computed(
    () => this.orders().filter((o) => o.status === 'IN_PREPARATION').length,
  );
  readonly readyCount = computed(
    () => this.orders().filter((o) => o.status === 'READY').length,
  );
  readonly completedCount = computed(
    () => this.orders().filter((o) => o.status === 'COMPLETED').length,
  );

  readonly remainingDeliveriesCount = computed(() => {
    return this.inPrepCount() + this.readyCount();
  });

  readonly urgentOrder = computed<MerchantOrder | null>(() => {
    return this.orders().find((o) => o.isUrgent && o.status !== 'COMPLETED') || null;
  });

  // Lista Filtrada Reactiva
  readonly filteredOrders = computed(() => {
    const tab = this.activeTab();
    const query = this.searchQuery().toLowerCase().trim();
    const channel = this.channelFilter();

    return this.orders().filter((order) => {
      // 1. Filtro por estado / pestaña
      if (tab === 'CONFIRMED' && order.status !== 'CONFIRMED') return false;
      if (tab === 'IN_PREPARATION' && order.status !== 'IN_PREPARATION') return false;
      if (tab === 'READY' && order.status !== 'READY') return false;
      if (tab === 'COMPLETED' && order.status !== 'COMPLETED') return false;

      // 2. Filtro por canal
      if (channel !== 'ALL' && order.channel !== channel) return false;

      // 3. Filtro por búsqueda de texto
      if (query) {
        const matchNumber = order.orderNumber.toLowerCase().includes(query);
        const matchCustomer = order.customerName.toLowerCase().includes(query);
        const matchPhone = order.customerPhone?.includes(query);
        const matchItems = order.items.some((i) => i.name.toLowerCase().includes(query));
        if (!matchNumber && !matchCustomer && !matchPhone && !matchItems) {
          return false;
        }
      }

      return true;
    });
  });

  readonly isEmptyOrders = computed(() => {
    return !this.isLoading() && this.isLoaded() && this.orders().length === 0;
  });

  readonly isFilterEmpty = computed(() => {
    return !this.isLoading() && this.isLoaded() && this.orders().length > 0 && this.filteredOrders().length === 0;
  });

  loadOrders(forceRefresh = false): Observable<MerchantOrder[]> {
    if (this.isLoaded() && !forceRefresh) {
      return of(this.orders());
    }

    this.isLoading.set(true);
    // Simula inicialización Cache First de pedidos
    this.isLoaded.set(true);
    this.isLoading.set(false);
    return of(this.orders());
  }

  setActiveTab(tab: OrderFilterTab): void {
    this.activeTab.set(tab);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setChannelFilter(channel: string): void {
    this.channelFilter.set(channel);
  }

  clearFilters(): void {
    this.activeTab.set('ALL');
    this.searchQuery.set('');
    this.channelFilter.set('ALL');
  }

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  addOrder(order: MerchantOrder): void {
    this.orders.update((list) => [order, ...list]);
    this.isLoaded.set(true);
    this.toastService.success(`Pedido ${order.orderNumber} registrado exitosamente.`);
  }

  openDrawer(order: MerchantOrder): void {
    this.selectedOrder.set(order);
    this.isDrawerOpen.set(true);
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedOrder.set(null);
  }

  updateOrderStatus(orderId: string, newStatus: OrderStatus): void {
    this.orders.update((list) =>
      list.map((o) => (o.id === orderId ? { ...o, status: newStatus } : o)),
    );

    if (this.selectedOrder()?.id === orderId) {
      this.selectedOrder.update((o) => (o ? { ...o, status: newStatus } : null));
    }

    const labelMap: Record<OrderStatus, string> = {
      CONFIRMED: 'confirmado',
      IN_PREPARATION: 'en preparación',
      READY: 'listo para entrega',
      OUT_FOR_DELIVERY: 'en camino / despacho',
      COMPLETED: 'completado / entregado',
      CANCELLED: 'cancelado',
    };

    this.toastService.success(`Pedido actualizado a ${labelMap[newStatus]}.`);
  }

  notifyViaWhatsApp(order: MerchantOrder): void {
    if (!order.customerPhone) {
      this.toastService.info('No hay teléfono registrado para este cliente.');
      return;
    }
    const cleanPhone = order.customerPhone.replace(/\D/g, '');
    const message = encodeURIComponent(
      `¡Hola ${order.customerName}! Te escribimos de JaldiShop. Tu pedido ${order.orderNumber} (${order.items.map((i) => i.name).join(', ')}) está ${
        order.status === 'READY'
          ? 'LISTO para entrega / despacho'
          : order.status === 'IN_PREPARATION'
            ? 'en preparación en cocina'
            : 'confirmado'
      }. ¡Gracias por tu compra!`,
    );
    window.open(`https://wa.me/51${cleanPhone}?text=${message}`, '_blank');
    this.toastService.success(`WhatsApp abierto para ${order.customerName}.`);
  }
}

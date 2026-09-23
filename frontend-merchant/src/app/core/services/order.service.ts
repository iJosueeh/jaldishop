import { computed, inject, Service, signal } from '@angular/core';
import { MerchantOrder, OrderFilterTab, OrderStatus } from '../models/order.models';
import { ToastService } from './toast.service';

@Service()
export class OrderService {
  private readonly toastService = inject(ToastService);

  readonly orders = signal<MerchantOrder[]>([
    {
      id: 'ord-1039',
      orderNumber: '#PED-1039',
      customerName: 'Valeria Ramos',
      customerPhone: '984552109',
      channel: 'WHATSAPP',
      channelLabel: 'WhatsApp',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. José Pardo 450, Dpto 802, Miraflores',
      deliveryReference: 'Frente a Vivanda',
      deliveryTimeLabel: 'Entrega en 15 min (16:15)',
      isUrgent: true,
      urgentLabel: 'Entrega en 15 min (16:15)',
      status: 'IN_PREPARATION',
      paymentMethod: 'YAPE',
      paymentLabel: 'Yape',
      totalAmount: 58.0,
      notes: 'Por favor no tocar timbre, bebé durmiendo.',
      createdAt: new Date().toISOString(),
      items: [
        {
          name: 'Caja Brownies Melcochosos x6',
          variant: 'Caja Regalo',
          quantity: 1,
          unitPrice: 36.0,
          totalPrice: 36.0,
        },
        {
          name: 'Galletas de Avena con Chispas x4',
          quantity: 1,
          unitPrice: 22.0,
          totalPrice: 22.0,
        },
      ],
    },
    {
      id: 'ord-1040',
      orderNumber: '#PED-1040',
      customerName: 'Mariana Torres',
      customerPhone: '984123456',
      channel: 'COUNTER',
      channelLabel: 'Mostrador / Tienda',
      deliveryMode: 'PICKUP',
      deliveryAddress: 'Recojo en taller · San Isidro',
      deliveryReference: 'Caja con visor preparada',
      deliveryTimeLabel: 'Retiro: 16:30 (En 30 min)',
      isUrgent: false,
      urgentLabel: 'Retiro: 16:30 (En 30 min)',
      status: 'READY',
      paymentMethod: 'PLIN',
      paymentLabel: 'Plin',
      totalAmount: 84.0,
      notes: 'Incluir tarjeta de dedicatoria.',
      createdAt: new Date().toISOString(),
      items: [
        {
          name: 'Torta de Chocolate Mediana (12 porc.)',
          variant: '12 Porciones',
          quantity: 1,
          unitPrice: 78.0,
          totalPrice: 78.0,
        },
        {
          name: 'Velita dorada con chispas especiales',
          quantity: 1,
          unitPrice: 6.0,
          totalPrice: 6.0,
        },
      ],
    },
    {
      id: 'ord-1041',
      orderNumber: '#PED-1041',
      customerName: 'Carlos Benavides',
      customerPhone: '992345678',
      channel: 'INSTAGRAM',
      channelLabel: 'Instagram Direct',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Calle Los Robles 112, Surco',
      deliveryReference: 'Surco (Cerca a El Polo)',
      deliveryTimeLabel: 'Para las 17:00 (En 50 min)',
      isUrgent: false,
      urgentLabel: 'Para las 17:00 (En 50 min)',
      status: 'IN_PREPARATION',
      paymentMethod: 'BCP',
      paymentLabel: 'BCP',
      totalAmount: 65.0,
      createdAt: new Date().toISOString(),
      items: [
        {
          name: 'Pie de Limón Artesanal Mediano',
          quantity: 1,
          unitPrice: 45.0,
          totalPrice: 45.0,
        },
        {
          name: 'Porción Cheesecake Frutos Rojos',
          quantity: 1,
          unitPrice: 20.0,
          totalPrice: 20.0,
        },
      ],
    },
    {
      id: 'ord-1042',
      orderNumber: '#PED-1042',
      customerName: 'Lucía Morales',
      customerPhone: '991802443',
      channel: 'WHATSAPP',
      channelLabel: 'WhatsApp',
      deliveryMode: 'PICKUP',
      deliveryAddress: 'Recojo en taller · San Isidro',
      deliveryTimeLabel: 'Recojo estimado: 18:00',
      isUrgent: false,
      urgentLabel: 'Recibido hace 6 min',
      status: 'CONFIRMED',
      paymentMethod: 'YAPE',
      paymentLabel: 'Yape · Voucher adjunto',
      totalAmount: 48.0,
      createdAt: new Date().toISOString(),
      items: [
        {
          name: 'Alfajores Clásicos de Maicena x6',
          quantity: 2,
          unitPrice: 13.0,
          totalPrice: 26.0,
        },
        {
          name: 'Queque de Zanahoria con Nuez',
          quantity: 1,
          unitPrice: 22.0,
          totalPrice: 22.0,
        },
      ],
    },
  ]);

  readonly activeTab = signal<OrderFilterTab>('ALL');
  readonly searchQuery = signal<string>('');
  readonly channelFilter = signal<string>('ALL');
  readonly selectedOrder = signal<MerchantOrder | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

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

  setActiveTab(tab: OrderFilterTab): void {
    this.activeTab.set(tab);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setChannelFilter(channel: string): void {
    this.channelFilter.set(channel);
  }

  readonly isCreateModalOpen = signal<boolean>(false);

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  addOrder(order: MerchantOrder): void {
    this.orders.update((list) => [order, ...list]);
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

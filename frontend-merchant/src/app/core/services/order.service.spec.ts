import { TestBed } from '@angular/core/testing';
import { OrderService } from './order.service';
import { ToastService } from './toast.service';
import { MerchantOrder } from '../models/order.models';

describe('OrderService', () => {
  let service: OrderService;
  let toastMock: any;

  const mockOrders: MerchantOrder[] = [
    {
      id: 'ord-1039',
      orderNumber: '#PED-1039',
      customerName: 'Valeria Ramos',
      customerPhone: '984552109',
      channel: 'WHATSAPP',
      channelLabel: 'WhatsApp',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. José Pardo 450',
      deliveryTimeLabel: 'En 15 min',
      isUrgent: false,
      status: 'IN_PREPARATION',
      paymentMethod: 'YAPE',
      totalAmount: 58.0,
      createdAt: new Date().toISOString(),
      items: [{ name: 'Brownies', quantity: 1, unitPrice: 36, totalPrice: 36 }],
    },
    {
      id: 'ord-1040',
      orderNumber: '#PED-1040',
      customerName: 'Mariana Torres',
      customerPhone: '984123456',
      channel: 'COUNTER',
      channelLabel: 'Mostrador',
      deliveryMode: 'PICKUP',
      deliveryTimeLabel: 'En 30 min',
      isUrgent: false,
      status: 'READY',
      paymentMethod: 'PLIN',
      totalAmount: 84.0,
      createdAt: new Date().toISOString(),
      items: [{ name: 'Torta de Chocolate', quantity: 1, unitPrice: 78, totalPrice: 78 }],
    },
  ];

  beforeEach(() => {
    toastMock = {
      success: vi.fn(),
      info: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [OrderService, { provide: ToastService, useValue: toastMock }],
    });
    service = TestBed.inject(OrderService);
  });

  it('debe crearse correctamente e inicializar vacío sin datos quemados', () => {
    expect(service).toBeTruthy();
    expect(service.orders().length).toBe(0);
    expect(service.isEmptyOrders()).toBe(false);

    service.loadOrders().subscribe((orders) => {
      expect(orders.length).toBe(0);
      expect(service.isEmptyOrders()).toBe(true);
    });
  });

  it('debe filtrar pedidos por pestaña de estado', () => {
    service.orders.set(mockOrders);

    service.setActiveTab('IN_PREPARATION');
    expect(service.activeTab()).toBe('IN_PREPARATION');
    const inPrep = service.filteredOrders();
    expect(inPrep.every((o) => o.status === 'IN_PREPARATION')).toBe(true);

    service.setActiveTab('READY');
    const ready = service.filteredOrders();
    expect(ready.every((o) => o.status === 'READY')).toBe(true);
  });

  it('debe filtrar pedidos por búsqueda de texto', () => {
    service.orders.set(mockOrders);

    service.setSearchQuery('Valeria');
    const filtered = service.filteredOrders();
    expect(filtered.length).toBe(1);
    expect(filtered[0].customerName).toBe('Valeria Ramos');
  });

  it('debe actualizar el estado del pedido y mostrar toast', () => {
    service.orders.set(mockOrders);
    const firstOrder = mockOrders[0];

    service.updateOrderStatus(firstOrder.id, 'READY');

    const updated = service.orders().find((o) => o.id === firstOrder.id);
    expect(updated?.status).toBe('READY');
    expect(toastMock.success).toHaveBeenCalledWith('Pedido actualizado a listo para entrega.');
  });

  it('debe abrir y cerrar drawer de detalle', () => {
    const firstOrder = mockOrders[0];
    service.openDrawer(firstOrder);
    expect(service.isDrawerOpen()).toBe(true);
    expect(service.selectedOrder()).toEqual(firstOrder);

    service.closeDrawer();
    expect(service.isDrawerOpen()).toBe(false);
    expect(service.selectedOrder()).toBeNull();
  });

  it('debe abrir, cerrar modal de creación y agregar nuevo pedido al inicio', () => {
    service.openCreateModal();
    expect(service.isCreateModalOpen()).toBe(true);

    service.closeCreateModal();
    expect(service.isCreateModalOpen()).toBe(false);

    const newOrder: MerchantOrder = {
      ...mockOrders[0],
      id: 'new-ord-999',
      orderNumber: '#PED-9999',
    };

    service.addOrder(newOrder);
    expect(service.orders().length).toBe(1);
    expect(service.orders()[0].id).toBe('new-ord-999');
    expect(toastMock.success).toHaveBeenCalledWith('Pedido #PED-9999 registrado exitosamente.');
  });
});

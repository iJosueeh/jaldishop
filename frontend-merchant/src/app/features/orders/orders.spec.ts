import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Orders } from './orders';
import { OrderService } from '../../core/services/order.service';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';
import { MerchantOrder } from '../../core/models/order.models';

describe('Orders', () => {
  let component: Orders;
  let fixture: ComponentFixture<Orders>;
  let orderService: OrderService;

  const mockOrders: MerchantOrder[] = [
    {
      id: 'ord-1',
      orderNumber: '#PED-1001',
      customerName: 'Valeria Ramos',
      customerPhone: '984552109',
      channel: 'WHATSAPP',
      channelLabel: 'WhatsApp',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. José Pardo 450',
      deliveryTimeLabel: 'En 15 min',
      isUrgent: false,
      status: 'CONFIRMED',
      paymentMethod: 'YAPE',
      totalAmount: 58.0,
      createdAt: new Date().toISOString(),
      items: [{ name: 'Brownies', quantity: 1, unitPrice: 36, totalPrice: 36 }],
    },
    {
      id: 'ord-2',
      orderNumber: '#PED-1002',
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Orders],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CapacityService,
        OrderService,
        ToastService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Orders);
    component = fixture.componentInstance;
    orderService = component.orderService;
    orderService.orders.set(mockOrders);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and load initial orders', () => {
    expect(component).toBeTruthy();
    expect(component.orders().length).toBe(2);
  });

  it('debe cambiar de pestaña de filtrado', () => {
    component.onTabChange('READY');
    expect(component.activeTab()).toBe('READY');
    expect(component.orders().every((o) => o.status === 'READY')).toBe(true);
  });

  it('debe avanzar el estado de una comanda (CONFIRMED -> IN_PREPARATION -> READY -> COMPLETED)', () => {
    const order = component.orders().find((o) => o.status === 'CONFIRMED')!;
    expect(order).toBeDefined();

    component.onStatusAdvance(order);
    const updated = orderService.orders().find((o) => o.id === order.id);
    expect(updated?.status).toBe('IN_PREPARATION');

    component.onStatusAdvance(updated!);
    const updated2 = orderService.orders().find((o) => o.id === order.id);
    expect(updated2?.status).toBe('READY');
  });

  it('debe abrir y cerrar drawer de detalle desde Orders', () => {
    const order = component.orders()[0];
    component.onOpenDetails(order);

    expect(orderService.isDrawerOpen()).toBe(true);
    expect(component.drawerOrder()?.id).toBe(order.id);

    component.onCloseDrawer();
    expect(orderService.isDrawerOpen()).toBe(false);
  });

  it('debe manejar cambios de página y paginación reactiva', () => {
    expect(component.currentPage()).toBe(1);
    component.onPageChange(2);
    expect(component.currentPage()).toBe(2);

    component.onTabChange('CONFIRMED');
    expect(component.currentPage()).toBe(1);
  });

  it('debe abrir modal de nuevo pedido', () => {
    component.onNewOrder();
    expect(orderService.isCreateModalOpen()).toBe(true);
  });
});

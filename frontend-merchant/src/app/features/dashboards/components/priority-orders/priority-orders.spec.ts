import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { PriorityOrders } from './priority-orders';
import { OrderService } from '../../../../core/services/order.service';
import { MerchantOrder } from '../../../../core/models/order.models';

describe('PriorityOrders', () => {
  let component: PriorityOrders;
  let fixture: ComponentFixture<PriorityOrders>;
  let orderService: OrderService;

  const mockOrder: MerchantOrder = {
    id: 'ord-test-1',
    orderNumber: '#ORD-0001',
    customerName: 'María García',
    customerPhone: '987654321',
    channel: 'WHATSAPP',
    channelLabel: 'WhatsApp',
    deliveryMode: 'DELIVERY',
    deliveryAddress: 'Av. Larco 123',
    deliveryTimeLabel: '10:00 - 11:30',
    isUrgent: true,
    status: 'IN_PREPARATION',
    paymentMethod: 'YAPE',
    totalAmount: 45.0,
    items: [
      {
        name: 'Torta de Chocolate',
        variant: 'Porción personal',
        quantity: 1,
        unitPrice: 45.0,
        totalPrice: 45.0,
      },
    ],
    createdAt: '2026-09-25T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriorityOrders],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        OrderService,
      ],
    }).compileComponents();

    orderService = TestBed.inject(OrderService);
    orderService.orders.set([mockOrder]);

    fixture = TestBed.createComponent(PriorityOrders);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe listar pedidos prioritarios activos', () => {
    expect(component.orders().length).toBe(1);
    expect(component.orders()[0].id).toBe('ord-test-1');
  });

  it('debe abrir y cerrar el drawer correctamente', () => {
    const order = component.orders()[0];
    expect(order).toBeDefined();

    component.openDrawer(order);
    expect(component.isDrawerOpen()).toBe(true);
    expect(component.selectedOrder()?.id).toBe(order.id);

    component.closeDrawer();
    expect(component.isDrawerOpen()).toBe(false);
    expect(component.selectedOrder()).toBeNull();
  });

  it('debe actualizar el estado de un pedido', () => {
    const order = component.orders()[0];
    component.openDrawer(order);

    component.onStatusChange({ order, newStatus: 'READY' });
    const updated = component.orders().find((o) => o.id === order.id);
    expect(updated?.status).toBe('READY');
    expect(component.selectedOrder()?.status).toBe('READY');
  });
});

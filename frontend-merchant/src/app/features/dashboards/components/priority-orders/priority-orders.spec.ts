import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PriorityOrders } from './priority-orders';
import { OrderService } from '../../../../core/services/order.service';
import { StoreService } from '../../../../core/services/store.service';
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
    const storeService = TestBed.inject(StoreService);
    storeService.currentStore.set({
      id: 'store-1',
      merchantUserId: 'usr-1',
      name: 'Tienda Test',
      slug: 'tienda-test',
      pickupEnabled: true,
      deliveryEnabled: true,
      status: 'ACTIVE',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    });
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
    const httpMock = TestBed.inject(HttpTestingController);
    const order = component.orders()[0];
    component.openDrawer(order);

    component.onStatusChange({ order, newStatus: 'READY' });

    const req = httpMock.expectOne((r) => r.method === 'PATCH' && r.url.includes('/orders/ord-test-1/status'));
    expect(req.request.body).toEqual({ status: 'READY' });
    req.flush({
      id: 'ord-test-1',
      orderNumber: '#ORD-0001',
      storeId: 'store-1',
      customerUserId: 'usr-1',
      paymentId: 'pay-1',
      capacityReservationId: 'cap-1',
      status: 'READY',
      deliveryMode: 'DELIVERY',
      serviceDate: '2026-09-25',
      serviceStartTime: '10:00:00',
      serviceEndTime: '11:30:00',
      customerName: 'María García',
      customerPhone: '987654321',
      customerEmail: null,
      deliveryAddress: 'Av. Larco 123',
      deliveryReference: null,
      deliveryLatitude: null,
      deliveryLongitude: null,
      currency: 'PEN',
      productsSubtotalAmount: 45.0,
      discountAmount: 0,
      discountCode: null,
      deliveryFeeAmount: 0,
      includedTaxAmount: 0,
      totalAmount: 45.0,
      confirmedAt: '2026-09-25T10:00:00Z',
      updatedAt: '2026-09-25T10:05:00Z',
      items: [],
      statusHistory: [],
    });

    const updated = component.orders().find((o) => o.id === order.id);
    expect(updated?.status).toBe('READY');
  });

  it('debe consultar el detalle real del pedido al backend al abrir el drawer', () => {
    const httpMock = TestBed.inject(HttpTestingController);
    const order = component.orders()[0];

    component.openDrawer(order);

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url.endsWith('/orders/ord-test-1'));
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'ord-test-1',
      orderNumber: '#ORD-0001',
      storeId: 'store-1',
      customerUserId: 'usr-1',
      paymentId: 'pay-1',
      capacityReservationId: 'cap-1',
      status: 'IN_PREPARATION',
      deliveryMode: 'DELIVERY',
      serviceDate: '2026-09-25',
      serviceStartTime: '10:00:00',
      serviceEndTime: '11:30:00',
      customerName: 'María García Detalle Actualizado',
      customerPhone: '987654321',
      customerEmail: 'maria@example.com',
      deliveryAddress: 'Av. Larco 123',
      deliveryReference: 'Piso 4',
      deliveryLatitude: null,
      deliveryLongitude: null,
      currency: 'PEN',
      productsSubtotalAmount: 45.0,
      discountAmount: 0,
      discountCode: null,
      deliveryFeeAmount: 0,
      includedTaxAmount: 0,
      totalAmount: 45.0,
      confirmedAt: '2026-09-25T10:00:00Z',
      updatedAt: '2026-09-25T10:00:00Z',
      items: [
        {
          id: 'item-1',
          variantId: 'var-1',
          productName: 'Torta de Chocolate',
          variantName: 'Porción personal',
          quantity: 1,
          unitPriceAmount: 45.0,
          subtotalAmount: 45.0,
        },
      ],
      statusHistory: [],
    });

    expect(component.selectedOrder()?.customerName).toBe('María García Detalle Actualizado');
  });

  it('no debe atribuir canal WhatsApp cuando el backend no proporciona channelLabel', () => {
    orderService.orders.set([
      {
        ...mockOrder,
        id: 'ord-no-channel',
        channel: undefined,
        channelLabel: undefined,
      },
    ]);
    fixture.detectChanges();

    const mapped = component.orders()[0];
    expect(mapped.channelLabel).toBeUndefined();
    expect(mapped.channelIcon).toBeUndefined();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).not.toContain('WhatsApp');
  });

  it('debe generar mensaje de cancelación con restitución de inventario y capacidad para estado CONFIRMED', () => {
    const confirmedOrder = {
      ...component.orders()[0],
      status: 'CONFIRMED' as const,
    };
    component.onCancelOrder(confirmedOrder);
    expect(component.isCancelModalOpen()).toBe(true);

    const msg = component.cancelModalMessage();
    expect(msg).toContain('Se liberará el cupo de capacidad');
    expect(msg).toContain('se restituirá el inventario');
    expect(msg).toContain('RN-ORD-13');
    expect(msg).toContain('PENDING');
  });

  it('debe generar mensaje de cancelación sin restitución para estado IN_PREPARATION', () => {
    const inPrepOrder = {
      ...component.orders()[0],
      status: 'IN_PREPARATION' as const,
    };
    component.onCancelOrder(inPrepOrder);
    expect(component.isCancelModalOpen()).toBe(true);

    const msg = component.cancelModalMessage();
    expect(msg).toContain('NO se restituirá la capacidad');
    expect(msg).toContain('ni el inventario consumido en cocina');
    expect(msg).toContain('RN-ORD-13');
    expect(msg).toContain('PENDING');
  });

  it('no debe permitir cancelar pedidos en estados no autorizados (READY, COMPLETED)', () => {
    const readyOrder = {
      ...component.orders()[0],
      status: 'READY' as const,
    };
    component.onCancelOrder(readyOrder);
    expect(component.isCancelModalOpen()).toBe(false);
  });

  it('no debe abrir modal de cancelación si el pedido está en actualización o pendiente de verificación', () => {
    orderService['updatingOrderIds'].set(new Set([mockOrder.id]));
    component.onCancelOrder(component.orders()[0]);
    expect(component.isCancelModalOpen()).toBe(false);

    orderService['updatingOrderIds'].set(new Set());
    orderService['pendingVerificationOrderIds'].set(new Set([mockOrder.id]));
    component.onCancelOrder(component.orders()[0]);
    expect(component.isCancelModalOpen()).toBe(false);
  });
});

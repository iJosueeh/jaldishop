import { of } from 'rxjs';
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

  it('debe avanzar el estado de una comanda respetando la modalidad de entrega', () => {
    const updateSpy = vi.spyOn(orderService, 'updateOrderStatus').mockReturnValue(of(null));
    const confirmedOrder = component.orders().find((o) => o.status === 'CONFIRMED')!;
    component.onStatusAdvance(confirmedOrder);
    expect(updateSpy).toHaveBeenCalledWith(confirmedOrder.id, 'IN_PREPARATION');

    // READY con PICKUP (ord-2) debe avanzar a COMPLETED
    const pickupOrder = component.orders().find((o) => o.id === 'ord-2')!;
    component.onStatusAdvance(pickupOrder);
    expect(updateSpy).toHaveBeenCalledWith(pickupOrder.id, 'COMPLETED');
  });

  it('debe propagar cambio de estado desde el drawer invocando updateOrderStatus', () => {
    const updateSpy = vi.spyOn(orderService, 'updateOrderStatus').mockReturnValue(of(null));
    component.onDrawerStatusChange({ order: mockOrders[0], newStatus: 'READY' });
    expect(updateSpy).toHaveBeenCalledWith('ord-1', 'READY');
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

  it('debe entregar la página directamente en paginatedOrders sin aplicar slice secundario', () => {
    expect(component.paginatedOrders().length).toBe(2);
    expect(component.paginatedOrders()).toEqual(component.orders());
  });

  it('debe preferir selectedOrderDetail sobre selectedOrder en drawerOrder cuando esté disponible', () => {
    orderService.selectedOrder.set(mockOrders[0]);
    expect(component.drawerOrder()?.id).toBe('ord-1');

    orderService.selectedOrderDetail.set({
      id: 'ord-1-detail',
      orderNumber: '#PED-DETAIL',
      customerName: 'Cliente Detalle',
      icon: 'matRestaurantOutline',
      itemSummary: '1x Detallado',
      deliveryMode: 'DELIVERY',
      scheduledTime: '15:00',
      deliveryTimeLabel: '15:00 - 16:00',
      isUrgent: false,
      status: 'CONFIRMED',
      totalAmount: 120,
    });

    expect(component.drawerOrder()?.id).toBe('ord-1-detail');
    expect(component.drawerOrder()?.customerName).toBe('Cliente Detalle');
  });
});

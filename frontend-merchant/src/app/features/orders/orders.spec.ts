import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Orders } from './orders';
import { OrderService } from '../../core/services/order.service';
import { CapacityService } from '../../core/services/capacity.service';
import { ToastService } from '../../core/services/toast.service';

describe('Orders', () => {
  let component: Orders;
  let fixture: ComponentFixture<Orders>;
  let orderService: OrderService;

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
    await fixture.whenStable();
  });

  it('should create and load initial orders', () => {
    expect(component).toBeTruthy();
    expect(component.orders().length).toBeGreaterThan(0);
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

  it('debe abrir modal de nuevo pedido y registrar la orden', () => {
    component.onNewOrder();
    expect(orderService.isCreateModalOpen()).toBe(true);

    const initialLen = orderService.orders().length;
    component.onOrderCreated({
      ...orderService.orders()[0],
      id: 'test-modal-1',
      orderNumber: '#PED-8888',
    });

    expect(orderService.orders().length).toBe(initialLen + 1);
    expect(orderService.orders()[0].id).toBe('test-modal-1');

    component.onCloseCreateModal();
    expect(orderService.isCreateModalOpen()).toBe(false);
  });
});

import { TestBed } from '@angular/core/testing';
import { OrderService } from './order.service';
import { ToastService } from './toast.service';

describe('OrderService', () => {
  let service: OrderService;
  let toastMock: any;

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

  it('debe crearse correctamente con pedidos iniciales y KPIs calculados', () => {
    expect(service).toBeTruthy();
    expect(service.orders().length).toBeGreaterThan(0);
    expect(service.totalOrdersCount()).toBe(service.orders().length);
    expect(service.filteredOrders().length).toBe(service.orders().length);
  });

  it('debe filtrar pedidos por pestaña de estado', () => {
    service.setActiveTab('IN_PREPARATION');
    expect(service.activeTab()).toBe('IN_PREPARATION');
    const inPrep = service.filteredOrders();
    expect(inPrep.every((o) => o.status === 'IN_PREPARATION')).toBe(true);

    service.setActiveTab('READY');
    const ready = service.filteredOrders();
    expect(ready.every((o) => o.status === 'READY')).toBe(true);
  });

  it('debe filtrar pedidos por búsqueda de texto', () => {
    service.setSearchQuery('Valeria');
    const filtered = service.filteredOrders();
    expect(filtered.length).toBe(1);
    expect(filtered[0].customerName).toBe('Valeria Ramos');
  });

  it('debe actualizar el estado del pedido y mostrar toast', () => {
    const firstOrder = service.orders()[0];
    service.updateOrderStatus(firstOrder.id, 'READY');

    const updated = service.orders().find((o) => o.id === firstOrder.id);
    expect(updated?.status).toBe('READY');
    expect(toastMock.success).toHaveBeenCalledWith('Pedido actualizado a listo para entrega.');
  });

  it('debe abrir y cerrar drawer de detalle', () => {
    const firstOrder = service.orders()[0];
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

    const initialCount = service.orders().length;
    const newOrder = {
      ...service.orders()[0],
      id: 'new-ord-999',
      orderNumber: '#PED-9999',
    };

    service.addOrder(newOrder);
    expect(service.orders().length).toBe(initialCount + 1);
    expect(service.orders()[0].id).toBe('new-ord-999');
    expect(toastMock.success).toHaveBeenCalledWith('Pedido #PED-9999 registrado exitosamente.');
  });
});

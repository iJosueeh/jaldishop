import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PriorityOrders } from './priority-orders';

describe('PriorityOrders', () => {
  let component: PriorityOrders;
  let fixture: ComponentFixture<PriorityOrders>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriorityOrders],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PriorityOrders);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
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


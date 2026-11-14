import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderDetailsDrawer } from './order-details-drawer';
import { DashboardPriorityOrder } from '../../../../core/models/dashboard.models';

describe('OrderDetailsDrawer', () => {
  let component: OrderDetailsDrawer;
  let fixture: ComponentFixture<OrderDetailsDrawer>;

  const mockOrder: DashboardPriorityOrder = {
    id: 'ord-1',
    orderNumber: '#ORD-0842',
    customerName: 'María Fernanda Ruiz',
    customerPhone: '987654321',
    channel: 'WHATSAPP',
    channelLabel: 'WhatsApp',
    channelIcon: 'matChatOutline',
    icon: 'matRestaurantOutline',
    itemSummary: '1x Torta Selva Negra (Grande)',
    deliveryMode: 'DELIVERY',
    deliveryAddress: 'Av. Dos de Mayo 1420, San Isidro',
    scheduledTime: '10:00:00',
    deliveryTimeLabel: '10:00 - 11:30',
    isUrgent: true,
    status: 'IN_PREPARATION',
    totalAmount: 65.0,
    items: [
      {
        name: 'Torta Selva Negra',
        variant: 'Grande',
        quantity: 1,
        unitPrice: 65.0,
        totalPrice: 65.0,
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderDetailsDrawer],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailsDrawer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe formatear el total correctamente', () => {
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();
    expect(component.formattedTotal()).toBe('S/ 65.00');
  });

  it('debe emitir statusChange al avanzar estado de IN_PREPARATION a READY', () => {
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();

    let emitted: any = null;
    component.statusChange.subscribe((res) => {
      emitted = res;
    });

    component.onAdvanceStatus();
    expect(emitted).toBeTruthy();
    expect(emitted.newStatus).toBe('READY');
  });

  it('debe traducir correctamente los estados operativos al español', () => {
    fixture.componentRef.setInput('order', { ...mockOrder, status: 'IN_PREPARATION' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('EN PREPARACIÓN');

    fixture.componentRef.setInput('order', { ...mockOrder, status: 'CONFIRMED' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('CONFIRMADO');

    fixture.componentRef.setInput('order', { ...mockOrder, status: 'READY' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('LISTO');

    fixture.componentRef.setInput('order', { ...mockOrder, status: 'OUT_FOR_DELIVERY' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('EN CAMINO');

    fixture.componentRef.setInput('order', { ...mockOrder, status: 'COMPLETED' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('COMPLETADO');

    fixture.componentRef.setInput('order', { ...mockOrder, status: 'CANCELLED' });
    fixture.detectChanges();
    expect(component.statusLabel()).toBe('CANCELADO');
  });

  it('debe activar isClosing y emitir closeDrawer tras la animación de salida', () => {
    vi.useFakeTimers();
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();

    expect(component.isClosing()).toBe(false);

    let emitted = false;
    component.closeDrawer.subscribe(() => {
      emitted = true;
    });

    component.handleClose();
    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(250);
    expect(emitted).toBe(true);
    expect(component.isClosing()).toBe(false);
    vi.useRealTimers();
  });

  it('debe responder al evento de tecla Escape cuando está abierto', () => {
    const spy = vi.spyOn(component, 'handleClose');
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    component.onEscapeKey(event);

    expect(spy).toHaveBeenCalled();
  });

  it('no debe renderizar la etiqueta de canal cuando channelLabel es indefinido', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', {
      ...mockOrder,
      channel: undefined,
      channelLabel: undefined,
      channelIcon: undefined,
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).not.toContain('Canal:');
  });

  it('debe mostrar estado de carga cuando isLoading es true y order es null', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', null);
    fixture.componentRef.setInput('isLoading', true);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Cargando detalle del pedido...');
  });

  it('debe mostrar mensaje de error cuando errorMessage está presente', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', null);
    fixture.componentRef.setInput('errorMessage', 'Pedido no encontrado en esta tienda');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Pedido no encontrado en esta tienda');
  });

  it('debe avanzar a OUT_FOR_DELIVERY cuando READY es DELIVERY', () => {
    fixture.componentRef.setInput('order', { ...mockOrder, status: 'READY', deliveryMode: 'DELIVERY' });
    fixture.detectChanges();

    let emitted: any = null;
    component.statusChange.subscribe((res) => {
      emitted = res;
    });

    component.onAdvanceStatus();
    expect(emitted?.newStatus).toBe('OUT_FOR_DELIVERY');
  });

  it('debe avanzar a COMPLETED cuando READY es PICKUP', () => {
    fixture.componentRef.setInput('order', { ...mockOrder, status: 'READY', deliveryMode: 'PICKUP' });
    fixture.detectChanges();

    let emitted: any = null;
    component.statusChange.subscribe((res) => {
      emitted = res;
    });

    component.onAdvanceStatus();
    expect(emitted?.newStatus).toBe('COMPLETED');
  });

  it('debe deshabilitar botones de acción cuando isUpdating es true', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', mockOrder);
    fixture.componentRef.setInput('isUpdating', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('aside button[disabled]') as HTMLButtonElement;
    expect(button).toBeTruthy();
    expect(button.disabled).toBe(true);
  });

  it('debe mostrar banner de verificación pendiente y emitir verifyStatus al hacer clic', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', mockOrder);
    fixture.componentRef.setInput('isPendingVerification', true);
    fixture.detectChanges();

    let emitted: DashboardPriorityOrder | null = null;
    component.verifyStatus.subscribe((order) => {
      emitted = order;
    });

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Estado pendiente de verificación con el servidor');

    const verifyBtn = compiled.querySelector('aside button.bg-amber-600') as HTMLButtonElement;
    expect(verifyBtn).toBeTruthy();
    verifyBtn.click();

    expect(emitted).toEqual(mockOrder);
  });

  it('debe emitir cancelOrder al presionar botón Cancelar en estados permitidos', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('order', mockOrder);
    fixture.detectChanges();

    let emitted: DashboardPriorityOrder | null = null;
    component.cancelOrder.subscribe((order) => {
      emitted = order;
    });

    const cancelBtn = fixture.nativeElement.querySelector('aside button[title="Cancelar pedido"]') as HTMLButtonElement;
    expect(cancelBtn).toBeTruthy();
    cancelBtn.click();

    expect(emitted).toEqual(mockOrder);
  });

  it('debe validar canCancel para cada estado', () => {
    expect(component.canCancel('CONFIRMED')).toBe(true);
    expect(component.canCancel('IN_PREPARATION')).toBe(true);
    expect(component.canCancel('READY')).toBe(false);
    expect(component.canCancel('COMPLETED')).toBe(false);
    expect(component.canCancel('CANCELLED')).toBe(false);
  });
});


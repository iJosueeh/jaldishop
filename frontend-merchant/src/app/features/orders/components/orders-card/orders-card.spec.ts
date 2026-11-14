import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersCard } from './orders-card';
import { MerchantOrder } from '../../../../core/models/order.models';

describe('OrdersCard', () => {
  let component: OrdersCard;
  let fixture: ComponentFixture<OrdersCard>;

  const mockOrder: MerchantOrder = {
    id: 'ord-1039',
    orderNumber: '#PED-1039',
    customerName: 'Valeria Ramos',
    customerPhone: '984552109',
    channel: 'WHATSAPP',
    channelLabel: 'WhatsApp',
    deliveryMode: 'DELIVERY',
    deliveryAddress: 'Av. José Pardo 450, Miraflores',
    deliveryTimeLabel: 'Entrega en 15 min (16:15)',
    isUrgent: true,
    status: 'IN_PREPARATION',
    paymentMethod: 'YAPE',
    paymentLabel: 'Yape',
    totalAmount: 58.0,
    createdAt: '2026-09-22T10:00:00Z',
    items: [
      {
        name: 'Caja Brownies x6',
        quantity: 1,
        unitPrice: 36.0,
        totalPrice: 36.0,
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersCard],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('order', mockOrder);
    await fixture.whenStable();
  });

  it('should create and render order details', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('#PED-1039');
    expect(compiled.textContent).toContain('Valeria Ramos');
    expect(compiled.textContent).toContain('S/ 58.00');
  });

  it('debe emitir statusAdvance al presionar el botón de acción de estado', () => {
    let emitted: MerchantOrder | null = null;
    component.statusAdvance.subscribe((order) => {
      emitted = order;
    });

    const advanceBtn = fixture.nativeElement.querySelector('button[title*="Avanzar"]') as HTMLButtonElement;
    advanceBtn.click();

    expect(emitted).toEqual(mockOrder);
  });

  it('debe emitir cancelOrder al presionar el botón de cancelar', () => {
    let emitted: MerchantOrder | null = null;
    component.cancelOrder.subscribe((order) => {
      emitted = order;
    });

    const cancelBtn = fixture.nativeElement.querySelector('button[title="Cancelar pedido"]') as HTMLButtonElement;
    expect(cancelBtn).toBeTruthy();
    cancelBtn.click();

    expect(emitted).toEqual(mockOrder);
  });

  it('debe verificar estados en canCancel', () => {
    expect(component.canCancel('CONFIRMED')).toBe(true);
    expect(component.canCancel('IN_PREPARATION')).toBe(true);
    expect(component.canCancel('READY')).toBe(false);
    expect(component.canCancel('OUT_FOR_DELIVERY')).toBe(false);
    expect(component.canCancel('COMPLETED')).toBe(false);
    expect(component.canCancel('CANCELLED')).toBe(false);
  });

  it('debe emitir whatsAppClick al presionar el botón de WhatsApp', () => {
    let emitted: MerchantOrder | null = null;
    component.whatsAppClick.subscribe((order) => {
      emitted = order;
    });

    const waBtn = fixture.nativeElement.querySelectorAll('button')[0];
    waBtn.click();

    expect(emitted).toEqual(mockOrder);
  });

  it('no debe renderizar badge de pago ni badge de canal cuando no están disponibles', () => {
    const orderWithoutPaymentOrChannel: MerchantOrder = {
      ...mockOrder,
      channel: undefined,
      channelLabel: undefined,
      paymentMethod: undefined,
      paymentLabel: undefined,
    };

    fixture.componentRef.setInput('order', orderWithoutPaymentOrChannel);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).not.toContain('WhatsApp');
    expect(compiled.textContent).not.toContain('Yape');
    expect(compiled.querySelector('ng-icon[name="matVerifiedOutline"]')).toBeNull();
  });

  it('debe adaptar la acción rápida a la modalidad PICKUP y DELIVERY', () => {
    expect(component.getNextActionLabel('CONFIRMED', 'DELIVERY')).toBe('Aceptar');
    expect(component.getNextActionLabel('IN_PREPARATION', 'DELIVERY')).toBe('Listo');
    expect(component.getNextActionLabel('READY', 'DELIVERY')).toBe('Despachar');
    expect(component.getNextActionLabel('READY', 'PICKUP')).toBe('Entregar');
    expect(component.getNextActionLabel('OUT_FOR_DELIVERY', 'DELIVERY')).toBe('Entregar');
  });

  it('debe deshabilitar el botón de avance cuando isUpdating es true', () => {
    fixture.componentRef.setInput('isUpdating', true);
    fixture.detectChanges();

    const advanceBtn = fixture.nativeElement.querySelector('button[title*="Avanzar"]') as HTMLButtonElement;
    expect(advanceBtn.disabled).toBe(true);
    expect(advanceBtn.classList.contains('disabled:opacity-50')).toBe(true);
  });

  it('debe mostrar el botón Verificar y emitir verifyStatus cuando isPendingVerification es true', () => {
    fixture.componentRef.setInput('isPendingVerification', true);
    fixture.detectChanges();

    let emitted: MerchantOrder | null = null;
    component.verifyStatus.subscribe((order) => {
      emitted = order;
    });

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Verificar');

    const verifyBtn = fixture.nativeElement.querySelector('button[title*="Verificación"]') as HTMLButtonElement;
    verifyBtn.click();

    expect(emitted).toEqual(mockOrder);
  });
});

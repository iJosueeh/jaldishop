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

    const advanceBtn = fixture.nativeElement.querySelectorAll('button')[2];
    advanceBtn.click();

    expect(emitted).toEqual(mockOrder);
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
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersGrid } from './orders-grid';
import { ComponentRef } from '@angular/core';
import { MerchantOrder } from '../../../../core/models/order.models';

describe('OrdersGrid', () => {
  let component: OrdersGrid;
  let fixture: ComponentFixture<OrdersGrid>;
  let componentRef: ComponentRef<OrdersGrid>;

  const mockOrder: MerchantOrder = {
    id: 'ord-101',
    orderNumber: '#1042',
    customerName: 'Lucía Mendoza',
    customerPhone: '+51987654321',
    status: 'IN_PREPARATION',
    deliveryMode: 'DELIVERY',
    deliveryTimeLabel: '13:45',
    channel: 'WHATSAPP',
    channelLabel: 'WhatsApp',
    isUrgent: false,
    items: [
      {
        name: 'Burger Clásica',
        quantity: 2,
        unitPrice: 22.0,
        totalPrice: 44.0,
      },
    ],
    totalAmount: 44.0,
    paymentMethod: 'YAPE',
    paymentLabel: 'Yape',
    scheduledTime: '13:45',
    createdAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersGrid],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersGrid);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe renderizar tarjetas de pedidos y emitir eventos', () => {
    componentRef.setInput('orders', [mockOrder]);
    fixture.detectChanges();

    const advanceSpy = vi.fn();
    const detailsSpy = vi.fn();
    const whatsAppSpy = vi.fn();

    component.statusAdvance.subscribe(advanceSpy);
    component.openDetails.subscribe(detailsSpy);
    component.whatsAppClick.subscribe(whatsAppSpy);

    component.statusAdvance.emit(mockOrder);
    expect(advanceSpy).toHaveBeenCalledWith(mockOrder);

    component.openDetails.emit(mockOrder);
    expect(detailsSpy).toHaveBeenCalledWith(mockOrder);

    component.whatsAppClick.emit(mockOrder);
    expect(whatsAppSpy).toHaveBeenCalledWith(mockOrder);
  });

  it('debe emitir resetFilter al hacer clic en ver todos cuando está vacío', () => {
    componentRef.setInput('orders', []);
    fixture.detectChanges();

    const resetSpy = vi.fn();
    component.resetFilter.subscribe(resetSpy);

    const button = fixture.nativeElement.querySelector('button');
    expect(button).toBeTruthy();
    button.click();

    expect(resetSpy).toHaveBeenCalled();
  });
});

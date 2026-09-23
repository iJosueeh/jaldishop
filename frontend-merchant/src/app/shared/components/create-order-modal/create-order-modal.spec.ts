import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateOrderModal } from './create-order-modal';
import { ComponentRef } from '@angular/core';

describe('CreateOrderModal', () => {
  let component: CreateOrderModal;
  let fixture: ComponentFixture<CreateOrderModal>;
  let componentRef: ComponentRef<CreateOrderModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateOrderModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateOrderModal);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular subtotal y total general con costo de delivery', () => {
    component.itemsFormArray.push(component.createItemFormGroup('Brownies', 1, 15));
    const items = component.itemsFormArray;
    items.at(0).patchValue({ quantity: 2, unitPrice: 20 });
    items.at(1).patchValue({ quantity: 1, unitPrice: 15 });
    component.orderForm.patchValue({ deliveryMode: 'DELIVERY', deliveryFee: 7.0 });

    expect(component.calculateItemsSubtotal()).toBe(55);
    expect(component.calculateGrandTotal()).toBe(62);
  });

  it('debe manejar incremento y decremento de cantidades mediante steppers', () => {
    component.itemsFormArray.at(0).patchValue({ quantity: 1 });
    component.incrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(2);

    component.decrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(1);

    // No debe decrementar por debajo de 1
    component.decrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(1);
  });

  it('debe permitir agregar y remover ítems de comanda', () => {
    expect(component.itemsFormArray.length).toBe(1);
    component.addCustomItem();
    expect(component.itemsFormArray.length).toBe(2);
    component.removeItem(1);
    expect(component.itemsFormArray.length).toBe(1);
    // No debe remover si solo queda 1
    component.removeItem(0);
    expect(component.itemsFormArray.length).toBe(1);
  });

  it('debe actualizar horario programado con atajos rápidos', () => {
    component.setQuickTime(30);
    expect(component.orderForm.get('scheduledTime')?.value).toBeTruthy();
  });

  it('debe cambiar canales y métodos de pago', () => {
    component.setChannel('INSTAGRAM');
    expect(component.orderForm.get('channel')?.value).toBe('INSTAGRAM');

    component.setPaymentMethod('PLIN');
    expect(component.orderForm.get('paymentMethod')?.value).toBe('PLIN');

    component.onDeliveryModeChange('PICKUP');
    expect(component.orderForm.get('deliveryMode')?.value).toBe('PICKUP');
    expect(component.orderForm.get('deliveryFee')?.value).toBe(0);
  });

  it('debe emitir orderCreated con datos válidos al enviar formulario', () => {
    componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const orderSpy = vi.fn();
    component.orderCreated.subscribe(orderSpy);

    component.orderForm.patchValue({
      customerName: 'Lucía Méndez',
      customerPhone: '987654321',
      channel: 'WHATSAPP',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. Brasil 123',
      deliveryReference: 'Frente a Metro',
      scheduledTime: '17:00',
      paymentMethod: 'YAPE',
    });

    component.onSubmit();
    expect(orderSpy).toHaveBeenCalled();
    const emittedOrder = orderSpy.mock.calls[0][0];
    expect(emittedOrder.customerName).toBe('Lucía Méndez');
    expect(emittedOrder.status).toBe('CONFIRMED');
  });

  it('debe emitir closeModal al invocar onClose', () => {
    const closeSpy = vi.fn();
    component.closeModal.subscribe(closeSpy);

    component.onClose();
    expect(closeSpy).toHaveBeenCalled();
  });

  it('debe alternar pestañas en vista móvil mediante setMobileTab', () => {
    expect(component.activeMobileTab()).toBe('DATA');
    component.setMobileTab('ITEMS');
    expect(component.activeMobileTab()).toBe('ITEMS');
  });

  it('debe filtrar catálogo en el combobox de búsqueda limitando a máximo 4 resultados', () => {
    component.productSearchQuery.set('Torta');
    const filtered = component.filteredCatalogProducts();
    expect(filtered.length).toBeLessThanOrEqual(4);
    expect(filtered.some((p) => p.name.includes('Torta'))).toBe(true);
  });

  it('debe agregar producto seleccionado desde el buscador e incrementar cantidad si ya existe', () => {
    const productA = component.catalogProducts()[0]; // Torta de Chocolate (ya en items iniciales)
    const productB = component.catalogProducts()[1]; // Brownies

    expect(component.itemsFormArray.length).toBe(1);

    // Al seleccionar producto ya existente en la comanda, incrementa cantidad
    component.selectProductFromSearch(productA);
    expect(component.itemsFormArray.length).toBe(1);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(2);

    // Al seleccionar nuevo producto, lo agrega a la comanda
    component.selectProductFromSearch(productB);
    expect(component.itemsFormArray.length).toBe(2);
    expect(component.itemsFormArray.at(1).get('name')?.value).toBe(productB.name);
    expect(component.itemsFormArray.at(1).get('unitPrice')?.value).toBe(productB.price);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { CreateOrderModal } from './create-order-modal';
import { ComponentRef } from '@angular/core';
import { StoreService } from '../../../core/services/store.service';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.models';
import { MerchantOrderDetailResponse } from '../../../core/models/order.models';

describe('CreateOrderModal', () => {
  let component: CreateOrderModal;
  let fixture: ComponentFixture<CreateOrderModal>;
  let componentRef: ComponentRef<CreateOrderModal>;
  let productService: ProductService;

  const mockCatalogProducts: Product[] = [
    {
      id: 'prod-1',
      name: 'Camisa Lino Premium',
      categoryName: 'Ropa',
      status: 'ACTIVE',
      minPrice: 78.0,
      maxPrice: 78.0,
      variants: [
        {
          id: 'v-p1',
          productId: 'prod-1',
          sku: 'CAM-LINO-M',
          priceAmount: 78.0,
          status: 'ACTIVE',
          stock: 10,
        } as any,
      ],
    } as any,
    {
      id: 'prod-2',
      name: 'Pantalón Chino',
      categoryName: 'Ropa',
      status: 'ACTIVE',
      minPrice: 95.0,
      maxPrice: 95.0,
      variants: [
        {
          id: 'v-p2',
          productId: 'prod-2',
          sku: 'PAN-CHINO-32',
          priceAmount: 95.0,
          status: 'ACTIVE',
          stock: 8,
        } as any,
      ],
    } as any,
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateOrderModal],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateOrderModal);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    productService = TestBed.inject(ProductService);
    productService.products.set(mockCatalogProducts);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular subtotal, unidades totales y total general con costo de delivery', () => {
    component.itemsFormArray.push(component.createItemFormGroup('Producto A', 2, 20, 'v-1'));
    component.itemsFormArray.push(component.createItemFormGroup('Producto B', 3, 15, 'v-2'));
    component.orderForm.patchValue({ deliveryMode: 'DELIVERY', deliveryFee: 7.0 });

    expect(component.calculateTotalUnits()).toBe(5);
    expect(component.calculateItemsSubtotal()).toBe(85);
    expect(component.calculateGrandTotal()).toBe(92);
  });

  it('debe manejar incremento y decremento de cantidades mediante steppers', () => {
    component.itemsFormArray.push(component.createItemFormGroup('Producto A', 1, 15, 'v-1'));
    component.incrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(2);

    component.decrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(1);

    // No debe decrementar por debajo de 1
    component.decrementQuantity(0);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(1);
  });

  it('debe permitir agregar y remover ítems de comanda', () => {
    expect(component.itemsFormArray.length).toBe(0);
    component.addCustomItem();
    expect(component.itemsFormArray.length).toBe(1);
    component.addCustomItem();
    expect(component.itemsFormArray.length).toBe(2);
    component.removeItem(1);
    expect(component.itemsFormArray.length).toBe(1);
    component.removeItem(0);
    expect(component.itemsFormArray.length).toBe(0);
  });

  it('debe actualizar horario programado con atajos rápidos', () => {
    component.setQuickTime(30);
    expect(component.orderForm.get('scheduledTime')?.value).toBeTruthy();
  });

  it('debe ajustar horario programado con steppers (+15m y -15m)', () => {
    component.orderForm.patchValue({ scheduledTime: '14:00' });
    
    component.adjustScheduledTime(15);
    expect(component.orderForm.get('scheduledTime')?.value).toBe('14:15');

    component.adjustScheduledTime(-15);
    expect(component.orderForm.get('scheduledTime')?.value).toBe('14:00');

    component.adjustScheduledTime(-30);
    expect(component.orderForm.get('scheduledTime')?.value).toBe('13:30');
  });

  it('debe calcular scheduledTimeRelativeText reactivamente', () => {
    component.orderForm.patchValue({ scheduledTime: '23:59' });
    expect(component.scheduledTimeRelativeText()).toBeTruthy();
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

  it('debe emitir orderCreated e invocar createMerchantOrder al enviar comanda válida', () => {
    componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const orderSpy = vi.fn();
    component.orderCreated.subscribe(orderSpy);

    const storeService = TestBed.inject(StoreService);
    storeService.currentStore.set({
      id: 'store-100',
      name: 'Mi Tienda',
      slug: 'mi-tienda',
      status: 'ACTIVE',
    } as any);

    const orderService = TestBed.inject(OrderService);
    const mockDetailResponse: MerchantOrderDetailResponse = {
      id: 'ord-new-1',
      orderNumber: '#ORD-00000001',
      storeId: 'store-100',
      customerUserId: 'usr-1',
      paymentId: '',
      capacityReservationId: '',
      status: 'CONFIRMED',
      deliveryMode: 'DELIVERY',
      serviceDate: '2026-11-20',
      serviceStartTime: '17:00:00',
      serviceEndTime: '17:30:00',
      customerName: 'Lucía Méndez',
      customerPhone: '987654321',
      customerEmail: null,
      deliveryAddress: 'Av. Brasil 123',
      deliveryReference: 'Frente a Metro',
      deliveryLatitude: null,
      deliveryLongitude: null,
      currency: 'PEN',
      productsSubtotalAmount: 78.0,
      discountAmount: 0,
      discountCode: null,
      deliveryFeeAmount: 5.0,
      includedTaxAmount: 0,
      totalAmount: 83.0,
      confirmedAt: '2026-11-20T17:00:00Z',
      updatedAt: '2026-11-20T17:00:00Z',
      items: [
        {
          id: 'item-1',
          variantId: 'v-p1',
          productName: 'Camisa Lino Premium',
          variantName: '',
          quantity: 1,
          unitPriceAmount: 78.0,
          subtotalAmount: 78.0,
        },
      ],
      statusHistory: [],
    };

    const createSpy = vi
      .spyOn(orderService, 'createMerchantOrder')
      .mockReturnValue(of(mockDetailResponse));

    component.itemsFormArray.push(
      component.createItemFormGroup('Camisa Lino Premium', 1, 78.0, 'v-p1')
    );

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
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Registro en vivo');
    const submitBtn = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submitBtn.disabled).toBe(false);

    component.onSubmit();
    expect(createSpy).toHaveBeenCalledWith(
      'store-100',
      expect.objectContaining({
        customerName: 'Lucía Méndez',
        customerPhone: '987654321',
        deliveryMode: 'DELIVERY',
        paymentMethod: 'YAPE',
      }),
    );
    expect(orderSpy).toHaveBeenCalled();
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
    component.productSearchQuery.set('Camisa');
    const filtered = component.filteredCatalogProducts();
    expect(filtered.length).toBeLessThanOrEqual(4);
    expect(filtered.some((p) => p.name.includes('Camisa'))).toBe(true);
  });

  it('debe agregar producto seleccionado desde el buscador e incrementar cantidad si ya existe', () => {
    const productA = component.catalogProducts()[0]; // Camisa Lino
    const productB = component.catalogProducts()[1]; // Pantalón Chino

    expect(component.itemsFormArray.length).toBe(0);

    // Al seleccionar producto, lo agrega a la comanda
    component.selectProductFromSearch(productA);
    expect(component.itemsFormArray.length).toBe(1);
    expect(component.itemsFormArray.at(0).get('name')?.value).toBe(productA.name);
    expect(component.itemsFormArray.at(0).get('quantity')?.value).toBe(1);

    // Al seleccionar el mismo producto, incrementa su cantidad
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

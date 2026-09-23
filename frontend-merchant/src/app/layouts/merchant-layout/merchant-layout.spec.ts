import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MerchantLayout } from './merchant-layout';
import { StoreService } from '../../core/services/store.service';
import { ProfileService } from '../../core/services/profile.service';
import { OrderService } from '../../core/services/order.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('MerchantLayout', () => {
  let component: MerchantLayout;
  let fixture: ComponentFixture<MerchantLayout>;
  let orderService: OrderService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MerchantLayout],
      providers: [
        StoreService,
        ProfileService,
        OrderService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MerchantLayout);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe alternar el estado del menú móvil', () => {
    expect(component.isMobileMenuOpen()).toBe(false);
    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(true);
    component.closeMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(false);
  });

  it('debe registrar un nuevo pedido mediante onOrderCreated', () => {
    const addOrderSpy = vi.spyOn(orderService, 'addOrder');
    const mockOrder = {
      ...orderService.orders()[0],
      id: 'mock-layout-order',
    };

    component.onOrderCreated(mockOrder);
    expect(addOrderSpy).toHaveBeenCalledWith(mockOrder);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Header } from './header';
import { ProfileService } from '../../../../core/services/profile.service';
import { OrderService } from '../../../../core/services/order.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let orderService: OrderService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        ProfileService,
        OrderService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe abrir el modal de nuevo pedido al llamar a handleNewOrder', () => {
    const modalSpy = vi.spyOn(orderService, 'openCreateModal');
    component.handleNewOrder();
    expect(modalSpy).toHaveBeenCalled();
  });

  it('debe emitir toggleSidebar al accionar el botón móvil', () => {
    const toggleSpy = vi.fn();
    component.toggleSidebar.subscribe(toggleSpy);

    component.toggleSidebar.emit();
    expect(toggleSpy).toHaveBeenCalled();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Siderbar } from './siderbar';
import { StoreService } from '../../../../core/services/store.service';
import { OrderService } from '../../../../core/services/order.service';
import { CapacityService } from '../../../../core/services/capacity.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { AuthService } from '../../../../core/services/auth-service';
import { getOperatingDateLima } from '../../../../core/utils/date.utils';
import { of } from 'rxjs';

describe('Siderbar', () => {
  let component: Siderbar;
  let fixture: ComponentFixture<Siderbar>;
  let orderService: OrderService;
  let storeService: StoreService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Siderbar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        StoreService,
        OrderService,
        CapacityService,
        ProfileService,
        AuthService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Siderbar);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService);
    storeService = TestBed.inject(StoreService);

    storeService.currentStore.set({
      id: 'store-1',
      merchantUserId: 'usr-1',
      name: 'Mi Panadería',
      slug: 'mi-panaderia',
      pickupEnabled: true,
      deliveryEnabled: true,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular activeOrdersCount sumando exclusivamente confirmed + inPreparation + ready', () => {
    // 12 confirmed + 6 inPrep + 4 ready = 22 activos
    // Excluye outForDelivery (2), completed (20), cancelled (4)
    orderService.todayOrderCounts.set({
      all: 48,
      confirmed: 12,
      inPreparation: 6,
      ready: 4,
      outForDelivery: 2,
      completed: 20,
      cancelled: 4,
    });
    fixture.detectChanges();

    expect(component.activeOrdersCount()).toBe(22);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('22 hoy');
  });

  it('no debe incluir outForDelivery, completed ni cancelled en el badge', () => {
    orderService.todayOrderCounts.set({
      all: 30,
      confirmed: 0,
      inPreparation: 0,
      ready: 0,
      outForDelivery: 5,
      completed: 20,
      cancelled: 5,
    });
    fixture.detectChanges();

    expect(component.activeOrdersCount()).toBe(0);
    const ordersLink = (fixture.nativeElement as HTMLElement).querySelector(
      'a[routerLink="/orders"]',
    );
    expect(ordersLink?.textContent).not.toContain('hoy');
    expect(ordersLink?.textContent).not.toContain('0 hoy');
  });

  it('debe mostrar estado de carga mientras los contadores están en vuelo y no hay datos previos', () => {
    orderService.todayOrderCounts.set(null);
    orderService.isTodayCountsLoading.set(true);
    fixture.detectChanges();

    const ordersLink = (fixture.nativeElement as HTMLElement).querySelector(
      'a[routerLink="/orders"]',
    );
    expect(ordersLink?.textContent).toContain('...');
  });

  it('debe manejar error sin mostrar cifras ficticias ni asumir cero como badge', () => {
    orderService.todayOrderCounts.set(null);
    orderService.isTodayCountsLoading.set(false);
    orderService.todayCountsError.set('Error 500 al consultar contadores');
    fixture.detectChanges();

    const ordersLink = (fixture.nativeElement as HTMLElement).querySelector(
      'a[routerLink="/orders"]',
    );
    expect(ordersLink?.textContent).not.toContain('hoy');
    expect(ordersLink?.textContent).not.toContain('0 hoy');
  });

  it('debe actualizar los contadores al cambiar de tienda activa', () => {
    const loadSpy = vi.spyOn(orderService, 'loadTodayOrderCounts').mockReturnValue(of(null));

    // Simular cambio de tienda en storeService
    storeService.currentStore.set({
      id: 'store-2',
      merchantUserId: 'usr-1',
      name: 'Segunda Sucursal',
      slug: 'segunda-sucursal',
      pickupEnabled: true,
      deliveryEnabled: true,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    orderService.resetCounts();
    expect(orderService.todayOrderCounts()).toBeNull();
    expect(component.activeOrdersCount()).toBe(0);

    orderService.loadTodayOrderCounts('store-2');
    expect(loadSpy).toHaveBeenCalledWith('store-2');
  });

  it('debe advertir en el badge si existen datos previos pero la actualización falló o está desactualizada', () => {
    orderService.todayOrderCounts.set({
      all: 10,
      confirmed: 3,
      inPreparation: 2,
      ready: 1,
      outForDelivery: 0,
      completed: 4,
      cancelled: 0,
    });
    orderService.todayOperatingDate.set('2026-11-14');
    orderService.todayCountsError.set('Error de red');
    fixture.detectChanges();

    expect(component.activeOrdersCount()).toBe(6);
    expect(orderService.isTodayOutdated()).toBe(true);

    const ordersLink = (fixture.nativeElement as HTMLElement).querySelector(
      'a[routerLink="/orders"]',
    );
    expect(ordersLink?.textContent).toContain('6 hoy (desactualizado)');
  });

  it('debe reflejar ocupación real de capacidad en el módulo del sidebar', () => {
    const capacityService = TestBed.inject(CapacityService);
    capacityService.dailyOccupancy.set({
      storeId: 'store-1',
      date: getOperatingDateLima(),
      isClosed: false,
      effectiveCapacity: 20,
      occupiedCapacity: 12,
      availableCapacity: 8,
      reservedCapacity: 4,
      committedCapacity: 8,
      occupancyPercentage: 60,
      slots: [],
    });

    fixture.detectChanges();

    expect(component.capacityOccupied()).toBe(12);
    expect(component.capacityTotal()).toBe(20);
    expect(component.availableCapacity()).toBe(8);
    expect(component.isOverCapacity()).toBe(false);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('12/20');
    expect(compiled.textContent).toContain('Quedan 8 cupos');
  });

  it('debe reflejar sobrecapacidad en el módulo del sidebar y advertir el exceso', () => {
    const capacityService = TestBed.inject(CapacityService);
    capacityService.dailyOccupancy.set({
      storeId: 'store-1',
      date: getOperatingDateLima(),
      isClosed: false,
      effectiveCapacity: 10,
      occupiedCapacity: 16,
      availableCapacity: 0,
      reservedCapacity: 6,
      committedCapacity: 10,
      occupancyPercentage: 160,
      slots: [],
    });

    fixture.detectChanges();

    expect(component.capacityOccupied()).toBe(16);
    expect(component.capacityTotal()).toBe(10);
    expect(component.isOverCapacity()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('16/10');
    expect(compiled.textContent).toContain('Sobrecapacidad (+6)');
  });
});

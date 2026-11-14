import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardKpis } from './dashboard-kpis';
import { CapacityService } from '../../../../core/services/capacity.service';
import { OrderService } from '../../../../core/services/order.service';

describe('DashboardKpis', () => {
  let component: DashboardKpis;
  let fixture: ComponentFixture<DashboardKpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardKpis],
      providers: [provideHttpClient(), provideHttpClientTesting(), CapacityService],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardKpis);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular la capacidad disponible y bloques correctamente', () => {
    expect(component.capacityTotal()).toBeGreaterThanOrEqual(0);
    expect(component.availableCapacity()).toBeNull();
    expect(component.metrics().capacityOccupied).toBeNull();
    expect(component.capacityBlocks().length).toBe(10);
    expect(component.shiftSchedule()).toBeDefined();

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Medición de ocupación en desarrollo');
  });

  it('debe reflejar estado de cierre cuando hay una excepción con capacidad 0 hoy', () => {
    const capacityService = TestBed.inject(CapacityService);
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    capacityService.exceptions.set([
      {
        id: 'exc-closed',
        storeId: 'store-1',
        serviceDate: todayStr,
        startTime: null,
        endTime: null,
        exceptionCapacity: 0,
        reason: 'Feriado Nacional',
        status: 'ACTIVE',
        createdAt: '2026-09-22T10:00:00Z',
        updatedAt: '2026-09-22T10:00:00Z',
      },
    ]);

    fixture.detectChanges();

    expect(component.isClosedToday()).toBe(true);
    expect(component.capacityTotal()).toBe(0);
    expect(component.shiftSchedule()).toContain('Cerrado: Feriado Nacional');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Cerrado');
  });

  it('debe reflejar capacidad especial cuando hay una excepción con cupos hoy', () => {
    const capacityService = TestBed.inject(CapacityService);
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    capacityService.exceptions.set([
      {
        id: 'exc-special',
        storeId: 'store-1',
        serviceDate: todayStr,
        startTime: null,
        endTime: null,
        exceptionCapacity: 40,
        reason: 'Campaña Cyber',
        status: 'ACTIVE',
        createdAt: '2026-09-22T10:00:00Z',
        updatedAt: '2026-09-22T10:00:00Z',
      },
    ]);

    fixture.detectChanges();

    expect(component.isClosedToday()).toBe(false);
    expect(component.capacityTotal()).toBe(40);
    expect(component.shiftSchedule()).toContain('Fecha especial: Campaña Cyber');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Especial');
  });

  it('debe reflejar los contadores de la fecha operativa diaria en las métricas', () => {
    const orderService = TestBed.inject(OrderService);
    orderService.todayOrderCounts.set({
      all: 15,
      confirmed: 5,
      inPreparation: 4,
      ready: 3,
      outForDelivery: 1,
      completed: 2,
      cancelled: 0,
    });
    fixture.detectChanges();

    expect(component.metrics().totalOrdersToday).toBe(15);
    expect(component.metrics().confirmedOrders).toBe(5);
    expect(component.metrics().inPrepOrders).toBe(4);
    expect(component.metrics().readyOrders).toBe(3);
    expect(component.metrics().completedOrders).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('15 pedidos');
    expect(compiled.textContent).toContain('5 confirmados');
    expect(compiled.textContent).toContain('4 en preparación');
    expect(compiled.textContent).toContain('3 listos');
    expect(compiled.textContent).toContain('2 completados');
  });

  it('no debe confundir cantidad de pedidos con ingresos ni inventar métricas monetarias', () => {
    expect((component.metrics() as any).revenue).toBeUndefined();
    expect((component.metrics() as any).totalSales).toBeUndefined();
    expect(component.metrics().capacityOccupied).toBeNull();
  });

  it('debe mostrar estado de carga y error sin inventar cifras ficticias ni ceros', () => {
    const orderService = TestBed.inject(OrderService);
    orderService.todayOrderCounts.set(null);
    orderService.isTodayCountsLoading.set(true);
    fixture.detectChanges();

    let compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Cargando...');

    orderService.isTodayCountsLoading.set(false);
    orderService.todayCountsError.set('Error al consultar contadores del día');
    fixture.detectChanges();

    compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Error al consultar contadores del día');
    expect(compiled.textContent).toContain('Desactualizado');
  });

  it('debe marcar estadísticas como desactualizadas si cambia la fecha operativa de Lima', () => {
    const orderService = TestBed.inject(OrderService);
    orderService.todayOrderCounts.set({
      all: 10,
      confirmed: 2,
      inPreparation: 3,
      ready: 1,
      outForDelivery: 0,
      completed: 4,
      cancelled: 0,
    });
    // Simular que los datos corresponden al día anterior
    orderService.todayOperatingDate.set('2026-11-13');
    orderService.todayCountsError.set(null);
    fixture.detectChanges();

    expect(component.isTodayOutdated()).toBe(true);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Desactualizado');
  });

  it('debe renderizar Ritmo Operativo como no disponible sin métricas ni metas ficticias', () => {
    expect(component.metrics().operatingRhythmMin).toBeNull();
    expect(component.metrics().targetRhythmMin).toBeNull();

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Ritmo Operativo');
    expect(compiled.textContent).toContain('No disponible');
    expect(compiled.textContent).toContain('Sin métrica calculada');
    expect(compiled.textContent).not.toContain('25 min');
    expect(compiled.textContent).not.toContain('Operación ágil');
  });

  it('debe advertir visualmente que se muestran datos previos si falla un refresco diario', () => {
    const orderService = TestBed.inject(OrderService);
    orderService.todayOrderCounts.set({
      all: 8,
      confirmed: 3,
      inPreparation: 2,
      ready: 1,
      outForDelivery: 0,
      completed: 2,
      cancelled: 0,
    });
    orderService.todayOperatingDate.set('2026-11-14');
    orderService.todayCountsError.set('Error de red al actualizar');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('8 pedidos');
    expect(compiled.textContent).toContain('No se pudo actualizar: mostrando datos previos.');
    expect(compiled.textContent).toContain('Desactualizado');
  });
});

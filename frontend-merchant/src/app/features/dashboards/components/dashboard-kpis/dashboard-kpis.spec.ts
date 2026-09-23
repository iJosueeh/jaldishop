import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardKpis } from './dashboard-kpis';
import { CapacityService } from '../../../../core/services/capacity.service';

describe('DashboardKpis', () => {
  let component: DashboardKpis;
  let fixture: ComponentFixture<DashboardKpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardKpis],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CapacityService,
      ],
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
    expect(component.availableCapacity()).toBeGreaterThanOrEqual(0);
    expect(component.capacityBlocks().length).toBe(10);
    expect(component.shiftSchedule()).toBeDefined();
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
});


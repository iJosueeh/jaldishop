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
});


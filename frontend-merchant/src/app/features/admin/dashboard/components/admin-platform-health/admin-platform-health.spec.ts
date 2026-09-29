import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminPlatformHealth } from './admin-platform-health';
import { AdminDashboardMetrics } from '../../../../../core/models/admin.models';

describe('AdminPlatformHealth', () => {
  let component: AdminPlatformHealth;
  let fixture: ComponentFixture<AdminPlatformHealth>;

  const mockMetricsWithIssues: AdminDashboardMetrics = {
    totalUsers: 10,
    totalMerchants: 4,
    totalCustomers: 5,
    totalAdmins: 1,
    activeUsers: 8,
    suspendedUsers: 2,
    totalStores: 5,
    activeStores: 4,
    suspendedStores: 1,
  };

  const mockMetricsPerfect: AdminDashboardMetrics = {
    totalUsers: 10,
    totalMerchants: 4,
    totalCustomers: 5,
    totalAdmins: 1,
    activeUsers: 10,
    suspendedUsers: 0,
    totalStores: 5,
    activeStores: 5,
    suspendedStores: 0,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPlatformHealth],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPlatformHealth);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('metrics', mockMetricsWithIssues);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular ratios y detectar incidencias correctamente', () => {
    expect(component.activeRatio()).toBe(80);
    expect(component.suspendedRatio()).toBe(20);
    expect(component.hasIssues()).toBe(true);
  });

  it('debe indicar 100% de operatividad y cero incidencias cuando todo está activo', () => {
    fixture.componentRef.setInput('metrics', mockMetricsPerfect);
    fixture.detectChanges();

    expect(component.activeRatio()).toBe(100);
    expect(component.suspendedRatio()).toBe(0);
    expect(component.hasIssues()).toBe(false);
  });

  it('debe emitir filterSuspended al hacer clic en revisar comercios pausados', () => {
    const spy = vi.spyOn(component.filterSuspended, 'emit');
    component.filterSuspended.emit();
    expect(spy).toHaveBeenCalled();
  });
});

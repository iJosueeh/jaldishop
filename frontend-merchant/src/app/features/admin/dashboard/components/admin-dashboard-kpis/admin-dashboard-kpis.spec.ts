import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminDashboardKpis } from './admin-dashboard-kpis';

describe('AdminDashboardKpis', () => {
  let component: AdminDashboardKpis;
  let fixture: ComponentFixture<AdminDashboardKpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardKpis],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardKpis);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('metrics', {
      totalUsers: 15,
      totalMerchants: 5,
      totalCustomers: 8,
      totalAdmins: 2,
      activeUsers: 14,
      suspendedUsers: 1,
      totalStores: 5,
      activeStores: 4,
      suspendedStores: 1,
    });
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar las métricas', () => {
    expect(component).toBeTruthy();
    expect(component.metrics().totalUsers).toBe(15);
    expect(component.metrics().activeStores).toBe(4);
  });
});

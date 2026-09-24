import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomersKpis } from './customers-kpis';
import { CustomerKpis } from '../../../../core/models/customer.models';

describe('CustomersKpis', () => {
  let component: CustomersKpis;
  let fixture: ComponentFixture<CustomersKpis>;

  const mockKpis: CustomerKpis = {
    totalCustomers: 45,
    repeatCustomersCount: 18,
    repeatPercentage: 40,
    averageTicketAmount: 32.5,
    totalSpentOverall: 1462.5,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomersKpis],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersKpis);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('kpis', mockKpis);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render KPI metrics correctly', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('45');
    expect(compiled.textContent).toContain('40%');
    expect(compiled.textContent).toContain('18 con ≥2 compras');
    expect(compiled.textContent).toContain('32.50');
  });
});

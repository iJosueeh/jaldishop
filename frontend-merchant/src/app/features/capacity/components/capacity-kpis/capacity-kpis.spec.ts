import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityKpis } from './capacity-kpis';

describe('CapacityKpis', () => {
  let component: CapacityKpis;
  let fixture: ComponentFixture<CapacityKpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityKpis],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityKpis);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('dayLabel', 'Sábado');
    fixture.componentRef.setInput('totalCapacity', 20);
    fixture.componentRef.setInput('totalSlotsCount', 2);
    fixture.componentRef.setInput('activeSlotsCount', 1);
    await fixture.whenStable();
  });

  it('should create and display input metrics', () => {
    expect(component).toBeTruthy();
    expect(component.dayLabel()).toBe('Sábado');
    expect(component.totalCapacity()).toBe(20);
    expect(component.totalSlotsCount()).toBe(2);
    expect(component.activeSlotsCount()).toBe(1);
  });
});

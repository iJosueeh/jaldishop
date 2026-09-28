import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersKpis } from './orders-kpis';

describe('OrdersKpis', () => {
  let component: OrdersKpis;
  let fixture: ComponentFixture<OrdersKpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersKpis],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersKpis);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente con valores por defecto y sin ritmo promedio', () => {
    expect(component).toBeTruthy();
    expect(component.availableCapacity()).toBe(0);
    expect(component.capacityBlocks().length).toBe(10);
    expect(component.hasAveragePace()).toBe(false);
  });

  it('debe calcular capacidad disponible y bloques cuando se reciben inputs', () => {
    fixture.componentRef.setInput('capacityTotal', 10);
    fixture.componentRef.setInput('capacityOccupied', 8);
    fixture.componentRef.setInput('averagePaceMin', 18);
    fixture.detectChanges();

    expect(component.availableCapacity()).toBe(2);
    expect(component.capacityBlocks().length).toBe(10);
    expect(component.hasAveragePace()).toBe(true);
  });

  it('debe detectar ausencia de promedio cuando averagePaceMin es null o 0', () => {
    fixture.componentRef.setInput('averagePaceMin', null);
    fixture.detectChanges();
    expect(component.hasAveragePace()).toBe(false);

    fixture.componentRef.setInput('averagePaceMin', 0);
    fixture.detectChanges();
    expect(component.hasAveragePace()).toBe(false);
  });
});

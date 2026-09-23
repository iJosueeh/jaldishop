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

  it('should create and calculate available capacity and blocks', () => {
    expect(component).toBeTruthy();
    expect(component.availableCapacity()).toBe(2);
    expect(component.capacityBlocks().length).toBe(10);
  });
});

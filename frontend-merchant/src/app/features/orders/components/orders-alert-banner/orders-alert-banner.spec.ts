import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersAlertBanner } from './orders-alert-banner';

describe('OrdersAlertBanner', () => {
  let component: OrdersAlertBanner;
  let fixture: ComponentFixture<OrdersAlertBanner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersAlertBanner],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersAlertBanner);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir viewUrgent al hacer clic en Ver urgentes', () => {
    let emitted = false;
    component.viewUrgent.subscribe(() => {
      emitted = true;
    });

    const btn = fixture.nativeElement.querySelector('button');
    btn.click();

    expect(emitted).toBe(true);
  });
});

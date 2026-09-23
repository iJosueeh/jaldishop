import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersHeader } from './orders-header';

describe('OrdersHeader', () => {
  let component: OrdersHeader;
  let fixture: ComponentFixture<OrdersHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir newOrder al hacer clic en Registrar Pedido', () => {
    let emitted = false;
    component.newOrder.subscribe(() => {
      emitted = true;
    });

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[2].click();

    expect(emitted).toBe(true);
  });
});

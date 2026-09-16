import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PriorityOrders } from './priority-orders';

describe('PriorityOrders', () => {
  let component: PriorityOrders;
  let fixture: ComponentFixture<PriorityOrders>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriorityOrders],
      providers: [
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PriorityOrders);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

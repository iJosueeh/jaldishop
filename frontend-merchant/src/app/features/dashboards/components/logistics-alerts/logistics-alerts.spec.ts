import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LogisticsAlerts } from './logistics-alerts';
import { OrderService } from '../../../../core/services/order.service';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';

describe('LogisticsAlerts', () => {
  let component: LogisticsAlerts;
  let fixture: ComponentFixture<LogisticsAlerts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogisticsAlerts],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        OrderService,
        ProductService,
        StoreService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogisticsAlerts);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe inicializarse con arreglos reactivos', () => {
    expect(component.upcomingDeliveries()).toBeDefined();
    expect(component.stockAlerts()).toBeDefined();
  });
});

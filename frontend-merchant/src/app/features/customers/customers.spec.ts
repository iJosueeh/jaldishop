import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Customers } from './customers';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { StoreCustomer } from '../../core/models/customer.models';
import { environment } from '../../../environments/environment';
import { vi } from 'vitest';

describe('Customers', () => {
  let component: Customers;
  let fixture: ComponentFixture<Customers>;
  let httpMock: HttpTestingController;

  const mockCustomers: StoreCustomer[] = [
    {
      userId: 'c-1',
      firstName: 'Ana',
      lastName: 'García',
      email: 'ana@example.com',
      phone: '999111222',
      customerSince: '2026-01-01T00:00:00Z',
      ordersCount: 4,
      totalSpentAmount: 180.0,
      lastOrderAt: '2026-03-22T10:00:00Z',
    },
    {
      userId: 'c-2',
      firstName: 'Bruno',
      lastName: 'Díaz',
      email: 'bruno@example.com',
      phone: '999333444',
      customerSince: '2026-01-15T00:00:00Z',
      ordersCount: 2,
      totalSpentAmount: 60.0,
      lastOrderAt: '2026-03-21T10:00:00Z',
    },
    {
      userId: 'c-3',
      firstName: 'Carla',
      lastName: 'López',
      email: 'carla@example.com',
      phone: null,
      customerSince: '2026-02-01T00:00:00Z',
      ordersCount: 1,
      totalSpentAmount: 25.0,
      lastOrderAt: '2026-03-20T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Customers],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CustomerService,
        ToastService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Customers);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create and load customer data on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/merchant/customers`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomers);

    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(3);
    expect(component.vipCount()).toBe(1);
    expect(component.frequentCount()).toBe(1);
    expect(component.newCount()).toBe(1);
  });

  it('should filter customers by segment tab and search query', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/merchant/customers`);
    req.flush(mockCustomers);
    fixture.detectChanges();

    component.onTabChange('vip');
    expect(component.filteredCustomers().length).toBe(1);
    expect(component.filteredCustomers()[0].firstName).toBe('Ana');

    component.onTabChange('all');
    component.onSearchChange('bruno');
    expect(component.filteredCustomers().length).toBe(1);
    expect(component.filteredCustomers()[0].firstName).toBe('Bruno');
  });

  it('should handle pagination changes', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/merchant/customers`);
    req.flush(mockCustomers);
    fixture.detectChanges();

    expect(component.currentPage()).toBe(1);
    component.onPageChange(2);
    expect(component.currentPage()).toBe(2);
  });

  it('should trigger CSV export and WhatsApp action', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/merchant/customers`);
    req.flush(mockCustomers);
    fixture.detectChanges();

    const toastService = TestBed.inject(ToastService);
    const successSpy = vi.spyOn(toastService, 'success');
    const infoSpy = vi.spyOn(toastService, 'info');

    component.onExportCsv();
    expect(successSpy).toHaveBeenCalledWith('Lista de clientes exportada en CSV correctamente.');

    component.onOpenWhatsApp(mockCustomers[0]);
    expect(infoSpy).toHaveBeenCalledWith('Iniciando chat con Ana...');
  });
});

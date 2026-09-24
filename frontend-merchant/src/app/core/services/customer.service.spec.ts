import { TestBed } from '@angular/core/testing';
import { CustomerService } from './customer.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { StoreCustomer } from '../models/customer.models';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('CustomerService', () => {
  let service: CustomerService;
  let httpTesting: HttpTestingController;

  const mockCustomers: StoreCustomer[] = [
    {
      userId: 'u1',
      firstName: 'Mariana',
      lastName: 'Torres',
      email: 'mariana@example.com',
      phone: '+51987654321',
      customerSince: '2026-09-01T10:00:00Z',
      ordersCount: 5,
      totalSpentAmount: 250.0,
      lastOrderAt: '2026-09-20T12:00:00Z',
    },
    {
      userId: 'u2',
      firstName: 'Carlos',
      lastName: 'Valenzuela',
      email: 'carlos@example.com',
      phone: '+51976543210',
      customerSince: '2026-09-10T10:00:00Z',
      ordersCount: 2,
      totalSpentAmount: 90.0,
      lastOrderAt: '2026-09-18T15:00:00Z',
    },
    {
      userId: 'u3',
      firstName: 'Valentina',
      lastName: 'Morales',
      email: 'valeria@example.com',
      phone: null,
      customerSince: '2026-09-22T10:00:00Z',
      ordersCount: 1,
      totalSpentAmount: 40.0,
      lastOrderAt: '2026-09-22T10:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CustomerService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CustomerService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
    expect(service.customers().length).toBe(0);
    expect(service.isLoading()).toBe(false);
  });

  it('debe cargar clientes desde la API y actualizar signals', () => {
    service.loadCustomers().subscribe((data) => {
      expect(data.length).toBe(3);
      expect(service.customers().length).toBe(3);
      expect(service.isLoading()).toBe(false);
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/merchant/customers`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomers);
  });

  it('debe aplicar la estrategia Cache First y permitir forzar refresco', () => {
    // 1. Primera carga: hace HTTP request
    service.loadCustomers().subscribe((data) => {
      expect(data.length).toBe(3);
    });
    const req1 = httpTesting.expectOne(`${environment.apiUrl}/merchant/customers`);
    req1.flush(mockCustomers);

    // 2. Segunda carga sin forceRefresh: responde desde caché síncrona/reactiva sin nuevo HTTP
    service.loadCustomers(false).subscribe((data) => {
      expect(data.length).toBe(3);
      expect(data[0].firstName).toBe('Mariana');
    });
    httpTesting.expectNone(`${environment.apiUrl}/merchant/customers`);

    // 3. Tercera carga con forceRefresh = true: hace nueva petición HTTP
    service.loadCustomers(true).subscribe((data) => {
      expect(data.length).toBe(1);
    });
    const req2 = httpTesting.expectOne(`${environment.apiUrl}/merchant/customers`);
    req2.flush([mockCustomers[0]]);
  });

  it('debe limpiar caché al llamar clearCache()', () => {
    service.customers.set(mockCustomers);
    service.clearCache();

    expect(service.customers().length).toBe(0);
    expect(service.searchQuery()).toBe('');
    expect(service.activeFilter()).toBe('all');
  });

  it('debe enviar query param cuando se solicita búsqueda', () => {
    service.loadCustomers(true, 'Mariana').subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/merchant/customers?query=Mariana`);
    expect(req.request.method).toBe('GET');
    req.flush([mockCustomers[0]]);
  });

  it('debe calcular KPIs agregados correctamente', () => {
    service.customers.set(mockCustomers);

    const kpis = service.kpis();
    expect(kpis.totalCustomers).toBe(3);
    // 2 clientes con >= 2 pedidos (Mariana con 5, Carlos con 2) -> 2/3 = 67%
    expect(kpis.repeatCustomersCount).toBe(2);
    expect(kpis.repeatPercentage).toBe(67);
    expect(kpis.totalSpentOverall).toBe(380.0);
    // totalOrders = 5 + 2 + 1 = 8. averageTicket = 380 / 8 = 47.5
    expect(kpis.averageTicketAmount).toBe(47.5);
  });

  it('debe filtrar clientes por pestaña de segmento y texto reactivamente', () => {
    service.customers.set(mockCustomers);

    // Filtro VIP (>= 3 compras)
    service.setFilter('vip');
    expect(service.filteredCustomers().length).toBe(1);
    expect(service.filteredCustomers()[0].firstName).toBe('Mariana');

    // Filtro Frecuente (2 compras)
    service.setFilter('frequent');
    expect(service.filteredCustomers().length).toBe(1);
    expect(service.filteredCustomers()[0].firstName).toBe('Carlos');

    // Filtro Nuevos (1 compra)
    service.setFilter('new');
    expect(service.filteredCustomers().length).toBe(1);
    expect(service.filteredCustomers()[0].firstName).toBe('Valentina');

    // Filtro búsqueda por texto
    service.setFilter('all');
    service.setSearchQuery('Valenzuela');
    expect(service.filteredCustomers().length).toBe(1);
    expect(service.filteredCustomers()[0].lastName).toBe('Valenzuela');
  });
});

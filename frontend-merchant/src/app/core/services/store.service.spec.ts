import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { StoreService } from './store.service';
import { StoreResponse } from '../models/store.models';
import { environment } from '../../../environments/environment';

describe('StoreService', () => {
  let service: StoreService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/stores`;

  const mockStore: StoreResponse = {
    id: 'store-1',
    merchantUserId: 'user-1',
    name: 'Panadería Central',
    slug: 'panaderia-central',
    description: 'La mejor panadería artesanal',
    status: 'ACTIVE',
    contactPhone: '999888777',
    address: 'Av. Siempre Viva 123',
    pickupEnabled: true,
    deliveryEnabled: true,
    taxApplies: false,
    createdAt: '2026-09-19T10:00:00Z',
    updatedAt: '2026-09-19T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StoreService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StoreService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente con estado inicial nulo', () => {
    expect(service).toBeTruthy();
    expect(service.currentStore()).toBeNull();
    expect(service.hasStore()).toBe(false);
    expect(service.storeName()).toBe('Mi tienda');
    expect(service.storeStatus()).toBe('INACTIVE');
  });

  it('debe obtener tienda por HTTP y actualizar signals', () => {
    service.getMyStore().subscribe((store) => {
      expect(store).toEqual(mockStore);
      expect(service.currentStore()).toEqual(mockStore);
      expect(service.hasStore()).toBe(true);
      expect(service.storeName()).toBe('Panadería Central');
    });

    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStore);
  });

  it('debe retornar tienda desde memoria en llamadas posteriores sin repetir HTTP', () => {
    service.getMyStore().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockStore);

    let cached: StoreResponse | null = null;
    service.getMyStore(false).subscribe((res) => {
      cached = res;
    });

    expect(cached).toEqual(mockStore);
    httpTesting.expectNone(`${baseUrl}/me`);
  });

  it('debe forzar llamada HTTP cuando forceRefresh es true', () => {
    service.getMyStore().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockStore);

    service.getMyStore(true).subscribe();
    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStore);
  });

  it('debe manejar error 404 guardando null y marcando isLoaded', () => {
    service.getMyStore().subscribe((res) => {
      expect(res).toBeNull();
      expect(service.currentStore()).toBeNull();
    });

    const req = httpTesting.expectOne(`${baseUrl}/me`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });

    // Siguiente llamada debe devolver null desde memoria
    service.getMyStore(false).subscribe((res) => {
      expect(res).toBeNull();
    });
    httpTesting.expectNone(`${baseUrl}/me`);
  });

  it('debe actualizar memoria tras updateStore()', () => {
    const updated = { ...mockStore, name: 'Panadería y Pastelería Central' };
    service.updateStore({ name: 'Panadería y Pastelería Central', pickupEnabled: true, deliveryEnabled: true, taxApplies: false }).subscribe((res) => {
      expect(res.name).toBe('Panadería y Pastelería Central');
      expect(service.storeName()).toBe('Panadería y Pastelería Central');
    });

    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('PUT');
    req.flush(updated);
  });

  it('debe limpiar memoria tras clearStore()', () => {
    service.getMyStore().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockStore);

    service.clearStore();
    expect(service.currentStore()).toBeNull();
    expect(service.hasStore()).toBe(false);
  });
});

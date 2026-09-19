import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CapacityService } from './capacity.service';
import {
  CapacityConfiguration,
  CreateCapacityConfigRequest,
  UpdateCapacityConfigRequest,
} from '../models/capacity.models';
import { environment } from '../../../environments/environment';

describe('CapacityService', () => {
  let service: CapacityService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/capacity-configurations`;

  const mockConfigs: CapacityConfiguration[] = [
    {
      id: 'cfg-1',
      storeId: 'store-1',
      dayOfWeek: 6, // Sábado
      startTime: '09:00:00',
      endTime: '14:00:00',
      maxCapacity: 10,
      status: 'ACTIVE',
      createdAt: '2026-09-19T10:00:00Z',
      updatedAt: '2026-09-19T10:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CapacityService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CapacityService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente con estado inicial vacío', () => {
    expect(service).toBeTruthy();
    expect(service.configurations()).toEqual([]);
    expect(service.isLoading()).toBe(false);
  });

  it('debe obtener configuraciones por HTTP y guardar en memoria', () => {
    service.getConfigurations().subscribe((configs) => {
      expect(configs).toEqual(mockConfigs);
      expect(service.configurations()).toEqual(mockConfigs);
      expect(service.isLoading()).toBe(false);
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockConfigs);
  });

  it('debe responder desde memoria en llamadas posteriores sin repetir petición HTTP', () => {
    // 1. Primera llamada HTTP
    service.getConfigurations().subscribe();
    const req1 = httpTesting.expectOne(baseUrl);
    req1.flush(mockConfigs);

    // 2. Segunda llamada (debe salir de memoria)
    let secondCallResult: CapacityConfiguration[] = [];
    service.getConfigurations(false).subscribe((configs) => {
      secondCallResult = configs;
    });

    expect(secondCallResult).toEqual(mockConfigs);
    // No debe haber otra petición HTTP pendiente
    httpTesting.expectNone(baseUrl);
  });

  it('debe forzar petición HTTP cuando forceRefresh es true', () => {
    // 1. Primera llamada
    service.getConfigurations().subscribe();
    httpTesting.expectOne(baseUrl).flush(mockConfigs);

    // 2. Segunda llamada con forceRefresh = true
    service.getConfigurations(true).subscribe();
    const req2 = httpTesting.expectOne(baseUrl);
    expect(req2.request.method).toBe('GET');
    req2.flush(mockConfigs);
  });

  it('debe agregar nueva configuración a la memoria tras createConfiguration()', () => {
    const newRequest: CreateCapacityConfigRequest = {
      dayOfWeek: 6,
      startTime: '14:00:00',
      endTime: '18:00:00',
      maxCapacity: 15,
    };
    const createdConfig: CapacityConfiguration = {
      id: 'cfg-2',
      storeId: 'store-1',
      ...newRequest,
      status: 'ACTIVE',
      createdAt: '2026-09-19T11:00:00Z',
      updatedAt: '2026-09-19T11:00:00Z',
    };

    service.createConfiguration(newRequest).subscribe((res) => {
      expect(res).toEqual(createdConfig);
      expect(service.configurations()).toContainEqual(createdConfig);
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    req.flush(createdConfig);
  });

  it('debe actualizar configuración en memoria tras updateConfiguration()', () => {
    service.createConfiguration({
      dayOfWeek: 6,
      startTime: '09:00:00',
      endTime: '14:00:00',
      maxCapacity: 10,
    }).subscribe();
    httpTesting.expectOne(baseUrl).flush(mockConfigs[0]);

    const updateRequest: UpdateCapacityConfigRequest = {
      dayOfWeek: 6,
      startTime: '09:00:00',
      endTime: '15:00:00',
      maxCapacity: 20,
    };
    const updatedConfig: CapacityConfiguration = {
      ...mockConfigs[0],
      endTime: '15:00:00',
      maxCapacity: 20,
    };

    service.updateConfiguration('cfg-1', updateRequest).subscribe((res) => {
      expect(res.maxCapacity).toBe(20);
      expect(service.configurations()[0].maxCapacity).toBe(20);
    });

    const req = httpTesting.expectOne(`${baseUrl}/cfg-1`);
    expect(req.request.method).toBe('PUT');
    req.flush(updatedConfig);
  });

  it('debe pausar franja y actualizar su estado en memoria', () => {
    service.createConfiguration({
      dayOfWeek: 6,
      startTime: '09:00:00',
      endTime: '14:00:00',
      maxCapacity: 10,
    }).subscribe();
    httpTesting.expectOne(baseUrl).flush(mockConfigs[0]);

    const deactivated: CapacityConfiguration = {
      ...mockConfigs[0],
      status: 'INACTIVE',
    };

    service.deactivateConfiguration('cfg-1').subscribe((res) => {
      expect(res.status).toBe('INACTIVE');
      expect(service.configurations()[0].status).toBe('INACTIVE');
    });

    const req = httpTesting.expectOne(`${baseUrl}/cfg-1/deactivate`);
    expect(req.request.method).toBe('PATCH');
    req.flush(deactivated);
  });

  it('debe limpiar la memoria tras clearCache()', () => {
    service.getConfigurations().subscribe();
    httpTesting.expectOne(baseUrl).flush(mockConfigs);
    expect(service.configurations().length).toBe(1);

    service.clearCache();
    expect(service.configurations()).toEqual([]);
  });
});

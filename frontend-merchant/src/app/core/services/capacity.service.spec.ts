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
    expect(service.exceptions()).toEqual([]);
  });

  describe('CapacityExceptions', () => {
    const exceptionsUrl = `${environment.apiUrl}/capacity-exceptions`;

    const mockExceptions = [
      {
        id: 'exc-1',
        storeId: 'store-1',
        serviceDate: '2026-12-25',
        startTime: '09:00:00',
        endTime: '14:00:00',
        exceptionCapacity: 0,
        reason: 'Cerrado por Navidad',
        status: 'ACTIVE' as const,
        createdAt: '2026-09-22T10:00:00Z',
        updatedAt: '2026-09-22T10:00:00Z',
      },
    ];

    it('debe obtener excepciones por HTTP y guardar en memoria', () => {
      service.getExceptions().subscribe((res) => {
        expect(res).toEqual(mockExceptions);
        expect(service.exceptions()).toEqual(mockExceptions);
        expect(service.isLoadingExceptions()).toBe(false);
      });

      const req = httpTesting.expectOne(exceptionsUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockExceptions);
    });

    it('debe agregar nueva excepción en memoria tras createException()', () => {
      const newRequest = {
        serviceDate: '2026-12-31',
        startTime: '10:00:00',
        endTime: '16:00:00',
        exceptionCapacity: 25,
        reason: 'Cena de Año Nuevo',
      };
      const createdException = {
        id: 'exc-2',
        storeId: 'store-1',
        ...newRequest,
        status: 'ACTIVE' as const,
        createdAt: '2026-09-22T10:00:00Z',
        updatedAt: '2026-09-22T10:00:00Z',
      };

      service.createException(newRequest).subscribe((res) => {
        expect(res).toEqual(createdException);
        expect(service.exceptions()).toContainEqual(createdException);
      });

      const req = httpTesting.expectOne(exceptionsUrl);
      expect(req.request.method).toBe('POST');
      req.flush(createdException);
    });

    it('debe actualizar excepción en memoria tras updateException()', () => {
      service.createException({
        serviceDate: '2026-12-25',
        startTime: '09:00:00',
        endTime: '14:00:00',
        exceptionCapacity: 0,
      }).subscribe();
      httpTesting.expectOne(exceptionsUrl).flush(mockExceptions[0]);

      const updated = {
        ...mockExceptions[0],
        exceptionCapacity: 5,
        reason: 'Horario especial reducido',
      };

      service.updateException('exc-1', {
        serviceDate: '2026-12-25',
        startTime: '09:00:00',
        endTime: '14:00:00',
        exceptionCapacity: 5,
        reason: 'Horario especial reducido',
      }).subscribe((res) => {
        expect(res.exceptionCapacity).toBe(5);
        expect(service.exceptions()[0].exceptionCapacity).toBe(5);
      });

      const req = httpTesting.expectOne(`${exceptionsUrl}/exc-1`);
      expect(req.request.method).toBe('PUT');
      req.flush(updated);
    });

    it('debe activar y desactivar excepción actualizando el estado en memoria', () => {
      service.createException({
        serviceDate: '2026-12-25',
        startTime: '09:00:00',
        endTime: '14:00:00',
        exceptionCapacity: 0,
      }).subscribe();
      httpTesting.expectOne(exceptionsUrl).flush(mockExceptions[0]);

      // Deactivate
      const deactivated = { ...mockExceptions[0], status: 'INACTIVE' as const };
      service.deactivateException('exc-1').subscribe((res) => {
        expect(res.status).toBe('INACTIVE');
        expect(service.exceptions()[0].status).toBe('INACTIVE');
      });
      const reqDeact = httpTesting.expectOne(`${exceptionsUrl}/exc-1/deactivate`);
      expect(reqDeact.request.method).toBe('PATCH');
      reqDeact.flush(deactivated);

      // Activate
      const activated = { ...mockExceptions[0], status: 'ACTIVE' as const };
      service.activateException('exc-1').subscribe((res) => {
        expect(res.status).toBe('ACTIVE');
        expect(service.exceptions()[0].status).toBe('ACTIVE');
      });
      const reqAct = httpTesting.expectOne(`${exceptionsUrl}/exc-1/activate`);
      expect(reqAct.request.method).toBe('PATCH');
      reqAct.flush(activated);
    });

    it('debe priorizar excepción activa del día sobre la capacidad base', () => {
      const today = new Date();
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

      // Configuración base
      const isDay = today.getDay();
      const todayDayOfWeek = isDay === 0 ? 7 : isDay;
      const baseConfig: CapacityConfiguration = {
        id: 'cfg-today',
        storeId: 'store-1',
        dayOfWeek: todayDayOfWeek,
        startTime: '09:00:00',
        endTime: '14:00:00',
        maxCapacity: 10,
        status: 'ACTIVE',
        createdAt: '2026-09-19T10:00:00Z',
        updatedAt: '2026-09-19T10:00:00Z',
      };

      service.configurations.set([baseConfig]);
      expect(service.todayTotalCapacity()).toBe(10);
      expect(service.todayEffectiveCapacity()).toBe(10);
      expect(service.isTodayClosed()).toBe(false);

      // Excepción activa hoy con 0 cupos (Cerrado)
      const closedException = {
        id: 'exc-today',
        storeId: 'store-1',
        serviceDate: todayStr,
        startTime: null,
        endTime: null,
        exceptionCapacity: 0,
        reason: 'Cerrado por Mantenimiento',
        status: 'ACTIVE' as const,
        createdAt: '2026-09-22T10:00:00Z',
        updatedAt: '2026-09-22T10:00:00Z',
      };

      service.exceptions.set([closedException]);
      expect(service.todayException()).toEqual(closedException);
      expect(service.todayEffectiveCapacity()).toBe(0);
      expect(service.isTodayClosed()).toBe(true);

      // Excepción activa hoy con capacidad aumentada (ej. 30 cupos)
      const specialException = {
        ...closedException,
        exceptionCapacity: 30,
        reason: 'Evento Especial',
      };
      service.exceptions.set([specialException]);
      expect(service.todayEffectiveCapacity()).toBe(30);
      expect(service.isTodayClosed()).toBe(false);
    });
  });
});

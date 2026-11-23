import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Subject } from 'rxjs';
import { InventoryService } from './inventory.service';
import { InventoryResponse } from '../models/inventory.models';
import { TokenService } from './token-service';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import { OrderService } from './order.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;
  let tokenServiceMock: {
    getUserId: ReturnType<typeof vi.fn>;
    getToken: ReturnType<typeof vi.fn>;
    hasValidToken: ReturnType<typeof vi.fn>;
  };
  let orderConfirmedSubject: Subject<any>;
  let reconnectedSubject: Subject<void>;
  let orderMutatedSubject: Subject<any>;
  let storeServiceMock: { currentStore: ReturnType<typeof vi.fn> };
  let stompServiceMock: { orderConfirmed$: any; reconnected$: any };
  let orderServiceMock: { orderMutated$: any };

  const baseUrl = `${environment.apiUrl}/inventory`;

  const mockInventoryItem1: InventoryResponse = {
    variantId: 'var-1',
    productId: 'prod-1',
    productName: 'Torta de Chocolate',
    presentationName: 'Porción individual',
    sku: 'TOR-CHOC-01',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 15,
    reservedQuantity: 2,
    availableQuantity: 13,
    lowStockThreshold: 5,
    isLowStock: false,
    updatedAt: '2026-11-16T10:00:00Z',
  };

  const mockInventoryItem2: InventoryResponse = {
    variantId: 'var-2',
    productId: 'prod-1',
    productName: 'Torta de Chocolate',
    presentationName: 'Torta entera 8 porciones',
    sku: 'TOR-CHOC-08',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 2,
    reservedQuantity: 0,
    availableQuantity: 2,
    lowStockThreshold: 5,
    isLowStock: true,
    updatedAt: '2026-11-16T10:00:00Z',
  };

  const mockInventoryItem3: InventoryResponse = {
    variantId: 'var-3',
    productId: 'prod-2',
    productName: 'Galletas de Avena',
    presentationName: 'Paquete x 6',
    sku: null,
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'NOT_CONFIGURED',
    physicalQuantity: null,
    reservedQuantity: null,
    availableQuantity: null,
    lowStockThreshold: null,
    isLowStock: false,
    updatedAt: null,
  };

  const mockInventoryItem4: InventoryResponse = {
    variantId: 'var-4',
    productId: 'prod-3',
    productName: 'Café Americano',
    presentationName: 'Taza 12oz',
    sku: 'CAF-AMER-12',
    tracksInventory: false,
    status: 'ACTIVE',
    inventoryStatus: 'NOT_TRACKED',
    physicalQuantity: null,
    reservedQuantity: null,
    availableQuantity: null,
    lowStockThreshold: null,
    isLowStock: false,
    updatedAt: null,
  };

  beforeEach(() => {
    tokenServiceMock = {
      getUserId: vi.fn().mockReturnValue('merchant-user-1'),
      getToken: vi.fn().mockReturnValue('valid-jwt-token-1'),
      hasValidToken: vi.fn().mockReturnValue(true),
    };

    orderConfirmedSubject = new Subject();
    reconnectedSubject = new Subject();
    orderMutatedSubject = new Subject();

    storeServiceMock = {
      currentStore: vi.fn().mockReturnValue({ id: 'store-123' }),
    };

    stompServiceMock = {
      orderConfirmed$: orderConfirmedSubject.asObservable(),
      reconnected$: reconnectedSubject.asObservable(),
    };

    orderServiceMock = {
      orderMutated$: orderMutatedSubject.asObservable(),
    };

    TestBed.configureTestingModule({
      providers: [
        InventoryService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TokenService, useValue: tokenServiceMock },
        { provide: StoreService, useValue: storeServiceMock },
        { provide: StompService, useValue: stompServiceMock },
        { provide: OrderService, useValue: orderServiceMock },
      ],
    });

    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente con estado inicial vacío', () => {
    expect(service).toBeTruthy();
    expect(service.inventory()).toEqual([]);
    expect(service.lowStock()).toEqual([]);
    expect(service.isLoading()).toBe(false);
    expect(service.isLoaded()).toBe(false);
    expect(service.lowStockCount()).toBe(0);
    expect(service.totalCount()).toBe(0);
    expect(service.updatingVariantId()).toBeNull();
  });

  describe('1. GET /api/v1/inventory (getStoreInventory)', () => {
    it('debe emitir petición GET a la URL base y cargar el inventario', () => {
      let result: InventoryResponse[] | undefined;

      service.getStoreInventory().subscribe((data) => {
        result = data;
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');

      req.flush([mockInventoryItem1, mockInventoryItem2, mockInventoryItem3, mockInventoryItem4]);

      expect(result).toHaveLength(4);
      expect(service.inventory()).toHaveLength(4);
      expect(service.isLoaded()).toBe(true);
      expect(service.isLoading()).toBe(false);
      expect(service.totalCount()).toBe(4);
      expect(service.configuredCount()).toBe(2);
      expect(service.notConfiguredCount()).toBe(1);
      expect(service.notTrackedCount()).toBe(1);
      // Prellena lowStock coherentemente
      expect(service.lowStock()).toHaveLength(1);
      expect(service.lowStockCount()).toBe(1);
      expect(service.lowStock()[0].variantId).toBe('var-2');
    });

    it('debe retornar datos en memoria sin llamada HTTP en segunda invocación (Cache-First)', () => {
      service.getStoreInventory().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush([mockInventoryItem1]);

      expect(service.inventory()).toHaveLength(1);

      let cachedResult: InventoryResponse[] | undefined;
      service.getStoreInventory().subscribe((data) => {
        cachedResult = data;
      });

      httpMock.expectNone(baseUrl);
      expect(cachedResult).toHaveLength(1);
    });

    it('debe forzar refresco HTTP si forceRefresh=true', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      service.getStoreInventory(true).subscribe();
      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockInventoryItem1, mockInventoryItem2]);

      expect(service.inventory()).toHaveLength(2);
    });

    it('debe deduplicar peticiones concurrentes en tránsito', () => {
      let res1: InventoryResponse[] | undefined;
      let res2: InventoryResponse[] | undefined;

      service.getStoreInventory().subscribe((d) => (res1 = d));
      service.getStoreInventory().subscribe((d) => (res2 = d));

      const requests = httpMock.match(baseUrl);
      expect(requests).toHaveLength(1);

      requests[0].flush([mockInventoryItem1]);

      expect(res1).toHaveLength(1);
      expect(res2).toHaveLength(1);
    });

    it('debe propagar error HTTP 401 y registrar mensaje de error', () => {
      let actualError: any;

      service.getStoreInventory().subscribe({
        next: () => expect.unreachable('no debió tener éxito'),
        error: (err) => (actualError = err),
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush({ code: 'UNAUTHORIZED', message: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

      expect(actualError.status).toBe(401);
      expect(service.isLoading()).toBe(false);
      expect(service.error()).toBeTruthy();
    });

    it('debe propagar error HTTP 403 Forbidden', () => {
      let actualError: any;

      service.getStoreInventory().subscribe({
        next: () => expect.unreachable('no debió tener éxito'),
        error: (err) => (actualError = err),
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush({ code: 'FORBIDDEN', message: 'Rol inválido' }, { status: 403, statusText: 'Forbidden' });

      expect(actualError.status).toBe(403);
    });
  });

  describe('2. GET /api/v1/inventory/variants/{variantId} (getVariantInventory)', () => {
    it('debe consultar variante y actualizar el registro en la lista local', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      const updatedVariant: InventoryResponse = {
        ...mockInventoryItem1,
        physicalQuantity: 20,
        availableQuantity: 18,
      };

      let result: InventoryResponse | undefined;
      service.getVariantInventory('var-1').subscribe((data) => {
        result = data;
      });

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1`);
      expect(req.request.method).toBe('GET');
      req.flush(updatedVariant);

      expect(result?.physicalQuantity).toBe(20);
      expect(service.inventory()[0].physicalQuantity).toBe(20);
    });

    it('debe propagar error HTTP 404 si la variante no existe en la tienda', () => {
      let actualError: any;

      service.getVariantInventory('var-999').subscribe({
        next: () => expect.unreachable('no debió tener éxito'),
        error: (err) => (actualError = err),
      });

      const req = httpMock.expectOne(`${baseUrl}/variants/var-999`);
      req.flush(
        { code: 'VARIANT_NOT_FOUND', message: 'Variante no encontrada o no pertenece a la tienda' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(actualError.status).toBe(404);
    });
  });

  describe('3. GET /api/v1/inventory/low-stock (getLowStockInventory)', () => {
    it('debe consultar variantes en stock bajo y actualizar señal reactiva y estado general', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1, mockInventoryItem2]);

      let result: InventoryResponse[] | undefined;
      service.getLowStockInventory(true).subscribe((data) => {
        result = data;
      });

      expect(service.isLowStockLoading()).toBe(true);

      const req = httpMock.expectOne(`${baseUrl}/low-stock`);
      expect(req.request.method).toBe('GET');
      req.flush([mockInventoryItem2]);

      expect(result).toHaveLength(1);
      expect(service.lowStock()).toHaveLength(1);
      expect(service.isLowStockLoaded()).toBe(true);
      expect(service.isLowStockLoading()).toBe(false);
      expect(service.lowStockCount()).toBe(1);
    });

    it('debe reutilizar caché si isLowStockLoaded=true y forceRefresh=false', () => {
      service.getLowStockInventory().subscribe();
      httpMock.expectOne(`${baseUrl}/low-stock`).flush([mockInventoryItem2]);

      let cached: InventoryResponse[] | undefined;
      service.getLowStockInventory().subscribe((d) => (cached = d));

      httpMock.expectNone(`${baseUrl}/low-stock`);
      expect(cached).toHaveLength(1);
    });
  });

  describe('4. PUT /api/v1/inventory/variants/{variantId}/stock (updateStock)', () => {
    it('debe enviar payload numérico, actualizar updatingVariantId y sincronizar estado', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      const updatedResponse: InventoryResponse = {
        ...mockInventoryItem1,
        physicalQuantity: 30,
        availableQuantity: 28,
        isLowStock: false,
      };

      let result: InventoryResponse | undefined;
      service.updateStock('var-1', 30).subscribe((res) => (result = res));

      expect(service.updatingVariantId()).toBe('var-1');

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1/stock`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ quantity: 30 });

      req.flush(updatedResponse);

      expect(service.updatingVariantId()).toBeNull();
      expect(result?.physicalQuantity).toBe(30);
      expect(service.inventory()[0].physicalQuantity).toBe(30);
      expect(service.inventory()[0].availableQuantity).toBe(28);
    });

    it('debe aceptar objeto UpdateStockRequest directamente', () => {
      service.updateStock('var-1', { quantity: 10 }).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1/stock`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ quantity: 10 });

      req.flush({ ...mockInventoryItem1, physicalQuantity: 10 });
    });

    it('debe añadir a lowStock si la nueva cantidad física queda por debajo del umbral', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      expect(service.lowStockCount()).toBe(0);

      const becameLowStock: InventoryResponse = {
        ...mockInventoryItem1,
        physicalQuantity: 3,
        availableQuantity: 1,
        isLowStock: true,
      };

      service.updateStock('var-1', 3).subscribe();
      httpMock.expectOne(`${baseUrl}/variants/var-1/stock`).flush(becameLowStock);

      expect(service.lowStockCount()).toBe(1);
      expect(service.lowStock()[0].variantId).toBe('var-1');
    });

    it('debe remover de lowStock si el reabastecimiento supera el umbral de alerta', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem2]);

      expect(service.lowStockCount()).toBe(1);

      const replenished: InventoryResponse = {
        ...mockInventoryItem2,
        physicalQuantity: 25,
        availableQuantity: 25,
        isLowStock: false,
      };

      service.updateStock('var-2', 25).subscribe();
      httpMock.expectOne(`${baseUrl}/variants/var-2/stock`).flush(replenished);

      expect(service.lowStockCount()).toBe(0);
      expect(service.inventory()[0].isLowStock).toBe(false);
    });

    it('debe propagar error 409 Conflict si la cantidad es menor a reservas activas', () => {
      let actualError: any;

      service.updateStock('var-1', 1).subscribe({
        next: () => expect.unreachable('no debió tener éxito'),
        error: (err) => (actualError = err),
      });

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1/stock`);
      req.flush(
        {
          code: 'CANNOT_REDUCE_BELOW_RESERVED',
          message: 'No se puede ajustar la cantidad por debajo de las reservas activas',
        },
        { status: 409, statusText: 'Conflict' }
      );

      expect(actualError.status).toBe(409);
      expect(service.updatingVariantId()).toBeNull();
    });
  });

  describe('5. PATCH /api/v1/inventory/variants/{variantId}/threshold (updateThreshold)', () => {
    it('debe enviar payload PATCH con { lowStockThreshold: N } y sincronizar estado', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      const updatedResponse: InventoryResponse = {
        ...mockInventoryItem1,
        lowStockThreshold: 10,
        isLowStock: false,
      };

      let result: InventoryResponse | undefined;
      service.updateThreshold('var-1', 10).subscribe((res) => (result = res));

      expect(service.updatingVariantId()).toBe('var-1');

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1/threshold`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ lowStockThreshold: 10 });

      req.flush(updatedResponse);

      expect(service.updatingVariantId()).toBeNull();
      expect(result?.lowStockThreshold).toBe(10);
      expect(service.inventory()[0].lowStockThreshold).toBe(10);
    });

    it('debe permitir enviar null para desactivar el umbral de alerta', () => {
      service.updateThreshold('var-1', null).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/variants/var-1/threshold`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ lowStockThreshold: null });

      req.flush({ ...mockInventoryItem1, lowStockThreshold: null, isLowStock: false });
    });
  });

  describe('6. Gestión de Sesión, Desfase y Limpieza de Caché', () => {
    it('debe vaciar todas las señales reactivas con clearCache()', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1, mockInventoryItem2]);

      expect(service.inventory()).toHaveLength(2);
      expect(service.isLoaded()).toBe(true);

      service.clearCache();

      expect(service.inventory()).toEqual([]);
      expect(service.lowStock()).toEqual([]);
      expect(service.isLoaded()).toBe(false);
      expect(service.isLoading()).toBe(false);
      expect(service.isLowStockLoaded()).toBe(false);
      expect(service.error()).toBeNull();
      expect(service.searchQuery()).toBe('');
      expect(service.statusFilter()).toBe('ALL');
    });

    it('debe descartar respuestas fuera de orden si el usuario cambió antes de que termine la petición', () => {
      service.getStoreInventory().subscribe();
      const req = httpMock.expectOne(baseUrl);

      // El usuario cambia de sesión mientras la petición está en la red
      tokenServiceMock.getUserId.mockReturnValue('merchant-user-2');
      tokenServiceMock.getToken.mockReturnValue('jwt-token-user-2');

      // Llega la respuesta atrasada del usuario 1
      req.flush([mockInventoryItem1]);

      // No debió escribir datos del usuario 1 en el estado del usuario 2
      expect(service.inventory()).toEqual([]);
      expect(service.isLoaded()).toBe(false);
    });

    it('debe descartar respuestas fuera de orden si el usuario cerró sesión antes de responder', () => {
      service.getStoreInventory().subscribe();
      const req = httpMock.expectOne(baseUrl);

      // El usuario hace logout mientras la petición viaja
      tokenServiceMock.hasValidToken.mockReturnValue(false);
      tokenServiceMock.getUserId.mockReturnValue(null);

      req.flush([mockInventoryItem1]);

      expect(service.inventory()).toEqual([]);
    });
  });

  describe('7. Preparación para la Vista de Inventario (Búsqueda y Filtros)', () => {
    beforeEach(() => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([
        mockInventoryItem1,
        mockInventoryItem2,
        mockInventoryItem3,
        mockInventoryItem4,
      ]);
    });

    it('debe retornar todas las variantes cuando statusFilter=ALL y searchQuery vacío', () => {
      expect(service.filteredInventory()).toHaveLength(4);
    });

    it('debe filtrar por texto en productName, presentationName y sku', () => {
      service.setSearchQuery('selva');
      expect(service.filteredInventory()).toHaveLength(0);

      service.setSearchQuery('chocolate');
      expect(service.filteredInventory()).toHaveLength(2);

      service.setSearchQuery('porción');
      expect(service.filteredInventory()).toHaveLength(1);

      service.setSearchQuery('CAF-AMER');
      expect(service.filteredInventory()).toHaveLength(1);
    });

    it('debe filtrar por estado de configuración CONFIGURED, NOT_CONFIGURED, NOT_TRACKED y LOW_STOCK', () => {
      service.setStatusFilter('CONFIGURED');
      expect(service.filteredInventory()).toHaveLength(2);

      service.setStatusFilter('NOT_CONFIGURED');
      expect(service.filteredInventory()).toHaveLength(1);
      expect(service.filteredInventory()[0].variantId).toBe('var-3');

      service.setStatusFilter('NOT_TRACKED');
      expect(service.filteredInventory()).toHaveLength(1);
      expect(service.filteredInventory()[0].variantId).toBe('var-4');

      service.setStatusFilter('LOW_STOCK');
      expect(service.filteredInventory()).toHaveLength(1);
      expect(service.filteredInventory()[0].variantId).toBe('var-2');
    });

    it('debe limpiar filtros con clearFilters()', () => {
      service.setSearchQuery('chocolate');
      service.setStatusFilter('LOW_STOCK');
      expect(service.filteredInventory()).toHaveLength(1);

      service.clearFilters();
      expect(service.searchQuery()).toBe('');
      expect(service.statusFilter()).toBe('ALL');
      expect(service.filteredInventory()).toHaveLength(4);
    });
  });

  describe('8. Sincronización por Eventos y Aislamiento de Sesión (M2.1)', () => {
    it('1. Logout limpia datos de inventario', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1, mockInventoryItem2]);

      expect(service.inventory()).toHaveLength(2);
      expect(service.isLoaded()).toBe(true);

      // Simular cierre de sesión
      service.clearCache();

      expect(service.inventory()).toEqual([]);
      expect(service.lowStock()).toEqual([]);
      expect(service.isLoaded()).toBe(false);
      expect(service.isLoading()).toBe(false);
      expect(service.isLowStockLoaded()).toBe(false);
      expect(service.error()).toBeNull();
    });

    it('2. Cambio de usuario no reutiliza caché', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);

      expect(service.inventory()).toHaveLength(1);

      // El usuario activo cambia
      tokenServiceMock.getUserId.mockReturnValue('merchant-user-2');
      tokenServiceMock.getToken.mockReturnValue('valid-jwt-token-2');

      // Nueva consulta debe limpiar la caché previa y emitir petición HTTP fresca
      let user2Inventory: InventoryResponse[] | undefined;
      service.getStoreInventory().subscribe((data) => {
        user2Inventory = data;
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush([mockInventoryItem2]);

      expect(user2Inventory).toEqual([mockInventoryItem2]);
      expect(service.inventory()).toEqual([mockInventoryItem2]);
    });

    it('3. Respuesta HTTP tardía se descarta', () => {
      service.getStoreInventory().subscribe();
      const req = httpMock.expectOne(baseUrl);

      // El usuario cierra sesión antes de que el servidor responda
      service.clearCache();
      tokenServiceMock.hasValidToken.mockReturnValue(false);
      tokenServiceMock.getUserId.mockReturnValue(null);

      // Llega la respuesta demorada
      req.flush([mockInventoryItem1]);

      expect(service.inventory()).toEqual([]);
      expect(service.isLoaded()).toBe(false);
    });

    it('4. Compra confirmada invalida inventario', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);
      expect(service.isLoaded()).toBe(true);

      // Vista inactiva
      service.setViewActive(false);

      // Evento WebSocket de orden confirmada en la tienda activa
      orderConfirmedSubject.next({
        storeId: 'store-123',
        orderId: 'ord-1',
        orderNumber: 'ORD-001',
      });

      // La caché de inventario debe marcarse como sucia (no cargada)
      expect(service.isLoaded()).toBe(false);

      // Sincroniza en background low-stock para la barra lateral
      httpMock.expectOne(`${baseUrl}/low-stock`).flush([]);
    });

    it('5. Cancelación con restitución invalida inventario', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);
      expect(service.isLoaded()).toBe(true);

      service.setViewActive(false);

      // Mutación de pedido a CANCELLED (restituye inventario según RN-ORD-13)
      orderMutatedSubject.next({
        storeId: 'store-123',
        orderId: 'ord-1',
        status: 'CANCELLED',
      });

      expect(service.isLoaded()).toBe(false);

      httpMock.expectOne(`${baseUrl}/low-stock`).flush([]);
    });

    it('6. Evento duplicado no genera solicitudes innecesarias', () => {
      service.setViewActive(true);

      // Primer evento dispara refresco
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-1' });
      const req1 = httpMock.expectOne(baseUrl);

      // Eventos repetidos en ráfaga mientras la primera petición está en vuelo
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-2' });
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-3' });

      // No debe haber peticiones simultáneas paralelas abiertas
      httpMock.expectNone(`${baseUrl}?*`);

      // Al resolverse la primera petición, debe encadenar exactamente una trailing fresca
      req1.flush([mockInventoryItem1]);

      const req2 = httpMock.expectOne(baseUrl);
      req2.flush([{ ...mockInventoryItem1, physicalQuantity: 14 }]);

      expect(service.inventory()[0].physicalQuantity).toBe(14);
    });

    it('7. Inventario activo se refresca', () => {
      service.setViewActive(true);

      // Evento de compra confirmada mientras el comerciante observa la pantalla
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-1' });

      const req = httpMock.expectOne(baseUrl);
      req.flush([{ ...mockInventoryItem1, physicalQuantity: 12, availableQuantity: 10 }]);

      expect(service.inventory()[0].physicalQuantity).toBe(12);
      expect(service.inventory()[0].availableQuantity).toBe(10);
    });

    it('8. Inventario inactivo obtiene datos frescos al volver', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);
      expect(service.isLoaded()).toBe(true);

      // Navega fuera de /inventory
      service.setViewActive(false);

      // Ocurre venta mientras está fuera
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-1' });
      expect(service.isLoaded()).toBe(false);
      httpMock.expectOne(`${baseUrl}/low-stock`).flush([]);

      // Vuelve a /inventory: el componente se monta e invoca getStoreInventory()
      service.setViewActive(true);
      service.getStoreInventory().subscribe();

      // Debe consultar al backend porque la caché fue invalidada
      const req = httpMock.expectOne(baseUrl);
      req.flush([{ ...mockInventoryItem1, physicalQuantity: 10 }]);

      expect(service.inventory()[0].physicalQuantity).toBe(10);
      expect(service.isLoaded()).toBe(true);
    });

    it('9. Alertas de stock bajo se actualizan', () => {
      service.getLowStockInventory().subscribe();
      httpMock.expectOne(`${baseUrl}/low-stock`).flush([mockInventoryItem2]);
      expect(service.lowStock()).toHaveLength(1);

      service.setViewActive(false);

      // Compra confirmada empuja a otra variante a stock bajo
      orderConfirmedSubject.next({ storeId: 'store-123', orderId: 'ord-1' });

      // Debe refrescar low-stock automáticamente para mantener sidebar/dashboard al día
      const req = httpMock.expectOne(`${baseUrl}/low-stock`);
      req.flush([mockInventoryItem2, { ...mockInventoryItem1, isLowStock: true }]);

      expect(service.lowStock()).toHaveLength(2);
      expect(service.lowStockCount()).toBe(2);
    });

    it('10. Fallo HTTP permite reintento manual', () => {
      service.getStoreInventory().subscribe({
        error: () => {},
      });

      const req1 = httpMock.expectOne(baseUrl);
      req1.flush({ message: 'Error interno del servidor' }, { status: 500, statusText: 'Server Error' });

      expect(service.error()).toBeTruthy();
      expect(service.isLoading()).toBe(false);
      expect(service.isLoaded()).toBe(false);

      // Reintento manual forzado por el usuario (botón Reintentar)
      service.getStoreInventory(true).subscribe();

      expect(service.error()).toBeNull();
      expect(service.isLoading()).toBe(true);

      const req2 = httpMock.expectOne(baseUrl);
      req2.flush([mockInventoryItem1]);

      expect(service.error()).toBeNull();
      expect(service.inventory()).toEqual([mockInventoryItem1]);
      expect(service.isLoaded()).toBe(true);
    });

    it('11. Comanda directa en InvalidationBus invalida la caché de inventario', () => {
      service.getStoreInventory().subscribe();
      httpMock.expectOne(baseUrl).flush([mockInventoryItem1]);
      expect(service.isLoaded()).toBe(true);

      service.setViewActive(false);

      const bus = TestBed.inject(InvalidationBusService);
      bus.dispatchOrderCreated({
        storeId: 'store-123',
        orderId: 'ord-direct-1',
        source: 'LOCAL_MERCHANT',
      });

      expect(service.isLoaded()).toBe(false);
      // Debe refrescar alertas de stock bajo
      httpMock.expectOne(`${baseUrl}/low-stock`).flush([]);
    });
  });
});

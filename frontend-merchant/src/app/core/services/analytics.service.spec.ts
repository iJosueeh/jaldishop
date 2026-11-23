import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { AnalyticsService, calculateDateOffset } from './analytics.service';
import { StoreService } from './store.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';
import { MerchantAnalyticsSummaryResponse } from '../models/analytics.models';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;
  let storeService: StoreService;
  let invalidationBus: InvalidationBusService;
  let toastMock: { error: ReturnType<typeof vi.fn>; success: ReturnType<typeof vi.fn> };

  const mockSummary: MerchantAnalyticsSummaryResponse = {
    storeId: 'store-123',
    period: {
      from: '2026-10-21',
      to: '2026-11-20',
      timeZone: 'America/Lima',
    },
    commercial: {
      historicalConfirmedOrdersCount: 150,
      historicalConfirmedGmv: 4500.5,
      activeOrdersCount: 5,
      activeCommercialVolume: 180.0,
      completedOrdersCount: 140,
      productsGrossSales: 4200.0,
      totalDiscounts: 50.0,
      totalDeliveryFees: 350.5,
      cancelledOrdersCount: 5,
      cancelledOrdersVolume: 120.0,
      averageTicket: 30.0,
    },
    financial: {
      grossInflowAmount: 4500.5,
      approvedPaymentsCount: 150,
      settledRefundsAmount: 120.0,
      settledRefundsCount: 5,
      netFinancialInflow: 4380.5,
      orderLinkedPaymentsAmount: 4500.5,
      orderLinkedPaymentsCount: 150,
      orphanPaymentsAmount: 0,
      orphanPaymentsCount: 0,
      disclosure: 'Fondos capturados en Mercado Pago.',
    },
    topProducts: [
      {
        variantId: 'v-1',
        productId: 'p-1',
        productName: 'Torta Selva Negra',
        variantName: 'Mediana (8 porciones)',
        unitsSold: 35,
        totalRevenue: 1050.0,
      },
    ],
    capacityDemand: {
      committedReservationsCount: 145,
      releasedReservationsCount: 5,
      notes: null,
    },
  };

  beforeEach(() => {
    toastMock = {
      error: vi.fn(),
      success: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AnalyticsService,
        StoreService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toastMock },
      ],
    });

    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
    storeService = TestBed.inject(StoreService);
    invalidationBus = TestBed.inject(InvalidationBusService);

    storeService.currentStore.set({
      id: 'store-123',
      merchantUserId: 'usr-1',
      name: 'Mi Tienda Test',
      slug: 'mi-tienda-test',
      pickupEnabled: true,
      deliveryEnabled: true,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('calculateDateOffset', () => {
    it('debe restar días exactamente en base a fecha ISO YYYY-MM-DD', () => {
      expect(calculateDateOffset('2026-11-20', 7)).toBe('2026-11-13');
      expect(calculateDateOffset('2026-11-20', 30)).toBe('2026-10-21');
      expect(calculateDateOffset('2026-03-01', 1)).toBe('2026-02-28');
    });
  });

  describe('loadSummary', () => {
    it('debe enviar GET con parámetros from, to y storeId y actualizar la señal summary', () => {
      let result: MerchantAnalyticsSummaryResponse | null = null;
      service.loadSummary('2026-10-21', '2026-11-20').subscribe((res) => {
        result = res;
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne((r) =>
        r.url === `${environment.apiUrl}/merchant/analytics/summary` &&
        r.params.get('from') === '2026-10-21' &&
        r.params.get('to') === '2026-11-20' &&
        r.params.get('storeId') === 'store-123'
      );
      expect(req.request.method).toBe('GET');

      req.flush(mockSummary);

      expect(result).toEqual(mockSummary);
      expect(service.summary()).toEqual(mockSummary);
      expect(service.isLoading()).toBe(false);
      expect(service.error()).toBeNull();
    });

    it('debe manejar error HTTP, actualizar señal error y mostrar toast', () => {
      let hasError = false;
      service.loadSummary().subscribe({
        error: () => {
          hasError = true;
        },
      });

      const req = httpMock.expectOne((r) => r.url.includes('/analytics/summary'));
      req.flush(
        { message: 'Rango no permitido' },
        { status: 400, statusText: 'Bad Request' }
      );

      expect(hasError).toBe(true);
      expect(service.error()).toBe('Rango no permitido');
      expect(service.isLoading()).toBe(false);
      expect(toastMock.error).toHaveBeenCalledWith('Rango no permitido');
    });
  });

  describe('setPreset', () => {
    it('debe actualizar señales fromDate, toDate y selectedPreset y ejecutar loadSummary', () => {
      service.setPreset('7D').subscribe();

      expect(service.selectedPreset()).toBe('7D');

      const req = httpMock.expectOne((r) => r.url.includes('/analytics/summary'));
      expect(req.request.params.get('storeId')).toBe('store-123');
      req.flush(mockSummary);
    });
  });

  describe('exportPdf', () => {
    it('debe solicitar blob de PDF con query params y descargar archivo', () => {
      const mockBlob = new Blob(['%PDF-1.4 test'], { type: 'application/pdf' });
      const createObjectUrlSpy = vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:http://localhost/test-pdf');
      const revokeObjectUrlSpy = vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

      let emittedBlob: Blob | null = null;
      service.exportPdf('2026-10-21', '2026-11-20').subscribe((blob) => {
        emittedBlob = blob;
      });

      expect(service.isExportingPdf()).toBe(true);

      const req = httpMock.expectOne((r) =>
        r.url === `${environment.apiUrl}/merchant/analytics/export/pdf` &&
        r.params.get('from') === '2026-10-21' &&
        r.params.get('to') === '2026-11-20' &&
        r.params.get('storeId') === 'store-123'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');

      req.flush(mockBlob);

      expect(emittedBlob).toBe(mockBlob);
      expect(createObjectUrlSpy).toHaveBeenCalledWith(mockBlob);
      expect(revokeObjectUrlSpy).toHaveBeenCalled();
      expect(service.isExportingPdf()).toBe(false);
      expect(toastMock.success).toHaveBeenCalledWith('Reporte PDF descargado con éxito.');
    });

    it('debe manejar error al exportar PDF y mostrar toast', () => {
      let hasError = false;
      service.exportPdf().subscribe({
        error: () => {
          hasError = true;
        },
      });

      const req = httpMock.expectOne((r) => r.url.includes('/export/pdf'));
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      expect(hasError).toBe(true);
      expect(service.isExportingPdf()).toBe(false);
      expect(toastMock.error).toHaveBeenCalledWith('Error al exportar el reporte en PDF.');
    });
  });

  describe('InvalidationBus & Consistencia de Caché Reactiva', () => {
    it('debe marcar isOutdated=true cuando un pedido pasa a COMPLETED', () => {
      expect(service.isOutdated()).toBe(false);

      invalidationBus.dispatchOrderMutated({
        storeId: 'store-123',
        orderId: 'ord-1',
        status: 'COMPLETED',
        source: 'WEBSOCKET',
      });

      expect(service.isOutdated()).toBe(true);
    });

    it('debe marcar isOutdated=true cuando un pedido pasa a CANCELLED', () => {
      expect(service.isOutdated()).toBe(false);

      invalidationBus.dispatchOrderMutated({
        storeId: 'store-123',
        orderId: 'ord-1',
        status: 'CANCELLED',
        source: 'LOCAL_MERCHANT',
      });

      expect(service.isOutdated()).toBe(true);
    });

    it('no debe marcar isOutdated=true si el evento pertenece a otra tienda', () => {
      expect(service.isOutdated()).toBe(false);

      invalidationBus.dispatchOrderMutated({
        storeId: 'store-otra',
        orderId: 'ord-1',
        status: 'COMPLETED',
        source: 'WEBSOCKET',
      });

      expect(service.isOutdated()).toBe(false);
    });

    it('debe limpiar isOutdated=false al recargar exitosamente con loadSummary', () => {
      service.invalidateCache();
      expect(service.isOutdated()).toBe(true);

      service.loadSummary().subscribe();
      const req = httpMock.expectOne((r) => r.url.endsWith('/summary'));
      req.flush(mockSummary);

      expect(service.isOutdated()).toBe(false);
      expect(service.summary()).toEqual(mockSummary);
    });
  });
});

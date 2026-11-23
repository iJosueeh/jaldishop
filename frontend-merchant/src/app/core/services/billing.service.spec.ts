import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { of } from 'rxjs';
import { BillingService } from './billing.service';
import { StoreService } from './store.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';
import { MerchantBillingSummaryResponse } from '../models/billing.models';

describe('BillingService', () => {
  let service: BillingService;
  let httpMock: HttpTestingController;
  let storeService: StoreService;
  let invalidationBus: InvalidationBusService;
  let toastMock: { error: ReturnType<typeof vi.fn>; success: ReturnType<typeof vi.fn> };

  const mockStoreId = 'store-uuid-1';
  const mockSummary: MerchantBillingSummaryResponse = {
    storeId: mockStoreId,
    availableBalance: 150.0,
    lockedBalance: 0.0,
    totalPaidOut: 300.0,
    minPayoutThreshold: 50.0,
    currency: 'PEN',
    payoutDestination: {
      id: 'dest-uuid-1',
      storeId: mockStoreId,
      payoutChannel: 'BANK_TRANSFER_CCI',
      bankName: 'BCP',
      accountCci: '00219400123456789012',
      accountHolderName: 'Restaurante El Criollo',
      accountHolderDocType: 'RUC',
      accountHolderDocNumber: '20123456789',
      isPrimary: true,
      isVerified: true,
      updatedAt: '2026-11-21T10:00:00Z',
    },
    asOf: '2026-11-21T10:00:00Z',
  };

  beforeEach(() => {
    toastMock = {
      error: vi.fn(),
      success: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        BillingService,
        provideHttpClient(),
        provideHttpClientTesting(),
        InvalidationBusService,
        {
          provide: StoreService,
          useValue: {
            currentStore: vi.fn().mockReturnValue({ id: mockStoreId, name: 'Mi Tienda' }),
            getMyStore: vi.fn().mockReturnValue(of({ id: mockStoreId, name: 'Mi Tienda' })),
          },
        },
        { provide: ToastService, useValue: toastMock },
      ],
    });

    service = TestBed.inject(BillingService);
    httpMock = TestBed.inject(HttpTestingController);
    storeService = TestBed.inject(StoreService);
    invalidationBus = TestBed.inject(InvalidationBusService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe inicializarse con balances en 0 y cargar resumen exitosamente', () => {
    expect(service.availableBalance()).toBe(0);
    expect(service.canRequestPayout()).toBe(false);

    service.loadSummary().subscribe((res) => {
      expect(res.availableBalance).toBe(150.0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/billing/summary`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);

    expect(service.availableBalance()).toBe(150.0);
    expect(service.hasDestination()).toBe(true);
    expect(service.canRequestPayout()).toBe(true);
  });

  it('debe resolver la URL y cargar el resumen incluso si currentStore es nulo al inicio', () => {
    (storeService.currentStore as any).mockReturnValue(null);

    service.loadSummary().subscribe((res) => {
      expect(res.availableBalance).toBe(150.0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/billing/summary`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);

    expect(storeService.getMyStore).toHaveBeenCalled();
  });

  it('debe recargar silenciosamente en segundo plano cuando una orden pasa a COMPLETED', () => {
    service.summary.set(mockSummary);
    expect(service.availableBalance()).toBe(150.0);

    // Emitir evento COMPLETED en el bus
    invalidationBus.dispatchOrderMutated({
      storeId: mockStoreId,
      orderId: 'order-123',
      status: 'COMPLETED',
      source: 'WEBSOCKET',
    });

    const updatedSummary = { ...mockSummary, availableBalance: 195.5 };
    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/billing/summary`);
    expect(req.request.method).toBe('GET');
    req.flush(updatedSummary);

    // El balance se actualizó reactivamente sin forzar botones ni interacción del usuario
    expect(service.availableBalance()).toBe(195.5);
  });

  it('debe aplicar actualización optimista al solicitar liquidación y revertir si el servidor falla', () => {
    service.summary.set(mockSummary);
    expect(service.availableBalance()).toBe(150.0);
    expect(service.lockedBalance()).toBe(0.0);

    service.requestPayout().subscribe({
      error: () => {},
    });

    // Inmediatamente el saldo disponible pasa a 0 y se traslada a locked (Optimistic UI)
    expect(service.availableBalance()).toBe(0);
    expect(service.lockedBalance()).toBe(150.0);

    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/billing/payouts/request`);
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Error en servicio bancario' }, { status: 500, statusText: 'Server Error' });

    // Tras el error, se revierte al saldo previo
    expect(service.availableBalance()).toBe(150.0);
    expect(service.lockedBalance()).toBe(0.0);
    expect(toastMock.error).toHaveBeenCalledWith('Error en servicio bancario');
  });
});

import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FiscalReceiptResponse,
  MerchantBillingSummaryResponse,
  MerchantPayoutBatchResponse,
  MerchantPayoutDestinationResponse,
  RequestPayoutResponse,
  UpdatePayoutDestinationRequest,
} from '../models/billing.models';
import { StoreService } from './store.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';

@Injectable({
  providedIn: 'root',
})
export class BillingService {
  private readonly http = inject(HttpClient);
  private readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);
  private readonly invalidationBus = inject(InvalidationBusService, { optional: true });
  private readonly destroyRef = inject(DestroyRef, { optional: true });

  readonly summary = signal<MerchantBillingSummaryResponse | null>(null);
  readonly batches = signal<MerchantPayoutBatchResponse[]>([]);
  readonly receipts = signal<FiscalReceiptResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isBatchesLoading = signal<boolean>(false);
  readonly isReceiptsLoading = signal<boolean>(false);
  readonly isRequestingPayout = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly availableBalance = computed(() => this.summary()?.availableBalance ?? 0);
  readonly lockedBalance = computed(() => this.summary()?.lockedBalance ?? 0);
  readonly totalPaidOut = computed(() => this.summary()?.totalPaidOut ?? 0);
  readonly minThreshold = computed(() => this.summary()?.minPayoutThreshold ?? 50);
  readonly payoutDestination = computed(() => this.summary()?.payoutDestination ?? null);
  readonly hasDestination = computed(() => !!this.payoutDestination());
  readonly canRequestPayout = computed(() =>
    this.availableBalance() >= this.minThreshold() &&
    this.hasDestination() &&
    !this.isRequestingPayout()
  );

  constructor() {
    this.initRealtimeSubscriptions();
  }

  private initRealtimeSubscriptions(): void {
    if (!this.invalidationBus) return;

    const subOrderMutated = this.invalidationBus.orderMutated$.subscribe((event) => {
      const activeStoreId = this.storeService.currentStore()?.id;
      if (activeStoreId && event.storeId !== activeStoreId) return;
      if (event.status === 'COMPLETED' || event.status === 'CANCELLED') {
        this.loadSummarySilently();
      }
    });

    const subOrderCreated = this.invalidationBus.orderCreated$.subscribe((event) => {
      const activeStoreId = this.storeService.currentStore()?.id;
      if (activeStoreId && event.storeId !== activeStoreId) return;
      this.loadSummarySilently();
    });

    const subReconnected = this.invalidationBus.reconnected$.subscribe(() => {
      this.loadSummarySilently();
      this.loadBatches().subscribe();
    });

    const subCacheInvalidated = this.invalidationBus.cacheInvalidated$.subscribe((event) => {
      if (event.module === 'BILLING' || event.module === 'ALL') {
        this.loadSummarySilently();
      }
    });

    this.destroyRef?.onDestroy(() => {
      subOrderMutated.unsubscribe();
      subOrderCreated.unsubscribe();
      subReconnected.unsubscribe();
      subCacheInvalidated.unsubscribe();
    });
  }

  private ensureBaseUrl(): Observable<string> {
    const storeId = this.storeService.currentStore()?.id;
    if (storeId) {
      return of(`${environment.apiUrl}/merchants/stores/${storeId}/billing`);
    }

    return this.storeService.getMyStore().pipe(
      switchMap((store) => {
        if (!store?.id) {
          return throwError(() => new Error('No hay una tienda activa seleccionada.'));
        }
        return of(`${environment.apiUrl}/merchants/stores/${store.id}/billing`);
      })
    );
  }

  loadSummary(silent = false): Observable<MerchantBillingSummaryResponse> {
    if (!silent) {
      this.isLoading.set(true);
    }
    this.error.set(null);

    return this.ensureBaseUrl().pipe(
      switchMap((url) => this.http.get<MerchantBillingSummaryResponse>(`${url}/summary`)),
      tap((res) => {
        this.summary.set(res);
        this.isLoading.set(false);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.message || 'Error al cargar balance de liquidaciones';
        this.error.set(msg);
        return throwError(() => err);
      }),
    );
  }

  loadSummarySilently(): void {
    this.loadSummary(true).subscribe({
      error: () => {}, // Silencioso en fondo
    });
  }

  loadBatches(): Observable<MerchantPayoutBatchResponse[]> {
    this.isBatchesLoading.set(true);

    return this.ensureBaseUrl().pipe(
      switchMap((url) => this.http.get<MerchantPayoutBatchResponse[]>(`${url}/payouts`)),
      tap((list) => {
        this.batches.set(list);
        this.isBatchesLoading.set(false);
      }),
      catchError((err) => {
        this.isBatchesLoading.set(false);
        return throwError(() => err);
      }),
    );
  }

  requestPayout(): Observable<RequestPayoutResponse> {
    const current = this.summary();
    const storeId = this.storeService.currentStore()?.id;

    if (!current) {
      return throwError(() => new Error('Datos de liquidación no disponibles.'));
    }

    const requestedAmount = current.availableBalance;
    if (requestedAmount < (current.minPayoutThreshold || 50)) {
      this.toastService.error(`El monto mínimo de retiro es S/ ${(current.minPayoutThreshold || 50).toFixed(2)}`);
      return throwError(() => new Error('PAYOUT_BELOW_MINIMUM'));
    }

    this.isRequestingPayout.set(true);

    // Optimistic UI: mover saldo disponible a bloqueado
    this.summary.set({
      ...current,
      availableBalance: 0,
      lockedBalance: current.lockedBalance + requestedAmount,
    });

    return this.ensureBaseUrl().pipe(
      switchMap((url) => this.http.post<RequestPayoutResponse>(`${url}/payouts/request`, {})),
      tap((res) => {
        this.isRequestingPayout.set(false);
        this.toastService.success(res.message || 'Liquidación solicitada exitosamente');

        const activeStoreId = storeId || this.storeService.currentStore()?.id || '';
        const newBatch: MerchantPayoutBatchResponse = {
          id: res.batchId,
          storeId: activeStoreId,
          status: 'PROCESSING',
          grossAmount: requestedAmount,
          bankingFee: 0,
          netDisbursedAmount: requestedAmount,
          currency: 'PEN',
          scheduledFor: new Date().toISOString().split('T')[0],
          createdAt: new Date().toISOString(),
        };

        this.batches.set([newBatch, ...this.batches()]);

        if (activeStoreId) {
          this.invalidationBus?.dispatchSettlementRequested({
            storeId: activeStoreId,
            batchId: res.batchId,
            amount: requestedAmount,
          });
        }
      }),
      catchError((err) => {
        this.isRequestingPayout.set(false);
        // Rollback optimistic update
        this.summary.set(current);
        const errorMsg = err.error?.message || 'No se pudo procesar la solicitud de liquidación.';
        this.toastService.error(errorMsg);
        return throwError(() => err);
      }),
    );
  }

  updatePayoutDestination(request: UpdatePayoutDestinationRequest): Observable<MerchantPayoutDestinationResponse> {
    return this.ensureBaseUrl().pipe(
      switchMap((url) => this.http.put<MerchantPayoutDestinationResponse>(`${url}/payout-destination`, request)),
      tap((saved) => {
        this.toastService.success('Cuenta de liquidación guardada exitosamente.');
        const current = this.summary();
        if (current) {
          this.summary.set({
            ...current,
            payoutDestination: saved,
          });
        }
        this.loadSummarySilently();

        const storeId = this.storeService.currentStore()?.id;
        if (storeId) {
          this.invalidationBus?.dispatchCacheInvalidated({ storeId, module: 'BILLING' });
        }
      }),
      catchError((err) => {
        const errorMsg = err.error?.message || 'Error al guardar la cuenta de liquidación.';
        this.toastService.error(errorMsg);
        return throwError(() => err);
      }),
    );
  }

  loadReceipts(): Observable<FiscalReceiptResponse[]> {
    this.isReceiptsLoading.set(true);
    return this.ensureBaseUrl().pipe(
      switchMap((url) => this.http.get<FiscalReceiptResponse[]>(`${url}/receipts`)),
      tap((list) => {
        this.receipts.set(list);
      }),
      catchError((err) => {
        return throwError(() => err);
      }),
      finalize(() => this.isReceiptsLoading.set(false)),
    );
  }
}

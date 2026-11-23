import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, finalize, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnalyticsDateRangePreset,
  MerchantAnalyticsSummaryResponse,
} from '../models/analytics.models';
import { StoreService } from './store.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { getOperatingDateLima } from '../utils/date.utils';

export function calculateDateOffset(baseDateIso: string, daysOffset: number): string {
  const parts = baseDateIso.split('-').map(Number);
  const date = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2]));
  date.setUTCDate(date.getUTCDate() - daysOffset);
  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, '0');
  const d = String(date.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);
  private readonly invalidationBus = inject(InvalidationBusService, { optional: true });

  private readonly baseUrl = `${environment.apiUrl}/merchant/analytics`;

  readonly summary = signal<MerchantAnalyticsSummaryResponse | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly isExportingPdf = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly isOutdated = signal<boolean>(false);

  readonly selectedPreset = signal<AnalyticsDateRangePreset>('30D');
  readonly toDate = signal<string>(getOperatingDateLima());
  readonly fromDate = signal<string>(calculateDateOffset(getOperatingDateLima(), 30));

  constructor() {
    this.initEventSubscriptions();
  }

  private initEventSubscriptions(): void {
    if (this.invalidationBus) {
      this.invalidationBus.orderMutated$.subscribe((event) => {
        const activeStoreId = this.storeService.currentStore()?.id;
        if (activeStoreId && event.storeId !== activeStoreId) {
          return;
        }
        if (event.status === 'COMPLETED' || event.status === 'CANCELLED') {
          this.invalidateCache();
        }
      });

      this.invalidationBus.orderCreated$.subscribe((event) => {
        const activeStoreId = this.storeService.currentStore()?.id;
        if (activeStoreId && event.storeId !== activeStoreId) {
          return;
        }
        this.invalidateCache();
      });

      this.invalidationBus.reconnected$.subscribe(() => {
        this.invalidateCache();
      });

      this.invalidationBus.cacheInvalidated$.subscribe((event) => {
        if (event.module === 'ANALYTICS' || event.module === 'ALL') {
          this.invalidateCache();
        }
      });
    }
  }

  invalidateCache(): void {
    this.isOutdated.set(true);
  }

  /**
   * Carga el resumen analítico y financiero para el rango seleccionado.
   */
  loadSummary(from?: string, to?: string): Observable<MerchantAnalyticsSummaryResponse> {
    const activeStoreId = this.storeService.currentStore()?.id;
    const fromParam = from || this.fromDate();
    const toParam = to || this.toDate();

    let params = new HttpParams()
      .set('from', fromParam)
      .set('to', toParam);

    if (activeStoreId) {
      params = params.set('storeId', activeStoreId);
    }

    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<MerchantAnalyticsSummaryResponse>(`${this.baseUrl}/summary`, { params }).pipe(
      tap((res) => {
        this.summary.set(res);
        this.isOutdated.set(false);
      }),
      catchError((err) => {
        const message = err?.error?.message || 'Error al cargar las métricas financieras.';
        this.error.set(message);
        this.toastService.error(message);
        return throwError(() => err);
      }),
      finalize(() => {
        this.isLoading.set(false);
      })
    );
  }

  /**
   * Cambia el preset temporal ('7D', '30D', '90D') y recarga automáticamente.
   */
  setPreset(preset: '7D' | '30D' | '90D'): Observable<MerchantAnalyticsSummaryResponse> {
    const today = getOperatingDateLima();
    const days = preset === '7D' ? 7 : preset === '30D' ? 30 : 90;
    const start = calculateDateOffset(today, days);

    this.selectedPreset.set(preset);
    this.toDate.set(today);
    this.fromDate.set(start);

    return this.loadSummary(start, today);
  }

  /**
   * Establece un rango personalizado y recarga métricas.
   */
  setCustomRange(from: string, to: string): Observable<MerchantAnalyticsSummaryResponse> {
    this.selectedPreset.set('CUSTOM');
    this.fromDate.set(from);
    this.toDate.set(to);
    return this.loadSummary(from, to);
  }

  /**
   * Exporta el informe analítico oficial en formato PDF.
   */
  exportPdf(from?: string, to?: string): Observable<Blob> {
    const activeStoreId = this.storeService.currentStore()?.id;
    const fromParam = from || this.fromDate();
    const toParam = to || this.toDate();

    let params = new HttpParams()
      .set('from', fromParam)
      .set('to', toParam);

    if (activeStoreId) {
      params = params.set('storeId', activeStoreId);
    }

    this.isExportingPdf.set(true);

    return this.http
      .get(`${this.baseUrl}/export/pdf`, {
        params,
        responseType: 'blob',
      })
      .pipe(
        tap((blob) => {
          this.downloadBlob(blob, `jaldishop-reporte-${fromParam}-al-${toParam}.pdf`);
          this.toastService.success('Reporte PDF descargado con éxito.');
        }),
        catchError((err) => {
          const message = 'Error al exportar el reporte en PDF.';
          this.toastService.error(message);
          return throwError(() => err);
        }),
        finalize(() => {
          this.isExportingPdf.set(false);
        })
      );
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }
}

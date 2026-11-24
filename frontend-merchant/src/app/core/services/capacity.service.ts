import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  CapacityConfiguration,
  CapacityException,
  CreateCapacityConfigRequest,
  CreateCapacityExceptionRequest,
  MerchantStoreCapacityOccupancy,
  UpdateCapacityConfigRequest,
  UpdateCapacityExceptionRequest,
} from '../models/capacity.models';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import { OrderService } from './order.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { getLimaTimeParts, getOperatingDateLima } from '../utils/date.utils';
import { catchError, finalize, Observable, of, shareReplay, switchMap, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CapacityService {
  private readonly http = inject(HttpClient);
  private readonly storeService = inject(StoreService);
  private readonly stompService = inject(StompService, { optional: true });
  private readonly orderService = inject(OrderService, { optional: true });
  private readonly invalidationBus = inject(InvalidationBusService, { optional: true });

  private readonly baseUrl = `${environment.apiUrl}/capacity-configurations`;
  private readonly exceptionsUrl = `${environment.apiUrl}/capacity-exceptions`;
  private readonly merchantsUrl = `${environment.apiUrl}/merchants/stores`;

  private readonly isLoaded = signal<boolean>(false);
  private readonly areExceptionsLoaded = signal<boolean>(false);

  readonly exceptions = signal<CapacityException[]>([]);
  readonly isLoadingExceptions = signal<boolean>(false);
  readonly configurations = signal<CapacityConfiguration[]>([]);
  readonly isLoading = signal<boolean>(false);

  // Signals para ocupación real respaldada por backend (PR 5)
  readonly dailyOccupancy = signal<MerchantStoreCapacityOccupancy | null>(null);
  readonly isLoadingOccupancy = signal<boolean>(false);
  readonly occupancyError = signal<string | null>(null);
  readonly isOccupancyOutdated = signal<boolean>(false);

  // Computed unificados para consumidores (Dashboard, Orders, Sidebar) con verificación multitienda
  readonly todayOccupied = computed<number | null>(() => {
    const occ = this.dailyOccupancy();
    const currStoreId = this.storeService.currentStore()?.id;
    if (!occ || (currStoreId && occ.storeId !== currStoreId)) {
      return null;
    }
    return occ.occupiedCapacity;
  });

  readonly todayAvailable = computed<number | null>(() => {
    const occ = this.dailyOccupancy();
    const currStoreId = this.storeService.currentStore()?.id;
    if (!occ || (currStoreId && occ.storeId !== currStoreId)) {
      return null;
    }
    return occ.availableCapacity;
  });

  readonly todayOccupancyPercentage = computed<number | null>(() => {
    const occ = this.dailyOccupancy();
    const currStoreId = this.storeService.currentStore()?.id;
    if (!occ || (currStoreId && occ.storeId !== currStoreId)) {
      return null;
    }
    return occ.occupancyPercentage;
  });

  readonly isTodayClosedReal = computed<boolean>(() => {
    const occ = this.dailyOccupancy();
    const currStoreId = this.storeService.currentStore()?.id;
    if (!occ || (currStoreId && occ.storeId !== currStoreId)) {
      return this.isTodayClosed();
    }
    return occ.isClosed;
  });

  readonly todayEffectiveCapacityReal = computed<number>(() => {
    const occ = this.dailyOccupancy();
    const currStoreId = this.storeService.currentStore()?.id;
    if (!occ || (currStoreId && occ.storeId !== currStoreId)) {
      return this.todayEffectiveCapacity();
    }
    return occ.effectiveCapacity;
  });

  private inFlightOccupancy$: Observable<MerchantStoreCapacityOccupancy | null> | null = null;
  private inFlightOccupancyKey: string | null = null;
  private activeOccupancyRequestId = 0;

  constructor() {
    this.initRealtimeSubscriptions();
  }

  private initRealtimeSubscriptions(): void {
    if (this.invalidationBus) {
      this.invalidationBus.orderCreated$.subscribe((event) => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (!currentStoreId || event.storeId !== currentStoreId) {
          return;
        }
        this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
      });

      this.invalidationBus.orderMutated$.subscribe((event) => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (!currentStoreId || event.storeId !== currentStoreId) {
          return;
        }
        if (this.dailyOccupancy() !== null || this.isLoadingOccupancy()) {
          this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
        }
      });

      this.invalidationBus.capacityUpdated$.subscribe((event) => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (!currentStoreId || event.storeId !== currentStoreId) {
          return;
        }
        this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
      });

      this.invalidationBus.reconnected$.subscribe(() => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (currentStoreId) {
          this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
        }
      });

      this.invalidationBus.cacheInvalidated$.subscribe((event) => {
        if (event.module === 'CAPACITY' || event.module === 'ALL') {
          const currentStoreId = this.storeService.currentStore()?.id;
          if (currentStoreId) {
            this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
          }
        }
      });
    }

    if (this.stompService) {
      this.stompService.orderConfirmed$.subscribe((event) => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (!currentStoreId || event.storeId !== currentStoreId) {
          return;
        }
        this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
      });

      this.stompService.reconnected$.subscribe(() => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (currentStoreId) {
          this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
        }
      });
    }

    if (this.orderService) {
      this.orderService.orderMutated$.subscribe((event) => {
        const currentStoreId = this.storeService.currentStore()?.id;
        if (!currentStoreId || event.storeId !== currentStoreId) {
          return;
        }
        if (this.dailyOccupancy() !== null || this.isLoadingOccupancy()) {
          this.loadDailyOccupancy(currentStoreId, undefined, true).subscribe();
        }
      });
    }
  }

  readonly todayConfigurations = computed(() => {
    const todayDate = new Date();
    const parts = getLimaTimeParts(todayDate);
    const limaDate = new Date(Date.UTC(parts.year, parts.month - 1, parts.day, 12, 0, 0));
    const currentDayOfWeek = limaDate.getUTCDay();
    return this.configurations().filter(
      (c) => c.dayOfWeek === currentDayOfWeek && c.status === 'ACTIVE',
    );
  });

  readonly todayTotalCapacity = computed(() => {
    return this.todayConfigurations().reduce((acc, c) => acc + c.maxCapacity, 0);
  });

  readonly todayException = computed<CapacityException | null>(() => {
    const todayStr = this.getTodayDateString();
    const allToday = this.exceptions().filter((e) => e.serviceDate === todayStr && e.status === 'ACTIVE');
    if (allToday.length === 0) return null;

    // Priorizar excepción de todo el día si existe
    const allDayExc = allToday.find((e) => !e.startTime && !e.endTime);
    return allDayExc || allToday[0];
  });

  readonly isTodayClosed = computed<boolean>(() => {
    const exc = this.todayException();
    if (exc) {
      return exc.exceptionCapacity === 0;
    }
    return this.todayConfigurations().length === 0 || this.todayTotalCapacity() === 0;
  });

  readonly todayEffectiveCapacity = computed<number>(() => {
    const todayExc = this.todayException();
    if (todayExc) {
      return todayExc.exceptionCapacity;
    }
    return this.todayTotalCapacity();
  });

  getConfigurations(forceRefresh = false): Observable<CapacityConfiguration[]> {
    if (this.isLoaded() && !forceRefresh) {
      return of(this.configurations());
    }

    this.isLoading.set(true);
    return this.http.get<CapacityConfiguration[]>(this.baseUrl).pipe(
      tap({
        next: (config) => {
          this.configurations.set(config);
          this.isLoaded.set(true);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false),
      }),
    );
  }

  createConfiguration(request: CreateCapacityConfigRequest): Observable<CapacityConfiguration> {
    return this.http.post<CapacityConfiguration>(this.baseUrl, request).pipe(
      tap((created) => {
        this.configurations.update((list) => [...list, created]);
      }),
    );
  }

  updateConfiguration(
    id: string,
    request: UpdateCapacityConfigRequest,
  ): Observable<CapacityConfiguration> {
    return this.http.put<CapacityConfiguration>(`${this.baseUrl}/${id}`, request).pipe(
      tap((updated) => {
        this.configurations.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  activateConfiguration(id: string): Observable<CapacityConfiguration> {
    return this.http.patch<CapacityConfiguration>(`${this.baseUrl}/${id}/activate`, {}).pipe(
      tap((updated) => {
        this.configurations.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  deactivateConfiguration(id: string): Observable<CapacityConfiguration> {
    return this.http.patch<CapacityConfiguration>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(
      tap((updated) => {
        this.configurations.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  getExceptions(forceRefresh = false): Observable<CapacityException[]> {
    if (this.areExceptionsLoaded() && !forceRefresh) {
      return of(this.exceptions());
    }

    this.isLoadingExceptions.set(true);
    return this.http.get<CapacityException[]>(this.exceptionsUrl).pipe(
      tap({
        next: (list) => {
          this.exceptions.set(list);
          this.areExceptionsLoaded.set(true);
          this.isLoadingExceptions.set(false);
        },
        error: () => this.isLoadingExceptions.set(false),
      }),
    );
  }

  createException(request: CreateCapacityExceptionRequest): Observable<CapacityException> {
    return this.http.post<CapacityException>(this.exceptionsUrl, request).pipe(
      tap((created) => {
        this.exceptions.update((list) => [...list, created]);
      }),
    );
  }

  updateException(
    id: string,
    request: UpdateCapacityExceptionRequest,
  ): Observable<CapacityException> {
    return this.http.put<CapacityException>(`${this.exceptionsUrl}/${id}`, request).pipe(
      tap((updated) => {
        this.exceptions.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  activateException(id: string): Observable<CapacityException> {
    return this.http.patch<CapacityException>(`${this.exceptionsUrl}/${id}/activate`, {}).pipe(
      tap((updated) => {
        this.exceptions.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  deactivateException(id: string): Observable<CapacityException> {
    return this.http.patch<CapacityException>(`${this.exceptionsUrl}/${id}/deactivate`, {}).pipe(
      tap((updated) => {
        this.exceptions.update((list) => list.map((item) => (item.id === id ? updated : item)));
      }),
    );
  }

  deleteConfiguration(id: string): Observable<{ id: string; action: string; message: string }> {
    return this.http
      .delete<{ id: string; action: string; message: string }>(`${this.baseUrl}/${id}`)
      .pipe(
        tap(() => {
          this.configurations.update((list) => list.filter((item) => item.id !== id));
        }),
      );
  }

  deleteException(id: string): Observable<{ id: string; action: string; message: string }> {
    return this.http
      .delete<{ id: string; action: string; message: string }>(`${this.exceptionsUrl}/${id}`)
      .pipe(
        tap(() => {
          this.exceptions.update((list) => list.filter((item) => item.id !== id));
        }),
      );
  }

  // =========================================================================
  // Métodos de Ocupación Real para Comerciantes (PR 5)
  // =========================================================================

  getStoreCapacityOccupancy(
    storeId: string,
    date?: string,
  ): Observable<MerchantStoreCapacityOccupancy> {
    const url = `${this.merchantsUrl}/${storeId}/capacity/occupancy`;
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<MerchantStoreCapacityOccupancy>(url, { params });
  }

  loadDailyOccupancy(
    storeId?: string,
    date?: string,
    force = false,
  ): Observable<MerchantStoreCapacityOccupancy | null> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    const targetDate = date ?? getOperatingDateLima();

    if (!targetStoreId) {
      return this.storeService.getMyStore().pipe(
        switchMap((store) => {
          if (!store?.id) {
            this.isLoadingOccupancy.set(false);
            return of(null);
          }
          return this.fetchOccupancyDeduplicated(store.id, targetDate, force);
        }),
        catchError((err) => {
          this.isLoadingOccupancy.set(false);
          this.occupancyError.set(err?.message ?? 'Error al cargar capacidad');
          return of(null);
        }),
      );
    }

    return this.fetchOccupancyDeduplicated(targetStoreId, targetDate, force);
  }

  private fetchOccupancyDeduplicated(
    storeId: string,
    date: string,
    force: boolean,
  ): Observable<MerchantStoreCapacityOccupancy | null> {
    const cacheKey = `${storeId}_${date}`;
    if (!force && this.inFlightOccupancy$ && this.inFlightOccupancyKey === cacheKey) {
      return this.inFlightOccupancy$;
    }

    this.inFlightOccupancyKey = cacheKey;
    this.inFlightOccupancy$ = this.fetchOccupancyHttp(storeId, date).pipe(
      shareReplay({ bufferSize: 1, refCount: true }),
      finalize(() => {
        if (this.inFlightOccupancyKey === cacheKey) {
          this.inFlightOccupancy$ = null;
          this.inFlightOccupancyKey = null;
        }
      }),
    );
    return this.inFlightOccupancy$;
  }

  private fetchOccupancyHttp(
    storeId: string,
    date: string,
  ): Observable<MerchantStoreCapacityOccupancy | null> {
    const requestId = ++this.activeOccupancyRequestId;
    this.isLoadingOccupancy.set(true);

    return this.getStoreCapacityOccupancy(storeId, date).pipe(
      tap({
        next: (occupancy) => {
          const currentStoreId = this.storeService.currentStore()?.id;
          if (
            requestId !== this.activeOccupancyRequestId ||
            (currentStoreId && currentStoreId !== storeId)
          ) {
            return;
          }
          this.dailyOccupancy.set(occupancy);
          this.occupancyError.set(null);
          this.isLoadingOccupancy.set(false);
        },
        error: (err) => {
          if (requestId === this.activeOccupancyRequestId) {
            this.occupancyError.set(err?.message ?? 'Error al cargar ocupación');
            this.isLoadingOccupancy.set(false);
          }
        },
      }),
      catchError((err) => {
        if (requestId === this.activeOccupancyRequestId) {
          this.occupancyError.set(err?.message ?? 'Error al cargar ocupación');
          this.isLoadingOccupancy.set(false);
        }
        return of(null);
      }),
    );
  }

  clearCache(): void {
    this.configurations.set([]);
    this.isLoaded.set(false);
    this.exceptions.set([]);
    this.areExceptionsLoaded.set(false);
    this.dailyOccupancy.set(null);
    this.occupancyError.set(null);
    this.isLoadingOccupancy.set(false);
    this.inFlightOccupancy$ = null;
    this.inFlightOccupancyKey = null;
    this.activeOccupancyRequestId++;
  }

  private getTodayDateString(): string {
    return getOperatingDateLima();
  }
}

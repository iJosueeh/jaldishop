import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  CapacityConfiguration,
  CapacityException,
  CreateCapacityConfigRequest,
  CreateCapacityExceptionRequest,
  UpdateCapacityConfigRequest,
  UpdateCapacityExceptionRequest,
} from '../models/capacity.models';
import { Observable, of, tap } from 'rxjs';

@Service()
export class CapacityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/capacity-configurations`;
  private readonly exceptionsUrl = `${environment.apiUrl}/capacity-exceptions`;
  private readonly isLoaded = signal<boolean>(false);
  private readonly areExceptionsLoaded = signal<boolean>(false);

  readonly exceptions = signal<CapacityException[]>([]);
  readonly isLoadingExceptions = signal<boolean>(false);
  readonly configurations = signal<CapacityConfiguration[]>([]);
  readonly isLoading = signal<boolean>(false);

  readonly todayConfigurations = computed(() => {
    const isDay = new Date().getDay();
    const currentDayOfWeek = isDay === 0 ? 7 : isDay;
    return this.configurations().filter(
      (c) => c.dayOfWeek === currentDayOfWeek && c.status === 'ACTIVE',
    );
  });

  readonly todayTotalCapacity = computed(() => {
    return this.todayConfigurations().reduce((acc, c) => acc + c.maxCapacity, 0);
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

  clearCache(): void {
    this.configurations.set([]);
    this.isLoaded.set(false);
    this.exceptions.set([]);
    this.areExceptionsLoaded.set(false);
  }
}

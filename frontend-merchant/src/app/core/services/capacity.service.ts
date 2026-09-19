import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  CapacityConfiguration,
  CreateCapacityConfigRequest,
  UpdateCapacityConfigRequest,
} from '../models/capacity.models';
import { Observable, of, tap } from 'rxjs';

@Service()
export class CapacityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/capacity-configurations`;

  readonly configurations = signal<CapacityConfiguration[]>([]);
  readonly isLoading = signal<boolean>(false);

  private readonly isLoaded = signal<boolean>(false);

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

  clearCache(): void {
    this.configurations.set([]);
    this.isLoaded.set(false);
  }
}

import { computed, inject, Service, signal } from '@angular/core';
import {
  CreateStoreRequest,
  StoreResponse,
  UpdateStoreRequest,
} from '../models/store.models';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { catchError, Observable, of, tap, throwError } from 'rxjs';

@Service()
export class StoreService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/stores`;

  readonly currentStore = signal<StoreResponse | null>(null);
  readonly isLoading = signal<boolean>(false);
  private readonly isLoaded = signal<boolean>(false);

  readonly hasStore = computed(() => !!this.currentStore());
  readonly storeName = computed(() => this.currentStore()?.name ?? 'Mi tienda');
  readonly storeStatus = computed(() => this.currentStore()?.status ?? 'INACTIVE');

  getMyStore(forceRefresh = false): Observable<StoreResponse | null> {
    if (this.isLoaded() && !forceRefresh) {
      return of(this.currentStore());
    }

    this.isLoading.set(true);

    return this.http.get<StoreResponse>(`${this.baseUrl}/me`).pipe(
      tap((store) => {
        this.currentStore.set(store);
        this.isLoaded.set(true);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        if (error.status === 404) {
          this.currentStore.set(null);
          this.isLoaded.set(true);
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  createStore(request: CreateStoreRequest): Observable<StoreResponse> {
    this.isLoading.set(true);
    return this.http.post<StoreResponse>(this.baseUrl, request).pipe(
      tap((store) => {
        this.currentStore.set(store);
        this.isLoaded.set(true);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  updateStore(request: UpdateStoreRequest): Observable<StoreResponse> {
    this.isLoading.set(true);
    return this.http.put<StoreResponse>(`${this.baseUrl}/me`, request).pipe(
      tap((updateStore) => {
        this.currentStore.set(updateStore);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  clearStore(): void {
    this.currentStore.set(null);
    this.isLoaded.set(false);
  }
}

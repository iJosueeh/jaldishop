import { computed, inject, Service, signal } from '@angular/core';
import {
  CreateStoreRequest,
  StoreCategory,
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
  private readonly categoriesUrl = `${environment.apiUrl}/store-categories`;

  readonly currentStore = signal<StoreResponse | null>(null);
  readonly categories = signal<StoreCategory[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isLoadingCategories = signal<boolean>(false);
  private readonly isLoaded = signal<boolean>(false);

  readonly hasStore = computed(() => !!this.currentStore());
  readonly storeName = computed(() => this.currentStore()?.name ?? 'Mi tienda');
  readonly storeStatus = computed(() => this.currentStore()?.status ?? 'INACTIVE');

  getStoreCategories(forceRefresh = false): Observable<StoreCategory[]> {
    if (this.categories().length > 0 && !forceRefresh) {
      return of(this.categories());
    }

    this.isLoadingCategories.set(true);

    return this.http.get<StoreCategory[]>(this.categoriesUrl).pipe(
      tap((cats) => {
        this.categories.set(cats || []);
        this.isLoadingCategories.set(false);
      }),
      catchError((error) => {
        this.isLoadingCategories.set(false);
        return of([]);
      })
    );
  }

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

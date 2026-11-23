import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  InventoryResponse,
  UpdateStockRequest,
  UpdateThresholdRequest,
} from '../models/inventory.models';
import { TokenService } from './token-service';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import { OrderService } from './order.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { ToastService } from './toast.service';
import { catchError, finalize, Observable, of, shareReplay, tap, throwError } from 'rxjs';

export type InventoryFilterStatus = 'ALL' | 'CONFIGURED' | 'NOT_CONFIGURED' | 'NOT_TRACKED' | 'LOW_STOCK';

@Injectable({
  providedIn: 'root',
})
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly storeService = inject(StoreService, { optional: true });
  private readonly stompService = inject(StompService, { optional: true });
  private readonly orderService = inject(OrderService, { optional: true });
  private readonly invalidationBus = inject(InvalidationBusService, { optional: true });
  private readonly toastService = inject(ToastService, { optional: true });

  private readonly baseUrl = `${environment.apiUrl}/inventory`;

  // Estado Reactivo (Signals)
  readonly inventory = signal<InventoryResponse[]>([]);
  readonly lowStock = signal<InventoryResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isLoaded = signal<boolean>(false);
  readonly isLowStockLoading = signal<boolean>(false);
  readonly isLowStockLoaded = signal<boolean>(false);
  readonly updatingVariantId = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  // Visibilidad de la vista de inventario (/inventory)
  readonly isViewActive = signal<boolean>(false);

  // Filtros y Búsqueda para la Vista de Inventario
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<InventoryFilterStatus>('ALL');

  // Computados de Estado
  readonly lowStockCount = computed(() => this.lowStock().length);
  readonly totalCount = computed(() => this.inventory().length);
  readonly configuredCount = computed(() =>
    this.inventory().filter((item) => item.inventoryStatus === 'CONFIGURED').length
  );
  readonly notConfiguredCount = computed(() =>
    this.inventory().filter((item) => item.inventoryStatus === 'NOT_CONFIGURED').length
  );
  readonly notTrackedCount = computed(() =>
    this.inventory().filter((item) => item.inventoryStatus === 'NOT_TRACKED').length
  );

  // Inventario Filtrado para la UI
  readonly filteredInventory = computed(() => {
    const list = this.inventory();
    const query = this.searchQuery().trim().toLowerCase();
    const filter = this.statusFilter();

    return list.filter((item) => {
      // Filtro por estado
      if (filter === 'CONFIGURED' && item.inventoryStatus !== 'CONFIGURED') {
        return false;
      }
      if (filter === 'NOT_CONFIGURED' && item.inventoryStatus !== 'NOT_CONFIGURED') {
        return false;
      }
      if (filter === 'NOT_TRACKED' && item.inventoryStatus !== 'NOT_TRACKED') {
        return false;
      }
      if (filter === 'LOW_STOCK' && !item.isLowStock) {
        return false;
      }

      // Filtro por texto (nombre de producto, presentación o SKU)
      if (query.length > 0) {
        const prodMatch = item.productName?.toLowerCase().includes(query) ?? false;
        const presMatch = item.presentationName?.toLowerCase().includes(query) ?? false;
        const skuMatch = item.sku?.toLowerCase().includes(query) ?? false;
        return prodMatch || presMatch || skuMatch;
      }

      return true;
    });
  });

  // Deduplicación de Peticiones en Tránsito y Gestión de Sesión / Concurrencia
  private inFlightInventory$: Observable<InventoryResponse[]> | null = null;
  private inFlightLowStock$: Observable<InventoryResponse[]> | null = null;
  private currentUserId: string | null = null;
  private requestSequence = 0;
  private lowStockRequestSequence = 0;
  private hasPendingRefresh = false;
  private hasPendingLowStockRefresh = false;

  constructor() {
    this.initEventSubscriptions();
  }

  /**
   * Conecta las suscripciones a eventos de órdenes y WebSockets para sincronizar inventario reactivamente.
   */
  private initEventSubscriptions(): void {
    if (this.invalidationBus) {
      this.invalidationBus.orderCreated$.subscribe((event) => {
        const currStoreId = this.storeService?.currentStore()?.id;
        if (currStoreId && event.storeId !== currStoreId) {
          return;
        }
        this.handleInventoryInvalidation();
      });

      this.invalidationBus.orderMutated$.subscribe((event) => {
        const currStoreId = this.storeService?.currentStore()?.id;
        if (currStoreId && event.storeId !== currStoreId) {
          return;
        }
        if (
          event.status === 'CANCELLED' ||
          event.status === 'IN_PREPARATION' ||
          event.status === 'COMPLETED'
        ) {
          this.handleInventoryInvalidation();
        }
      });

      this.invalidationBus.inventoryChanged$.subscribe((event) => {
        const currStoreId = this.storeService?.currentStore()?.id;
        if (currStoreId && event.storeId !== currStoreId) {
          return;
        }
        this.handleInventoryInvalidation();
      });

      this.invalidationBus.reconnected$.subscribe(() => {
        this.handleInventoryInvalidation();
      });

      this.invalidationBus.cacheInvalidated$.subscribe((event) => {
        if (event.module === 'INVENTORY' || event.module === 'ALL') {
          this.handleInventoryInvalidation();
        }
      });
    }

    if (this.stompService) {
      this.stompService.orderConfirmed$.subscribe((event) => {
        const currStoreId = this.storeService?.currentStore()?.id;
        if (currStoreId && event.storeId !== currStoreId) {
          return;
        }
        this.handleInventoryInvalidation();
      });

      this.stompService.reconnected$.subscribe(() => {
        this.handleInventoryInvalidation();
      });
    }

    if (this.orderService) {
      this.orderService.orderMutated$.subscribe((event) => {
        const currStoreId = this.storeService?.currentStore()?.id;
        if (currStoreId && event.storeId !== currStoreId) {
          return;
        }
        // Mutaciones que impactan existencias o reservas (RN-ORD-13: cancelaciones, preparación y cierre)
        if (
          event.status === 'CANCELLED' ||
          event.status === 'IN_PREPARATION' ||
          event.status === 'COMPLETED'
        ) {
          this.handleInventoryInvalidation();
        }
      });
    }
  }

  /**
   * Actualiza el indicador de visibilidad de la vista de inventario.
   */
  setViewActive(active: boolean): void {
    this.isViewActive.set(active);
  }

  /**
   * Invalida la validez de la caché en memoria sin blanquear la vista de usuario.
   */
  invalidateCache(): void {
    this.isLoaded.set(false);
    this.isLowStockLoaded.set(false);
  }

  /**
   * Reacciona ante eventos de dominio de pedidos para refrescar o invalidar inventario según la visibilidad.
   */
  handleInventoryInvalidation(): void {
    if (!this.tokenService.hasValidToken()) {
      return;
    }

    // 1. Si la vista de inventario está activa, refrescamos el inventario completo inmediatamente
    if (this.isViewActive()) {
      if (this.inFlightInventory$) {
        this.hasPendingRefresh = true;
        return;
      }
      this.getStoreInventory(true).subscribe({ error: () => {} });
      return;
    }

    // 2. Si la vista de inventario NO está activa, invalidamos la caché general
    this.isLoaded.set(false);

    // 3. Si las alertas de stock bajo están cargadas en memoria (sidebar o dashboard), las refrescamos
    if (this.isLowStockLoaded() || this.lowStock().length > 0) {
      if (this.inFlightLowStock$) {
        this.hasPendingLowStockRefresh = true;
        return;
      }
      this.getLowStockInventory(true).subscribe({ error: () => {} });
    }
  }

  /**
   * Garantiza la coherencia de la sesión activa del usuario.
   * Si el token no existe, expiró o cambió de usuario, invalida y limpia el caché de inmediato.
   */
  private ensureActiveSession(): string | null {
    const activeUserId = this.tokenService.getUserId();
    if (!activeUserId || !this.tokenService.hasValidToken()) {
      if (this.currentUserId !== null || this.inventory().length > 0 || this.lowStock().length > 0) {
        this.clearCache();
      }
      return null;
    }

    if (this.currentUserId !== null && this.currentUserId !== activeUserId) {
      this.clearCache();
    }

    this.currentUserId = activeUserId;
    return activeUserId;
  }

  /**
   * 1. GET /api/v1/inventory
   * Consulta el inventario general de todas las variantes de la tienda.
   * Aplica estrategia Cache-First y deduplica llamadas concurrentes en tránsito.
   */
  getStoreInventory(forceRefresh = false): Observable<InventoryResponse[]> {
    const activeUserId = this.ensureActiveSession();
    if (!activeUserId) {
      return of([]);
    }

    const requestSeq = ++this.requestSequence;
    const requestUserId = activeUserId;
    const requestToken = this.tokenService.getToken();

    if (this.isLoaded() && !forceRefresh) {
      return of(this.inventory());
    }

    if (this.inFlightInventory$) {
      if (forceRefresh) {
        this.hasPendingRefresh = true;
      }
      return this.inFlightInventory$;
    }

    this.isLoading.set(true);
    this.error.set(null);

    this.inFlightInventory$ = this.http.get<InventoryResponse[]>(this.baseUrl).pipe(
      tap((data) => {
        // Ignorar respuesta si la sesión cambió o expiró mientras la petición estaba en tránsito
        if (
          requestSeq !== this.requestSequence ||
          this.currentUserId !== requestUserId ||
          this.tokenService.getToken() !== requestToken ||
          !this.tokenService.hasValidToken()
        ) {
          return;
        }

        const items = data || [];
        this.inventory.set(items);
        this.isLoaded.set(true);
        this.error.set(null);

        // Prellenar lowStock coherentemente a partir del inventario completo
        this.lowStock.set(items.filter((item) => item.isLowStock));
        this.isLowStockLoaded.set(true);
      }),
      catchError((err) => {
        if (
          requestSeq === this.requestSequence &&
          this.currentUserId === requestUserId &&
          this.tokenService.hasValidToken()
        ) {
          const errorMsg = err?.message || 'Error al consultar inventario de la tienda.';
          this.error.set(errorMsg);
        }
        return throwError(() => err);
      }),
      finalize(() => {
        this.isLoading.set(false);
        this.inFlightInventory$ = null;

        if (this.hasPendingRefresh) {
          this.hasPendingRefresh = false;
          if (this.tokenService.hasValidToken()) {
            if (this.isViewActive()) {
              this.getStoreInventory(true).subscribe({ error: () => {} });
            } else {
              this.isLoaded.set(false);
            }
          }
        }
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    return this.inFlightInventory$;
  }

  /**
   * 2. GET /api/v1/inventory/variants/{variantId}
   * Consulta el estado de inventario de una variante específica.
   * Sincroniza la respuesta en el estado local de variantes.
   */
  getVariantInventory(variantId: string): Observable<InventoryResponse> {
    const activeUserId = this.ensureActiveSession();
    if (!activeUserId) {
      return of({} as InventoryResponse);
    }
    const requestUserId = activeUserId;
    const requestToken = this.tokenService.getToken();

    return this.http.get<InventoryResponse>(`${this.baseUrl}/variants/${variantId}`).pipe(
      tap((item) => {
        if (
          this.currentUserId !== requestUserId ||
          this.tokenService.getToken() !== requestToken ||
          !this.tokenService.hasValidToken()
        ) {
          return;
        }
        if (item) {
          this.upsertLocalItem(item);
        }
      }),
      catchError((err) => {
        return throwError(() => err);
      })
    );
  }

  /**
   * 3. GET /api/v1/inventory/low-stock
   * Consulta las variantes que se encuentran en umbral de alerta de stock bajo.
   * Aplica estrategia Cache-First y deduplicación en tránsito.
   */
  getLowStockInventory(forceRefresh = false): Observable<InventoryResponse[]> {
    const activeUserId = this.ensureActiveSession();
    if (!activeUserId) {
      return of([]);
    }

    const requestSeq = ++this.lowStockRequestSequence;
    const requestUserId = activeUserId;
    const requestToken = this.tokenService.getToken();

    if (this.isLowStockLoaded() && !forceRefresh) {
      return of(this.lowStock());
    }

    if (this.inFlightLowStock$) {
      if (forceRefresh) {
        this.hasPendingLowStockRefresh = true;
      }
      return this.inFlightLowStock$;
    }

    this.isLowStockLoading.set(true);

    this.inFlightLowStock$ = this.http.get<InventoryResponse[]>(`${this.baseUrl}/low-stock`).pipe(
      tap((data) => {
        if (
          requestSeq !== this.lowStockRequestSequence ||
          this.currentUserId !== requestUserId ||
          this.tokenService.getToken() !== requestToken ||
          !this.tokenService.hasValidToken()
        ) {
          return;
        }

        const lowStockItems = data || [];
        this.lowStock.set(lowStockItems);
        this.isLowStockLoaded.set(true);

        // Si el inventario general ya estaba cargado, sincronizar coherentemente los datos actualizados
        if (this.isLoaded()) {
          const lowStockMap = new Map(lowStockItems.map((i) => [i.variantId, i]));
          this.inventory.update((current) =>
            current.map((item) => {
              const freshLowStock = lowStockMap.get(item.variantId);
              if (freshLowStock) {
                return freshLowStock;
              }
              return {
                ...item,
                isLowStock: false,
              };
            })
          );
        }
      }),
      catchError((err) => {
        return throwError(() => err);
      }),
      finalize(() => {
        this.isLowStockLoading.set(false);
        this.inFlightLowStock$ = null;

        if (this.hasPendingLowStockRefresh) {
          this.hasPendingLowStockRefresh = false;
          if (
            this.tokenService.hasValidToken() &&
            (this.isLowStockLoaded() || this.lowStock().length > 0)
          ) {
            this.getLowStockInventory(true).subscribe({ error: () => {} });
          }
        }
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    return this.inFlightLowStock$;
  }

  /**
   * 4. PUT /api/v1/inventory/variants/{variantId}/stock
   * Configura o ajusta la cantidad física absoluta de una variante.
   * Aplica actualización optimista inmediata en 0ms y rollback transparente con retry si la red falla.
   */
  updateStock(
    variantId: string,
    request: UpdateStockRequest | number
  ): Observable<InventoryResponse> {
    const activeUserId = this.ensureActiveSession();
    if (!activeUserId) {
      return of({} as InventoryResponse);
    }
    const requestUserId = activeUserId;
    const requestToken = this.tokenService.getToken();

    const payload: UpdateStockRequest =
      typeof request === 'number' ? { quantity: request } : request;

    this.updatingVariantId.set(variantId);

    // 0ms Optimistic UI: captura snapshot y actualiza señal local inmediatamente
    const previousInventory = this.inventory();
    const previousLowStock = this.lowStock();
    const targetItem = previousInventory.find((i) => i.variantId === variantId);

    if (targetItem) {
      const newQty = payload.quantity;
      const reserved = targetItem.reservedQuantity ?? 0;
      const available = Math.max(0, newQty - reserved);
      const isLowStock =
        targetItem.lowStockThreshold !== null && targetItem.lowStockThreshold !== undefined
          ? newQty <= targetItem.lowStockThreshold
          : false;

      const optimisticItem: InventoryResponse = {
        ...targetItem,
        physicalQuantity: newQty,
        availableQuantity: available,
        isLowStock,
        inventoryStatus: 'CONFIGURED',
      };

      this.upsertLocalItem(optimisticItem);
    }

    return this.http
      .put<InventoryResponse>(`${this.baseUrl}/variants/${variantId}/stock`, payload)
      .pipe(
        tap((updatedItem) => {
          if (
            this.currentUserId !== requestUserId ||
            this.tokenService.getToken() !== requestToken ||
            !this.tokenService.hasValidToken()
          ) {
            return;
          }
          if (updatedItem) {
            this.upsertLocalItem(updatedItem);
          }
        }),
        catchError((err) => {
          // Rollback transparente de estado en memoria
          this.inventory.set(previousInventory);
          this.lowStock.set(previousLowStock);

          this.toastService?.errorWithAction(
            'No se pudo sincronizar el stock con el servidor.',
            {
              label: 'Reintentar',
              run: () => {
                this.updateStock(variantId, payload).subscribe({ error: () => {} });
              },
            },
            'Error de Sincronización',
          );

          return throwError(() => err);
        }),
        finalize(() => {
          if (this.updatingVariantId() === variantId) {
            this.updatingVariantId.set(null);
          }
        })
      );
  }

  /**
   * 5. PATCH /api/v1/inventory/variants/{variantId}/threshold
   * Configura o actualiza el umbral de alerta de stock bajo (o null para desactivarlo).
   * Aplica actualización optimista inmediata en 0ms y rollback transparente con retry si la red falla.
   */
  updateThreshold(
    variantId: string,
    request: UpdateThresholdRequest | number | null
  ): Observable<InventoryResponse> {
    const activeUserId = this.ensureActiveSession();
    if (!activeUserId) {
      return of({} as InventoryResponse);
    }
    const requestUserId = activeUserId;
    const requestToken = this.tokenService.getToken();

    const payload: UpdateThresholdRequest =
      typeof request === 'object' && request !== null
        ? request
        : { lowStockThreshold: request };

    this.updatingVariantId.set(variantId);

    // 0ms Optimistic UI: captura snapshot y actualiza señal local inmediatamente
    const previousInventory = this.inventory();
    const previousLowStock = this.lowStock();
    const targetItem = previousInventory.find((i) => i.variantId === variantId);

    if (targetItem) {
      const newThreshold = payload.lowStockThreshold;
      const physical = targetItem.physicalQuantity ?? 0;
      const isLowStock =
        newThreshold !== null && newThreshold !== undefined
          ? physical <= newThreshold
          : false;

      const optimisticItem: InventoryResponse = {
        ...targetItem,
        lowStockThreshold: newThreshold,
        isLowStock,
      };

      this.upsertLocalItem(optimisticItem);
    }

    return this.http
      .patch<InventoryResponse>(`${this.baseUrl}/variants/${variantId}/threshold`, payload)
      .pipe(
        tap((updatedItem) => {
          if (
            this.currentUserId !== requestUserId ||
            this.tokenService.getToken() !== requestToken ||
            !this.tokenService.hasValidToken()
          ) {
            return;
          }
          if (updatedItem) {
            this.upsertLocalItem(updatedItem);
          }
        }),
        catchError((err) => {
          // Rollback transparente de estado en memoria
          this.inventory.set(previousInventory);
          this.lowStock.set(previousLowStock);

          this.toastService?.errorWithAction(
            'No se pudo sincronizar el umbral de existencias con el servidor.',
            {
              label: 'Reintentar',
              run: () => {
                this.updateThreshold(variantId, payload).subscribe({ error: () => {} });
              },
            },
            'Error de Sincronización',
          );

          return throwError(() => err);
        }),
        finalize(() => {
          if (this.updatingVariantId() === variantId) {
            this.updatingVariantId.set(null);
          }
        })
      );
  }

  /**
   * Actualiza o inserta un registro de inventario en el estado en memoria,
   * manteniendo la sincronización entre inventory() y lowStock().
   */
  private upsertLocalItem(item: InventoryResponse): void {
    if (this.isLoaded()) {
      this.inventory.update((current) => {
        const index = current.findIndex((i) => i.variantId === item.variantId);
        if (index >= 0) {
          const next = [...current];
          next[index] = item;
          return next;
        }
        return [...current, item];
      });
    }

    // Sincronización reactiva bidireccional de la señal lowStock
    this.lowStock.update((current) => {
      const exists = current.some((i) => i.variantId === item.variantId);
      if (item.isLowStock) {
        if (exists) {
          return current.map((i) => (i.variantId === item.variantId ? item : i));
        }
        return [...current, item];
      } else if (exists) {
        return current.filter((i) => i.variantId !== item.variantId);
      }
      return current;
    });
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setStatusFilter(filter: InventoryFilterStatus): void {
    this.statusFilter.set(filter);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('ALL');
  }

  /**
   * Limpia toda la memoria reactiva, estados de carga y peticiones en tránsito.
   * Usado en logout o alternancia de usuario autenticado.
   */
  clearCache(): void {
    this.requestSequence++;
    this.lowStockRequestSequence++;
    this.hasPendingRefresh = false;
    this.hasPendingLowStockRefresh = false;
    this.inFlightInventory$ = null;
    this.inFlightLowStock$ = null;
    this.currentUserId = null;

    this.inventory.set([]);
    this.lowStock.set([]);
    this.isLoading.set(false);
    this.isLoaded.set(false);
    this.isLowStockLoading.set(false);
    this.isLowStockLoaded.set(false);
    this.updatingVariantId.set(null);
    this.error.set(null);
    this.searchQuery.set('');
    this.statusFilter.set('ALL');
  }
}

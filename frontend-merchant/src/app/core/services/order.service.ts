import { computed, DestroyRef, inject, Injectable, OnDestroy, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  ChangeOrderStatusRequest,
  MerchantOrder,
  MerchantOrderDetailResponse,
  MerchantOrderPageResponse,
  MerchantOrderStatusCounts,
  OrderFilterTab,
  OrderStatus,
} from '../models/order.models';
import { DashboardPriorityOrder } from '../models/dashboard.models';
import {
  mapDetailToDashboardPriorityOrder,
  mapDetailToMerchantOrder,
  mapSummaryToMerchantOrder,
} from '../adapters/order.adapter';
import { ToastService } from './toast.service';
import { SettingsService } from './settings.service';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import { WhatsAppTemplateId } from '../models/settings.models';
import { getMsUntilNextMidnightLima, getOperatingDateLima } from '../utils/date.utils';
import {
  catchError,
  finalize,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class OrderService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly toastService = inject(ToastService);
  private readonly settingsService = inject(SettingsService);
  private readonly storeService = inject(StoreService);
  private readonly stompService = inject(StompService);
  private readonly destroyRef = inject(DestroyRef, { optional: true });

  private readonly baseUrl = `${environment.apiUrl}/merchants/stores`;

  constructor() {
    this.initRealtimeSubscriptions();
    this.scheduleMidnightCheck();
    this.destroyRef?.onDestroy(() => this.ngOnDestroy());
  }

  // Listado y Paginación
  readonly orders = signal<MerchantOrder[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isLoaded = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0); // 0-based en backend
  readonly pageSize = signal<number>(10);

  // Filtros y Búsqueda
  readonly activeTab = signal<OrderFilterTab>('ALL');
  readonly searchQuery = signal<string>('');
  readonly channelFilter = signal<string>('ALL');

  // Detalle y Drawer
  readonly selectedOrder = signal<MerchantOrder | null>(null);
  readonly selectedOrderDetail = signal<DashboardPriorityOrder | null>(null);
  readonly isDetailLoading = signal<boolean>(false);
  readonly detailError = signal<string | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);
  readonly isCreateModalOpen = signal<boolean>(false);

  // Control de Concurrencia, Mutaciones en Vuelo e Incertidumbre
  private activeListRequestId = 0;
  private activeCountsRequestId = 0;
  private activeTodayCountsRequestId = 0;
  private activeDetailOrderId: string | null = null;
  readonly updatingOrderIds = signal<Set<string>>(new Set<string>());
  readonly pendingVerificationOrderIds = signal<Set<string>>(new Set<string>());

  // Estado de Contadores Históricos (sin fecha)
  readonly orderCounts = signal<MerchantOrderStatusCounts | null>(null);
  readonly isCountsLoading = signal<boolean>(false);
  readonly countsError = signal<string | null>(null);

  // Estado de Contadores Diarios Operativos (fecha operativa America/Lima)
  readonly todayOrderCounts = signal<MerchantOrderStatusCounts | null>(null);
  readonly todayOperatingDate = signal<string | null>(null);
  readonly isTodayCountsLoading = signal<boolean>(false);
  readonly todayCountsError = signal<string | null>(null);

  // Fecha operativa actual reactiva en Lima
  readonly currentOperatingDate = signal<string>(getOperatingDateLima());
  private midnightTimerId: any = null;

  // Control de solicitudes concurrentes en vuelo (deduplicación)
  private inFlightCounts$: Observable<MerchantOrderStatusCounts | null> | null = null;
  private inFlightCountsStoreId: string | null = null;
  private inFlightTodayCounts$: Observable<MerchantOrderStatusCounts | null> | null = null;
  private inFlightTodayCountsKey: string | null = null;

  // Indicador de obsolescencia de contadores de hoy (por error HTTP o cambio de fecha operativa)
  readonly isTodayOutdated = computed(() => {
    if (this.todayCountsError()) return true;
    const recordedDate = this.todayOperatingDate();
    if (!recordedDate) return false;
    return recordedDate !== this.currentOperatingDate();
  });

  // KPIs derivados exclusivamente de los contadores agregados reales del backend
  readonly totalOrdersCount = computed(() => this.orderCounts()?.all ?? 0);
  readonly confirmedCount = computed(() => this.orderCounts()?.confirmed ?? 0);
  readonly inPrepCount = computed(() => this.orderCounts()?.inPreparation ?? 0);
  readonly readyCount = computed(() => this.orderCounts()?.ready ?? 0);
  readonly completedCount = computed(() => this.orderCounts()?.completed ?? 0);

  readonly remainingDeliveriesCount = computed(() => {
    return this.inPrepCount() + this.readyCount();
  });

  // Pedidos activos del día operativo actual (para Badge de Sidebar: confirmed + inPreparation + ready)
  readonly activeOrdersTodayCount = computed(() => {
    const counts = this.todayOrderCounts();
    if (!counts) return 0;
    return counts.confirmed + counts.inPreparation + counts.ready;
  });

  // Formateadores reactivos para los contadores de las pestañas de Orders
  readonly tabCounts = computed(() => {
    const counts = this.orderCounts();
    const loading = this.isCountsLoading();
    const error = this.countsError();

    const format = (value?: number): number | string => {
      if (error && !counts) return '—';
      if (loading && !counts) return '...';
      return value !== undefined ? value : '—';
    };

    return {
      all: format(counts?.all),
      confirmed: format(counts?.confirmed),
      inPreparation: format(counts?.inPreparation),
      ready: format(counts?.ready),
      completed: format(counts?.completed),
      cancelled: format(counts?.cancelled),
    };
  });

  readonly urgentOrder = computed<MerchantOrder | null>(() => {
    return this.orders().find((o) => o.isUrgent && o.status !== 'COMPLETED') || null;
  });

  readonly averagePrepTimeMin = computed<number | null>(() => {
    return null;
  });

  // Lista Filtrada Reactiva (local a la página cargada)
  readonly filteredOrders = computed(() => {
    const tab = this.activeTab();
    const query = this.searchQuery().toLowerCase().trim();
    const channel = this.channelFilter();

    return this.orders().filter((order) => {
      // 1. Filtro local por estado
      if (tab === 'CONFIRMED' && order.status !== 'CONFIRMED') return false;
      if (tab === 'IN_PREPARATION' && order.status !== 'IN_PREPARATION') return false;
      if (tab === 'READY' && order.status !== 'READY') return false;
      if (tab === 'COMPLETED' && order.status !== 'COMPLETED') return false;

      // 2. Filtro local por canal: inactivo en Fase 1 mientras el backend no soporte canales.
      // Garantiza que ningún valor residual pueda generar resultados ambiguos o filtrar pedidos válidos.

      // 3. Búsqueda local en la página actual
      if (query) {
        const matchNumber = order.orderNumber.toLowerCase().includes(query);
        const matchCustomer = order.customerName.toLowerCase().includes(query);
        const matchPhone = order.customerPhone?.includes(query);
        const matchItems = order.items.some((i) => i.name.toLowerCase().includes(query));
        if (!matchNumber && !matchCustomer && !matchPhone && !matchItems) {
          return false;
        }
      }

      return true;
    });
  });

  // Estados Vacíos
  readonly isEmptyOrders = computed(() => {
    return (
      !this.isLoading() &&
      this.isLoaded() &&
      this.totalElements() === 0 &&
      this.activeTab() === 'ALL'
    );
  });

  readonly isFilterEmpty = computed(() => {
    return (
      !this.isLoading() &&
      this.isLoaded() &&
      this.totalElements() === 0 &&
      this.activeTab() !== 'ALL'
    );
  });

  readonly isSearchEmpty = computed(() => {
    return (
      !this.isLoading() &&
      this.isLoaded() &&
      this.orders().length > 0 &&
      this.filteredOrders().length === 0
    );
  });

  isUpdating(orderId: string): boolean {
    return this.updatingOrderIds().has(orderId);
  }

  private clearUpdating(orderId: string): void {
    this.updatingOrderIds.update((set) => {
      const next = new Set(set);
      next.delete(orderId);
      return next;
    });
  }

  isPendingVerification(orderId: string): boolean {
    return this.pendingVerificationOrderIds().has(orderId);
  }

  private clearPendingVerification(orderId: string): void {
    this.pendingVerificationOrderIds.update((set) => {
      const next = new Set(set);
      next.delete(orderId);
      return next;
    });
  }

  private setPendingVerification(orderId: string): void {
    this.pendingVerificationOrderIds.update((set) => new Set(set).add(orderId));
  }

  private initRealtimeSubscriptions(): void {
    this.stompService.orderConfirmed$.subscribe((event) => {
      const currentStoreId = this.storeService.currentStore()?.id;
      if (!currentStoreId || event.storeId !== currentStoreId) {
        return; // Ignorar eventos de tiendas inactivas o ajenas
      }
      this.toastService.info(`Nuevo pedido recibido: ${event.orderNumber}`);
      this.loadOrders(this.currentPage(), this.pageSize(), this.activeTab()).subscribe();
      if (this.orderCounts() !== null || this.isCountsLoading() || this.countsError() !== null) {
        this.loadOrderCounts(currentStoreId, true).subscribe();
      }
      if (
        this.todayOrderCounts() !== null ||
        this.isTodayCountsLoading() ||
        this.todayCountsError() !== null ||
        this.isTodayOutdated()
      ) {
        this.loadTodayOrderCounts(currentStoreId, true).subscribe();
      }
    });

    this.stompService.reconnected$.subscribe(() => {
      const currentStoreId = this.storeService.currentStore()?.id;
      if (currentStoreId) {
        this.loadOrders(this.currentPage(), this.pageSize(), this.activeTab()).subscribe();
        if (this.orderCounts() !== null || this.isCountsLoading() || this.countsError() !== null) {
          this.loadOrderCounts(currentStoreId, true).subscribe();
        }
        if (
          this.todayOrderCounts() !== null ||
          this.isTodayCountsLoading() ||
          this.todayCountsError() !== null ||
          this.isTodayOutdated()
        ) {
          this.loadTodayOrderCounts(currentStoreId, true).subscribe();
        }
      }
    });
  }

  loadOrders(
    page = this.currentPage(),
    size = this.pageSize(),
    status = this.activeTab(),
    storeId?: string,
  ): Observable<MerchantOrderPageResponse | null> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      return this.storeService.getMyStore().pipe(
        switchMap((store) => {
          if (!store?.id) {
            this.isLoading.set(false);
            this.isLoaded.set(true);
            return of(null);
          }
          return this.fetchOrdersHttp(store.id, page, size, status);
        }),
        catchError((err) => {
          this.handleHttpError(err);
          return of(null);
        }),
      );
    }

    return this.fetchOrdersHttp(targetStoreId, page, size, status);
  }

  private fetchOrdersHttp(
    storeId: string,
    page: number,
    size: number,
    status: OrderFilterTab,
  ): Observable<MerchantOrderPageResponse | null> {
    const requestId = ++this.activeListRequestId;
    this.isLoading.set(true);
    this.error.set(null);

    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    const url = `${this.baseUrl}/${storeId}/orders`;

    return this.http.get<MerchantOrderPageResponse>(url, { params }).pipe(
      tap((response) => {
        if (requestId !== this.activeListRequestId) {
          return; // Descartar respuesta obsoleta
        }
        const mapped = (response.content || []).map(mapSummaryToMerchantOrder);
        // Protección de mutaciones en vuelo: preservar el estado en memoria de pedidos en actualización
        const currentOrdersMap = new Map(this.orders().map((o) => [o.id, o]));
        const merged = mapped.map((order) => {
          if (this.updatingOrderIds().has(order.id)) {
            const current = currentOrdersMap.get(order.id);
            if (current) return current;
          }
          return order;
        });
        this.orders.set(merged);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.currentPage.set(response.page);
        this.pageSize.set(response.size);
        this.isLoading.set(false);
        this.isLoaded.set(true);
        this.error.set(null);
      }),
      catchError((err) => {
        if (requestId === this.activeListRequestId) {
          this.isLoading.set(false);
          this.isLoaded.set(true);
          this.handleHttpError(err);
        }
        return of(null);
      }),
    );
  }

  getOrderDetail(storeId: string, orderId: string): Observable<MerchantOrderDetailResponse | null> {
    this.activeDetailOrderId = orderId;
    this.isDetailLoading.set(true);
    this.detailError.set(null);

    const url = `${this.baseUrl}/${storeId}/orders/${orderId}`;

    return this.http.get<MerchantOrderDetailResponse>(url).pipe(
      tap((detail) => {
        if (this.activeDetailOrderId !== orderId) {
          return; // Descartar respuesta obsoleta
        }
        const mapped = mapDetailToDashboardPriorityOrder(detail);
        this.selectedOrderDetail.set(mapped);
        this.isDetailLoading.set(false);
      }),
      catchError((err) => {
        if (this.activeDetailOrderId === orderId) {
          this.isDetailLoading.set(false);
          const errorMsg =
            err?.status === 404
              ? 'Pedido no encontrado en esta tienda'
              : err?.status === 403
                ? 'No tienes permiso para ver este pedido'
                : 'Error al obtener el detalle del pedido';
          this.detailError.set(errorMsg);
          this.toastService.error(errorMsg);
        }
        return of(null);
      }),
    );
  }

  updateOrderStatus(
    orderId: string,
    newStatus: OrderStatus,
    storeId?: string,
  ): Observable<MerchantOrderDetailResponse | null> {
    if (this.isUpdating(orderId) || this.isPendingVerification(orderId)) {
      return of(null); // Evitar solicitudes duplicadas o clics ciegos mientras esté en incertidumbre
    }

    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      this.toastService.error('No se pudo determinar la tienda activa.');
      return of(null);
    }

    this.updatingOrderIds.update((set) => new Set(set).add(orderId));

    const url = `${this.baseUrl}/${targetStoreId}/orders/${orderId}/status`;
    const body: ChangeOrderStatusRequest = { status: newStatus };

    return this.http.patch<MerchantOrderDetailResponse>(url, body).pipe(
      tap((response) => {
        this.clearUpdating(orderId);
        this.clearPendingVerification(orderId);
        this.applyUpdatedOrder(response, targetStoreId);
        this.toastService.success(`Pedido actualizado a ${this.getStatusLabel(newStatus)}.`);
      }),
      catchError((err) => {
        this.clearUpdating(orderId);
        this.handlePatchError(err, targetStoreId, orderId, newStatus);
        return of(null);
      }),
    );
  }

  private applyUpdatedOrder(detail: MerchantOrderDetailResponse, storeId: string): void {
    // Invalidar cualquier petición de listado en vuelo anterior para que no sobreescriba esta actualización confirmada
    this.activeListRequestId++;
    const updatedMerchantOrder = mapDetailToMerchantOrder(detail);

    // 1. Sincronizar el Drawer si corresponde a este pedido
    if (this.selectedOrder()?.id === detail.id) {
      this.selectedOrder.set(updatedMerchantOrder);
    }
    if (this.selectedOrderDetail()?.id === detail.id) {
      this.selectedOrderDetail.set(mapDetailToDashboardPriorityOrder(detail));
    }

    // 2. Comprobar si el pedido aún pertenece al filtro activo
    const tab = this.activeTab();
    if (tab !== 'ALL' && detail.status !== tab) {
      // El pedido dejó de pertenecer al filtro activo: recargar la página actual del filtro
      this.loadOrders(this.currentPage(), this.pageSize(), tab, storeId).subscribe();
    } else {
      // El pedido sigue perteneciendo al filtro o estamos en 'ALL': actualizar en memoria
      this.orders.update((list) =>
        list.map((o) => (o.id === detail.id ? updatedMerchantOrder : o)),
      );
    }

    // 3. Refrescar contadores reales confirmados forzando petición fresca tras mutación
    if (this.orderCounts() !== null || this.isCountsLoading() || this.countsError() !== null) {
      this.loadOrderCounts(storeId, true).subscribe();
    }
    if (
      this.todayOrderCounts() !== null ||
      this.isTodayCountsLoading() ||
      this.todayCountsError() !== null ||
      this.isTodayOutdated()
    ) {
      this.loadTodayOrderCounts(storeId, true).subscribe();
    }
  }

  private handlePatchError(
    err: any,
    storeId: string,
    orderId: string,
    targetStatus: OrderStatus,
  ): void {
    if (err?.status === 401) {
      this.toastService.error('Sesión no autorizada');
    } else if (err?.status === 403) {
      this.toastService.error('No tienes permisos para modificar pedidos de esta tienda.');
    } else if (err?.status === 404) {
      this.toastService.error('El pedido no existe o no pertenece a esta tienda.');
    } else if (err?.status === 422) {
      const message =
        err?.error?.message || 'Transición de estado no permitida según las reglas de negocio.';
      this.toastService.error(message);
    } else {
      // Error 500, error de red (status 0) o timeout: resultado incierto. Reconciliar vía GET
      this.toastService.error(
        'Error de comunicación con el servidor. Reconciliando estado del pedido...',
      );
      this.reconcileOrderStatus(storeId, orderId);
    }
  }

  private reconcileOrderStatus(storeId: string, orderId: string): void {
    this.getOrderDetail(storeId, orderId).subscribe({
      next: (detail) => {
        if (detail) {
          this.clearPendingVerification(orderId);
          this.applyUpdatedOrder(detail, storeId);
          this.toastService.info('Estado del pedido sincronizado con el servidor.');
        } else {
          this.setPendingVerification(orderId);
          this.toastService.warning(
            'No se pudo verificar el estado en el servidor. El pedido quedó marcado como pendiente de verificación.',
          );
        }
      },
      error: () => {
        this.setPendingVerification(orderId);
        this.toastService.warning(
          'No se pudo verificar el estado en el servidor. El pedido quedó marcado como pendiente de verificación.',
        );
      },
    });
  }

  verifyOrderStatus(
    orderId: string,
    storeId?: string,
  ): Observable<MerchantOrderDetailResponse | null> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      this.toastService.error('No se pudo determinar la tienda activa.');
      return of(null);
    }

    return this.getOrderDetail(targetStoreId, orderId).pipe(
      tap((detail) => {
        if (detail) {
          this.clearPendingVerification(orderId);
          this.applyUpdatedOrder(detail, targetStoreId);
          this.toastService.success('Estado del pedido verificado y sincronizado con el servidor.');
        }
      }),
      catchError(() => {
        this.toastService.error('Error al conectar con el servidor para verificar el estado.');
        return of(null);
      }),
    );
  }

  private getStatusLabel(status: OrderStatus): string {
    const labelMap: Record<OrderStatus, string> = {
      CONFIRMED: 'confirmado',
      IN_PREPARATION: 'en preparación',
      READY: 'listo para entrega',
      OUT_FOR_DELIVERY: 'en camino / despacho',
      COMPLETED: 'completado / entregado',
      CANCELLED: 'cancelado',
    };
    return labelMap[status] || status;
  }

  private handleHttpError(err: any): void {
    if (err?.status === 401) {
      this.error.set('Sesión no autorizada');
    } else if (err?.status === 403) {
      this.error.set('No tienes permisos para ver los pedidos de esta tienda');
      this.toastService.error('No tienes permisos para ver los pedidos de esta tienda.');
    } else {
      this.error.set('Error al cargar pedidos del servidor');
      this.toastService.error('No se pudieron cargar los pedidos.');
    }
  }

  setActiveTab(tab: OrderFilterTab): void {
    this.activeTab.set(tab);
    this.currentPage.set(0);
    this.loadOrders(0, this.pageSize(), tab).subscribe({
      error: () => {},
    });
  }

  setPage(pageIndex: number): void {
    this.currentPage.set(pageIndex);
    this.loadOrders(pageIndex, this.pageSize(), this.activeTab()).subscribe({
      error: () => {},
    });
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setChannelFilter(_channel: string): void {
    // El backend no soporta canales en Fase 1; mantener siempre en 'ALL' para evitar filtros residuales
    this.channelFilter.set('ALL');
  }

  clearFilters(): void {
    this.activeTab.set('ALL');
    this.searchQuery.set('');
    this.channelFilter.set('ALL');
  }

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  addOrder(_order: MerchantOrder): void {
    // La creación manual está deshabilitada hasta implementar el endpoint real en backend (Fase 1)
    this.toastService.info('La creación manual de pedidos estará disponible próximamente.');
  }

  openDrawer(order: MerchantOrder): void {
    this.selectedOrder.set(order);
    this.selectedOrderDetail.set(null);
    this.detailError.set(null);
    this.isDrawerOpen.set(true);

    const storeId = this.storeService.currentStore()?.id;
    if (storeId) {
      this.getOrderDetail(storeId, order.id).subscribe({
        error: () => {},
      });
    }
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedOrder.set(null);
    this.selectedOrderDetail.set(null);
    this.detailError.set(null);
    this.activeDetailOrderId = null;
  }

  notifyViaWhatsApp(order: MerchantOrder): void {
    if (!order.customerPhone) {
      this.toastService.info('No hay teléfono registrado para este cliente.');
      return;
    }
    const cleanPhone = order.customerPhone.replace(/\D/g, '');
    const clientFirstName = order.customerName.split(' ')[0];
    const storeName = this.storeService.storeName() || 'JaldiShop';
    const totalFormatted = `S/ ${order.totalAmount.toFixed(2)}`;
    const modality =
      order.deliveryMode === 'DELIVERY' ? 'Delivery a domicilio' : 'Recojo en tienda';

    let templateId: WhatsAppTemplateId = 'in_preparation';
    if (order.status === 'READY' || order.status === 'OUT_FOR_DELIVERY') {
      templateId = 'ready';
    } else if (order.status === 'COMPLETED') {
      templateId = 'completed';
    }

    const templates = this.settingsService.settings().whatsappTemplates;
    const template = templates.find((t) => t.id === templateId);

    let rawMessage = template
      ? template.message
      : `¡Hola {cliente}! Tu pedido {numero_pedido} está en estado {modalidad}. Total: {total}. ¡Gracias por tu compra en {tienda}!`;

    const formattedMessage = rawMessage
      .replace(/{cliente}/g, clientFirstName)
      .replace(/{numero_pedido}/g, order.orderNumber)
      .replace(/{tienda}/g, storeName)
      .replace(/{total}/g, totalFormatted)
      .replace(/{modalidad}/g, modality);

    const encoded = encodeURIComponent(formattedMessage);
    window.open(`https://wa.me/51${cleanPhone}?text=${encoded}`, '_blank');
    this.toastService.success(`WhatsApp abierto para ${order.customerName}.`);
  }

  clearOrders(): void {
    this.activeListRequestId++;
    this.activeDetailOrderId = null;
    this.updatingOrderIds.set(new Set());
    this.pendingVerificationOrderIds.set(new Set());
    this.orders.set([]);
    this.totalElements.set(0);
    this.totalPages.set(0);
    this.currentPage.set(0);
    this.isLoaded.set(false);
    this.isLoading.set(false);
    this.error.set(null);
    this.selectedOrder.set(null);
    this.selectedOrderDetail.set(null);
    this.isDetailLoading.set(false);
    this.detailError.set(null);
    this.isDrawerOpen.set(false);
    this.isCreateModalOpen.set(false);
    this.resetCounts();
    this.clearFilters();
  }

  resetCounts(): void {
    this.activeCountsRequestId++;
    this.activeTodayCountsRequestId++;
    this.inFlightCounts$ = null;
    this.inFlightCountsStoreId = null;
    this.inFlightTodayCounts$ = null;
    this.inFlightTodayCountsKey = null;
    this.orderCounts.set(null);
    this.isCountsLoading.set(false);
    this.countsError.set(null);
    this.todayOrderCounts.set(null);
    this.todayOperatingDate.set(null);
    this.isTodayCountsLoading.set(false);
    this.todayCountsError.set(null);
  }

  /**
   * Mecanismo para detectar el cruce de medianoche en Lima durante una sesión activa.
   */
  checkOperatingDateChange(): boolean {
    const freshDate = getOperatingDateLima();
    if (freshDate !== this.currentOperatingDate()) {
      this.handleOperatingDateTransition(freshDate);
      return true;
    }
    return false;
  }

  private handleOperatingDateTransition(newDate: string): void {
    this.currentOperatingDate.set(newDate);
    // Invalidar datos del día anterior para no presentarlos como vigentes
    this.todayOrderCounts.set(null);
    this.todayOperatingDate.set(null);
    this.todayCountsError.set(null);
    this.inFlightTodayCounts$ = null;
    this.inFlightTodayCountsKey = null;

    // Refrescar contadores diarios para la nueva fecha si hay tienda activa
    const currentStoreId = this.storeService.currentStore()?.id;
    if (currentStoreId) {
      this.loadTodayOrderCounts(currentStoreId, true).subscribe();
    }
  }

  private scheduleMidnightCheck(): void {
    if (this.midnightTimerId) {
      clearTimeout(this.midnightTimerId);
      this.midnightTimerId = null;
    }
    const ms = getMsUntilNextMidnightLima();
    // Añadir holgura de 200ms para asegurar que el reloj sobrepasó el segundo 00:00:00
    this.midnightTimerId = setTimeout(
      () => {
        this.checkOperatingDateChange();
        this.scheduleMidnightCheck();
      },
      Math.max(200, ms + 200),
    );
  }

  ngOnDestroy(): void {
    if (this.midnightTimerId) {
      clearTimeout(this.midnightTimerId);
      this.midnightTimerId = null;
    }
    this.inFlightCounts$ = null;
    this.inFlightTodayCounts$ = null;
  }

  getOrderStatusCounts(storeId?: string, date?: string): Observable<MerchantOrderStatusCounts> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      return throwError(() => new Error('No se pudo determinar la tienda activa.'));
    }

    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }

    const url = `${this.baseUrl}/${targetStoreId}/orders/counts`;
    return this.http.get<MerchantOrderStatusCounts>(url, { params });
  }

  loadOrderCounts(storeId?: string, force = false): Observable<MerchantOrderStatusCounts | null> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      return this.storeService.getMyStore().pipe(
        switchMap((store) => {
          if (!store?.id) {
            this.isCountsLoading.set(false);
            return of(null);
          }
          return this.fetchCountsDeduplicated(store.id, force);
        }),
        catchError((err) => {
          this.isCountsLoading.set(false);
          this.handleCountsError(err);
          return of(null);
        }),
      );
    }

    return this.fetchCountsDeduplicated(targetStoreId, force);
  }

  private fetchCountsDeduplicated(
    storeId: string,
    force: boolean,
  ): Observable<MerchantOrderStatusCounts | null> {
    if (!force && this.inFlightCounts$ && this.inFlightCountsStoreId === storeId) {
      return this.inFlightCounts$;
    }

    this.inFlightCountsStoreId = storeId;
    this.inFlightCounts$ = this.fetchCountsHttp(storeId).pipe(
      shareReplay({ bufferSize: 1, refCount: true }),
      finalize(() => {
        if (this.inFlightCountsStoreId === storeId) {
          this.inFlightCounts$ = null;
          this.inFlightCountsStoreId = null;
        }
      }),
    );
    return this.inFlightCounts$;
  }

  private fetchCountsHttp(storeId: string): Observable<MerchantOrderStatusCounts | null> {
    const requestId = ++this.activeCountsRequestId;
    this.isCountsLoading.set(true);

    return this.getOrderStatusCounts(storeId).pipe(
      tap((counts) => {
        const currentActiveStoreId = this.storeService.currentStore()?.id;
        if (
          requestId !== this.activeCountsRequestId ||
          (currentActiveStoreId && currentActiveStoreId !== storeId)
        ) {
          return;
        }
        this.orderCounts.set(counts);
        this.countsError.set(null);
        this.isCountsLoading.set(false);
      }),
      catchError((err) => {
        if (requestId === this.activeCountsRequestId) {
          this.isCountsLoading.set(false);
          this.handleCountsError(err);
        }
        return of(null);
      }),
    );
  }

  loadTodayOrderCounts(
    storeId?: string,
    force = false,
  ): Observable<MerchantOrderStatusCounts | null> {
    const targetStoreId = storeId ?? this.storeService.currentStore()?.id;
    if (!targetStoreId) {
      return this.storeService.getMyStore().pipe(
        switchMap((store) => {
          if (!store?.id) {
            this.isTodayCountsLoading.set(false);
            return of(null);
          }
          return this.fetchTodayCountsDeduplicated(store.id, force);
        }),
        catchError((err) => {
          this.isTodayCountsLoading.set(false);
          this.handleTodayCountsError(err);
          return of(null);
        }),
      );
    }

    return this.fetchTodayCountsDeduplicated(targetStoreId, force);
  }

  private fetchTodayCountsDeduplicated(
    storeId: string,
    force: boolean,
  ): Observable<MerchantOrderStatusCounts | null> {
    const todayLima = this.currentOperatingDate();
    const key = `${storeId}:${todayLima}`;

    if (!force && this.inFlightTodayCounts$ && this.inFlightTodayCountsKey === key) {
      return this.inFlightTodayCounts$;
    }

    this.inFlightTodayCountsKey = key;
    this.inFlightTodayCounts$ = this.fetchTodayCountsHttp(storeId, todayLima).pipe(
      shareReplay({ bufferSize: 1, refCount: true }),
      finalize(() => {
        if (this.inFlightTodayCountsKey === key) {
          this.inFlightTodayCounts$ = null;
          this.inFlightTodayCountsKey = null;
        }
      }),
    );
    return this.inFlightTodayCounts$;
  }

  private fetchTodayCountsHttp(
    storeId: string,
    todayLima = this.currentOperatingDate(),
  ): Observable<MerchantOrderStatusCounts | null> {
    const requestId = ++this.activeTodayCountsRequestId;
    this.isTodayCountsLoading.set(true);

    return this.getOrderStatusCounts(storeId, todayLima).pipe(
      tap((counts) => {
        const currentActiveStoreId = this.storeService.currentStore()?.id;
        if (
          requestId !== this.activeTodayCountsRequestId ||
          (currentActiveStoreId && currentActiveStoreId !== storeId)
        ) {
          return;
        }
        this.todayOrderCounts.set(counts);
        this.todayOperatingDate.set(todayLima);
        this.todayCountsError.set(null);
        this.isTodayCountsLoading.set(false);
      }),
      catchError((err) => {
        if (requestId === this.activeTodayCountsRequestId) {
          this.isTodayCountsLoading.set(false);
          this.handleTodayCountsError(err);
        }
        return of(null);
      }),
    );
  }

  private handleCountsError(err: any): void {
    const msg =
      err?.status === 403
        ? 'No tienes permisos para consultar contadores de esta tienda'
        : err?.status === 404
          ? 'Tienda no encontrada para consultar contadores'
          : 'Error al consultar contadores de pedidos';
    this.countsError.set(msg);
  }

  private handleTodayCountsError(err: any): void {
    const msg =
      err?.status === 403
        ? 'No tienes permisos para consultar contadores de hoy'
        : err?.status === 404
          ? 'Tienda no encontrada para consultar contadores de hoy'
          : 'Error al consultar contadores del día';
    this.todayCountsError.set(msg);
  }
}

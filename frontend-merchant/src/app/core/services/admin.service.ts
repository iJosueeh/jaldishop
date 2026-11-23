import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal, OnDestroy } from '@angular/core';
import { Observable, of, tap, catchError, throwError, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminAnalyticsSummary,
  AdminAuditLog,
  AdminAuditLogPage,
  AdminDashboardMetrics,
  AdminInvitation,
  AdminPlatformSettings,
  AdminStoreDetail,
  AdminStoreSummary,
  AdminUserRole,
  AdminUserStatus,
  AdminUserSummary,
  InviteAdminRequest,
  OrderInvestigationResult,
  OrderReconciliationResult,
} from '../models/admin.models';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';

@Injectable({
  providedIn: 'root',
})
export class AdminService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly toastService = inject(ToastService);
  private readonly invalidationBus = inject(InvalidationBusService);
  private readonly busSubscriptions = new Subscription();

  // Estados Reactivos Principales (Single Source of Truth)
  readonly users = signal<AdminUserSummary[]>([]);
  readonly stores = signal<AdminStoreSummary[]>([]);
  readonly isLoadingUsers = signal<boolean>(false);
  readonly isLoadingStores = signal<boolean>(false);
  private readonly isUsersLoaded = signal<boolean>(false);
  private readonly isStoresLoaded = signal<boolean>(false);
  private lastUsersSync = 0;
  private lastStoresSync = 0;
  private readonly STALE_THRESHOLD_MS = 60_000; // 60 segundos para SWR

  // Analíticas Globales de Plataforma
  readonly analyticsSummary = signal<AdminAnalyticsSummary | null>(null);
  readonly isLoadingAnalytics = signal<boolean>(false);
  readonly isExportingPdf = signal<boolean>(false);

  // Auditoría
  readonly auditLogs = signal<AdminAuditLog[]>([]);
  readonly totalAuditLogs = signal<number>(0);
  readonly isLoadingAuditLogs = signal<boolean>(false);

  // Invitaciones de Administradores
  readonly invitations = signal<AdminInvitation[]>([]);
  readonly isLoadingInvitations = signal<boolean>(false);

  // Parámetros Globales de la Plataforma
  readonly settings = signal<AdminPlatformSettings>({
    allowMerchantRegistration: true,
    requireStoreApproval: false,
    maintenanceMode: false,
    maintenanceNotice: 'La plataforma se encuentra en mantenimiento programado. Volveremos pronto.',
    sessionTimeoutHours: 24,
    enforce2FAForAdmins: true,
    notifyOnNewStoreRegistration: true,
    notifyOnStoreSuspension: true,
    adminAlertEmail: 'admin@jaldishop.com',
    platformVersion: '1.2.0',
    environment: 'production',
    marketplaceCommissionRate: 5.0,
  });

  // Filtros de Usuarios
  readonly userSearchQuery = signal<string>('');
  readonly userRoleFilter = signal<string>('ALL');
  readonly userStatusFilter = signal<string>('ALL');

  // Filtros de Tiendas
  readonly storeSearchQuery = signal<string>('');
  readonly storeStatusFilter = signal<string>('ALL');
  readonly storeModalityFilter = signal<string>('ALL');

  // Filtros de Comerciantes
  readonly merchantSearchQuery = signal<string>('');
  readonly merchantStatusFilter = signal<string>('ALL');

  // Cache en memoria para detalles completos de tiendas
  private readonly storeDetailCache = new Map<string, AdminStoreDetail>();

  constructor() {
    // Sincronización reactiva en tiempo real mediante InvalidationBus
    this.busSubscriptions.add(
      this.invalidationBus.cacheInvalidated$.subscribe((event) => {
        if (event.module === 'ALL' || event.module === 'STORE') {
          this.loadStores(true).subscribe();
        }
        if (event.module === 'ALL' || event.module === 'ANALYTICS') {
          this.loadAnalyticsSummary().subscribe();
        }
      })
    );

    this.busSubscriptions.add(
      this.invalidationBus.storeProfileMutated$.subscribe(() => {
        this.loadStores(true).subscribe();
      })
    );
  }

  ngOnDestroy(): void {
    this.busSubscriptions.unsubscribe();
  }

  // Métricas Computadas del Dashboard Global
  readonly metrics = computed<AdminDashboardMetrics>(() => {
    const allUsers = this.users();
    const allStores = this.stores();

    return {
      totalUsers: allUsers.length,
      totalMerchants: allUsers.filter((u) => u.roles.includes('MERCHANT')).length,
      totalCustomers: allUsers.filter((u) => u.roles.includes('CUSTOMER')).length,
      totalAdmins: allUsers.filter((u) => u.roles.includes('ADMIN')).length,
      activeUsers: allUsers.filter((u) => u.status === 'ACTIVE').length,
      suspendedUsers: allUsers.filter((u) => u.status === 'SUSPENDED').length,
      totalStores: allStores.length,
      activeStores: allStores.filter((s) => s.status === 'ACTIVE').length,
      suspendedStores: allStores.filter((s) => s.status === 'SUSPENDED').length,
    };
  });

  // Usuarios Filtrados Reactivos
  readonly filteredUsers = computed(() => {
    const query = this.userSearchQuery().toLowerCase().trim();
    const role = this.userRoleFilter();
    const status = this.userStatusFilter();

    return this.users().filter((user) => {
      if (role !== 'ALL' && !user.roles.includes(role as AdminUserRole)) {
        return false;
      }
      if (status !== 'ALL' && user.status !== status) {
        return false;
      }
      if (query) {
        const matchName = user.fullName.toLowerCase().includes(query);
        const matchEmail = user.email.toLowerCase().includes(query);
        const matchPhone = user.phone?.includes(query);
        const matchStore = user.storeName?.toLowerCase().includes(query);
        if (!matchName && !matchEmail && !matchPhone && !matchStore) {
          return false;
        }
      }
      return true;
    });
  });

  // Comerciantes Filtrados Reactivos
  readonly merchants = computed(() => {
    return this.users().filter((u) => u.roles.includes('MERCHANT'));
  });

  readonly filteredMerchants = computed(() => {
    const query = this.merchantSearchQuery().toLowerCase().trim();
    const status = this.merchantStatusFilter();

    return this.merchants().filter((merchant) => {
      if (status !== 'ALL' && merchant.status !== status) {
        return false;
      }
      if (query) {
        const matchName = merchant.fullName.toLowerCase().includes(query);
        const matchEmail = merchant.email.toLowerCase().includes(query);
        const matchPhone = merchant.phone?.includes(query);
        const matchStore = merchant.storeName?.toLowerCase().includes(query);
        if (!matchName && !matchEmail && !matchPhone && !matchStore) {
          return false;
        }
      }
      return true;
    });
  });

  // Tiendas Filtradas Reactivas
  readonly filteredStores = computed(() => {
    const query = this.storeSearchQuery().toLowerCase().trim();
    const status = this.storeStatusFilter();
    const modality = this.storeModalityFilter();

    return this.stores().filter((store) => {
      if (status !== 'ALL' && store.status !== status) {
        return false;
      }
      if (modality === 'DELIVERY' && !store.deliveryEnabled) {
        return false;
      }
      if (modality === 'PICKUP' && !store.pickupEnabled) {
        return false;
      }
      if (query) {
        const matchName = store.name.toLowerCase().includes(query);
        const matchSlug = store.slug.toLowerCase().includes(query);
        const matchOwner = store.merchant?.fullName.toLowerCase().includes(query);
        const matchEmail = store.merchant?.email.toLowerCase().includes(query);
        if (!matchName && !matchSlug && !matchOwner && !matchEmail) {
          return false;
        }
      }
      return true;
    });
  });

  // 1. CARGA DE USUARIOS (Cache-First + SWR)
  loadUsers(forceRefresh = false): Observable<AdminUserSummary[]> {
    const now = Date.now();
    const isStale = now - this.lastUsersSync > this.STALE_THRESHOLD_MS;

    if (this.isUsersLoaded() && !forceRefresh) {
      if (isStale) {
        this.fetchUsersSilently();
      }
      return of(this.users());
    }

    this.isLoadingUsers.set(true);
    return this.http.get<AdminUserSummary[]>(`${environment.apiUrl}/admin/users`).pipe(
      tap({
        next: (data) => {
          this.users.set(data);
          this.isUsersLoaded.set(true);
          this.lastUsersSync = Date.now();
          this.isLoadingUsers.set(false);
        },
        error: () => {
          this.isLoadingUsers.set(false);
        },
      }),
    );
  }

  private fetchUsersSilently(): void {
    this.http.get<AdminUserSummary[]>(`${environment.apiUrl}/admin/users`).subscribe({
      next: (data) => {
        this.users.set(data);
        this.lastUsersSync = Date.now();
      },
    });
  }

  // 2. CARGA DE TIENDAS (Cache-First + SWR)
  loadStores(forceRefresh = false): Observable<AdminStoreSummary[]> {
    const now = Date.now();
    const isStale = now - this.lastStoresSync > this.STALE_THRESHOLD_MS;

    if (this.isStoresLoaded() && !forceRefresh) {
      if (isStale) {
        this.fetchStoresSilently();
      }
      return of(this.stores());
    }

    this.isLoadingStores.set(true);
    return this.http.get<AdminStoreSummary[]>(`${environment.apiUrl}/admin/stores`).pipe(
      tap({
        next: (data) => {
          this.stores.set(data);
          this.isStoresLoaded.set(true);
          this.lastStoresSync = Date.now();
          this.isLoadingStores.set(false);
        },
        error: () => {
          this.isLoadingStores.set(false);
        },
      }),
    );
  }

  private fetchStoresSilently(): void {
    this.http.get<AdminStoreSummary[]>(`${environment.apiUrl}/admin/stores`).subscribe({
      next: (data) => {
        this.stores.set(data);
        this.lastStoresSync = Date.now();
      },
    });
  }

  // 3. MUTACIONES CON OPTIMISTIC UI Y SNAPSHOT ROLLBACK
  suspendUser(userId: string): Observable<AdminUserSummary> {
    const previousSnapshot = this.users();
    // Optimistic Update
    this.users.update((list) =>
      list.map((u) => (u.id === userId ? { ...u, status: 'SUSPENDED' as AdminUserStatus } : u))
    );

    return this.http.patch<AdminUserSummary>(`${environment.apiUrl}/admin/users/${userId}/suspend`, {}).pipe(
      tap({
        next: (updatedUser) => {
          this.users.update((list) => list.map((u) => (u.id === userId ? updatedUser : u)));
          this.toastService.warning(`Usuario ${updatedUser.fullName} suspendido exitosamente.`);
        },
        error: (err) => {
          // Rollback snapshot
          this.users.set(previousSnapshot);
          if (err.status === 409) {
            this.toastService.error('No puedes suspender tu propia cuenta de administrador.');
          } else {
            this.toastService.error('Error al suspender el usuario.');
          }
        },
      }),
    );
  }

  activateUser(userId: string): Observable<AdminUserSummary> {
    const previousSnapshot = this.users();
    // Optimistic Update
    this.users.update((list) =>
      list.map((u) => (u.id === userId ? { ...u, status: 'ACTIVE' as AdminUserStatus } : u))
    );

    return this.http.patch<AdminUserSummary>(`${environment.apiUrl}/admin/users/${userId}/activate`, {}).pipe(
      tap({
        next: (updatedUser) => {
          this.users.update((list) => list.map((u) => (u.id === userId ? updatedUser : u)));
          this.toastService.success(`Usuario ${updatedUser.fullName} reactivado.`);
        },
        error: () => {
          this.users.set(previousSnapshot);
          this.toastService.error('Error al reactivar el usuario.');
        },
      }),
    );
  }

  updateUserRoles(userId: string, roles: AdminUserRole[]): Observable<AdminUserSummary> {
    const previousSnapshot = this.users();
    this.users.update((list) =>
      list.map((u) => (u.id === userId ? { ...u, roles } : u))
    );

    return this.http
      .patch<AdminUserSummary>(`${environment.apiUrl}/admin/users/${userId}/roles`, { roles })
      .pipe(
        tap({
          next: (updatedUser) => {
            this.users.update((list) => list.map((u) => (u.id === userId ? updatedUser : u)));
            this.toastService.success(
              `Roles de ${updatedUser.fullName} actualizados correctamente.`,
              'Seguridad y Accesos'
            );
          },
          error: (err) => {
            this.users.set(previousSnapshot);
            if (err.status === 400 || err.status === 409) {
              this.toastService.error(
                err.error?.message || 'No puedes removerte tu propio rol de administrador.'
              );
            } else {
              this.toastService.error('Error al actualizar los roles del usuario.');
            }
          },
        }),
      );
  }

  suspendStore(storeId: string): Observable<AdminStoreSummary> {
    const previousSnapshot = this.stores();
    this.stores.update((list) =>
      list.map((s) => (s.id === storeId ? { ...s, status: 'SUSPENDED' } : s))
    );

    return this.http.patch<AdminStoreSummary>(`${environment.apiUrl}/admin/stores/${storeId}/suspend`, {}).pipe(
      tap({
        next: (updatedStore) => {
          this.stores.update((list) => list.map((s) => (s.id === storeId ? updatedStore : s)));
          this.toastService.warning(`Tienda ${updatedStore.name} suspendida.`);
        },
        error: () => {
          this.stores.set(previousSnapshot);
          this.toastService.error('Error al suspender la tienda.');
        },
      }),
    );
  }

  activateStore(storeId: string): Observable<AdminStoreSummary> {
    const previousSnapshot = this.stores();
    this.stores.update((list) =>
      list.map((s) => (s.id === storeId ? { ...s, status: 'ACTIVE' } : s))
    );

    return this.http.patch<AdminStoreSummary>(`${environment.apiUrl}/admin/stores/${storeId}/activate`, {}).pipe(
      tap({
        next: (updatedStore) => {
          this.stores.update((list) => list.map((s) => (s.id === storeId ? updatedStore : s)));
          this.toastService.success(`Tienda ${updatedStore.name} reactivada.`);
        },
        error: () => {
          this.stores.set(previousSnapshot);
          this.toastService.error('Error al reactivar la tienda.');
        },
      }),
    );
  }

  // 4. DETALLE DE TIENDA Y PERFIL FISCAL (Lazy-loaded con Cache)
  getStoreDetail(storeId: string, forceRefresh = false): Observable<AdminStoreDetail> {
    if (!forceRefresh && this.storeDetailCache.has(storeId)) {
      return of(this.storeDetailCache.get(storeId)!);
    }

    return this.http.get<AdminStoreDetail>(`${environment.apiUrl}/admin/stores/${storeId}`).pipe(
      tap((detail) => {
        this.storeDetailCache.set(storeId, detail);
      }),
    );
  }

  getStoreBySlug(slug: string): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}/stores/slug/${slug}`);
  }

  // 5. ANALÍTICAS CONSOLIDADAS Y EXPORTACIÓN PDF
  loadAnalyticsSummary(from?: string, to?: string, storeId?: string): Observable<AdminAnalyticsSummary> {
    this.isLoadingAnalytics.set(true);
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    if (storeId) params = params.set('storeId', storeId);

    return this.http
      .get<AdminAnalyticsSummary>(`${environment.apiUrl}/admin/analytics/summary`, { params })
      .pipe(
        tap({
          next: (data) => {
            if (!storeId) {
              this.analyticsSummary.set(data);
            }
            this.isLoadingAnalytics.set(false);
          },
          error: () => {
            this.isLoadingAnalytics.set(false);
          },
        }),
      );
  }

  downloadAnalyticsPdf(from?: string, to?: string, storeId?: string): Observable<Blob> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    if (storeId) params = params.set('storeId', storeId);

    return this.http.get(`${environment.apiUrl}/admin/analytics/export/pdf`, {
      params,
      responseType: 'blob',
    });
  }

  exportAnalyticsPdf(from?: string, to?: string, storeId?: string): void {
    this.isExportingPdf.set(true);
    this.downloadAnalyticsPdf(from, to, storeId).subscribe({
      next: (blob) => {
        this.isExportingPdf.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        const filename = `jaldishop-admin-analytics-${new Date().toISOString().slice(0, 10)}.pdf`;
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.toastService.success('Reporte ejecutivo descargado correctamente.', 'Exportación Completa');
      },
      error: () => {
        this.isExportingPdf.set(false);
        this.toastService.error('Error al generar el reporte en PDF de analíticas.', 'Error de Exportación');
      },
    });
  }

  // 6. PISTAS DE AUDITORÍA (AdminAuditLogController)
  loadAuditLogs(options?: {
    action?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Observable<AdminAuditLogPage> {
    this.isLoadingAuditLogs.set(true);
    let params = new HttpParams();
    if (options?.action) params = params.set('action', options.action);
    if (options?.status) params = params.set('status', options.status);
    params = params.set('page', (options?.page ?? 0).toString());
    params = params.set('size', (options?.size ?? 20).toString());

    return this.http
      .get<AdminAuditLogPage>(`${environment.apiUrl}/admin/audit-logs`, { params })
      .pipe(
        tap({
          next: (pageData) => {
            this.auditLogs.set(pageData.content || []);
            this.totalAuditLogs.set(pageData.totalElements || 0);
            this.isLoadingAuditLogs.set(false);
          },
          error: (err) => {
            this.isLoadingAuditLogs.set(false);
            if (err.status === 403) {
              this.toastService.warning(
                'Solo el administrador principal tiene permisos para consultar los registros de auditoría.',
                'Acceso Restringido'
              );
            } else {
              this.toastService.error('Error al cargar pistas de auditoría.');
            }
          },
        }),
      );
  }

  // 7. GOBIERNO DE ADMINISTRADORES E INVITACIONES (AdminAdministratorController)
  loadInvitations(status?: string, page = 0, size = 20): Observable<any> {
    this.isLoadingInvitations.set(true);
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) params = params.set('status', status);

    return this.http
      .get<{ content: AdminInvitation[] }>(`${environment.apiUrl}/admin/administrators/invitations`, { params })
      .pipe(
        tap({
          next: (res) => {
            this.invitations.set(res.content || []);
            this.isLoadingInvitations.set(false);
          },
          error: (err) => {
            this.isLoadingInvitations.set(false);
            if (err.status === 403) {
              this.toastService.warning(
                'Solo el administrador principal puede gestionar invitaciones de administradores.',
                'Acceso Restringido'
              );
            }
          },
        }),
      );
  }

  inviteAdmin(request: InviteAdminRequest): Observable<AdminInvitation> {
    return this.http
      .post<AdminInvitation>(`${environment.apiUrl}/admin/administrators/invitations`, request)
      .pipe(
        tap({
          next: (invitation) => {
            this.invitations.update((list) => [invitation, ...list]);
            this.toastService.success(
              `Invitación enviada a ${invitation.email}. Vigente por 48 horas.`,
              'Invitación Creada'
            );
          },
          error: (err) => {
            if (err.status === 409) {
              this.toastService.error(
                err.error?.message ||
                  'Ya existe un usuario con este correo. Utilice la opción Promover en su ficha.',
                'Usuario Ya Registrado'
              );
            } else if (err.status === 403) {
              this.toastService.error('Solo el administrador principal puede invitar administradores.');
            } else {
              this.toastService.error('Error al generar la invitación de administrador.');
            }
          },
        }),
      );
  }

  revokeAdminInvitation(invitationId: string): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/admin/administrators/invitations/${invitationId}/revoke`, {})
      .pipe(
        tap({
          next: () => {
            this.invitations.update((list) =>
              list.map((inv) => (inv.id === invitationId ? { ...inv, status: 'REVOKED' } : inv))
            );
            this.toastService.warning('Invitación administrativa revocada.');
          },
          error: () => {
            this.toastService.error('Error al revocar la invitación.');
          },
        }),
      );
  }

  promoteUserToAdmin(userId: string): Observable<AdminUserSummary> {
    return this.http
      .post<AdminUserSummary>(`${environment.apiUrl}/admin/administrators/${userId}/promote`, {})
      .pipe(
        tap({
          next: (promoted) => {
            this.users.update((list) => list.map((u) => (u.id === userId ? promoted : u)));
            this.toastService.success(
              `Usuario ${promoted.fullName} promovido a Administrador con éxito.`,
              'Gobierno de Accesos'
            );
          },
          error: (err) => {
            if (err.status === 409) {
              this.toastService.error(
                err.error?.message || 'No se permite la promoción de comerciantes con tiendas activas.',
                'Restricción de Privilegios'
              );
            } else if (err.status === 403) {
              this.toastService.error('Solo el administrador principal puede promover administradores.');
            } else {
              this.toastService.error('Error al promover usuario a administrador.');
            }
          },
        }),
      );
  }

  revokeAdminRole(userId: string): Observable<AdminUserSummary> {
    return this.http
      .post<AdminUserSummary>(`${environment.apiUrl}/admin/administrators/${userId}/revoke`, {})
      .pipe(
        tap({
          next: (revoked) => {
            this.users.update((list) => list.map((u) => (u.id === userId ? revoked : u)));
            this.toastService.warning(`Privilegios administrativos revocados a ${revoked.fullName}.`);
          },
          error: (err) => {
            if (err.status === 409) {
              this.toastService.error(err.error?.message || 'No puedes revocar tus propios privilegios.');
            } else if (err.status === 403) {
              this.toastService.error('Solo el administrador principal puede revocar privilegios administrativos.');
            } else {
              this.toastService.error('Error al revocar rol de administrador.');
            }
          },
        }),
      );
  }

  // 8. CONCILIACIÓN E INVESTIGACIÓN DE PAGOS (AdminOrderInvestigationController)
  investigatePaymentAttempt(attemptId: string): Observable<OrderInvestigationResult> {
    return this.http
      .post<OrderInvestigationResult>(
        `${environment.apiUrl}/admin/payments/attempts/${attemptId}/investigate-order`,
        {}
      )
      .pipe(
        tap({
          error: () => {
            this.toastService.error('Error al investigar el intento de pago.');
          },
        }),
      );
  }

  reconcilePaymentAttempt(attemptId: string): Observable<OrderReconciliationResult> {
    return this.http
      .post<OrderReconciliationResult>(
        `${environment.apiUrl}/admin/payments/attempts/${attemptId}/reconcile-order`,
        {}
      )
      .pipe(
        tap({
          next: (result) => {
            this.toastService.success(
              `Conciliación completada: ${result.detail || result.status}`,
              'Orden Conciliada'
            );
            // Notificar al bus reactivo para refrescar datos de órdenes/tiendas
            this.invalidationBus.invalidateAll('ALL');
          },
          error: (err) => {
            this.toastService.error(
              err.error?.message || 'Error al ejecutar conciliación de orden.',
              'Fallo de Conciliación'
            );
          },
        }),
      );
  }

  getReconciliationCandidates(limit: number = 15): Observable<import('../models/admin.models').ReconciliationCandidateItem[]> {
    return this.http.get<import('../models/admin.models').ReconciliationCandidateItem[]>(
      `${environment.apiUrl}/admin/payments/attempts/candidates`,
      { params: { limit: limit.toString() } }
    );
  }

  // 9. FILTROS Y PARÁMETROS
  setUserSearchQuery(query: string): void {
    this.userSearchQuery.set(query);
  }

  setUserRoleFilter(role: string): void {
    this.userRoleFilter.set(role);
  }

  setUserStatusFilter(status: string): void {
    this.userStatusFilter.set(status);
  }

  setStoreSearchQuery(query: string): void {
    this.storeSearchQuery.set(query);
  }

  setStoreStatusFilter(status: string): void {
    this.storeStatusFilter.set(status);
  }

  setStoreModalityFilter(modality: string): void {
    this.storeModalityFilter.set(modality);
  }

  setMerchantSearchQuery(query: string): void {
    this.merchantSearchQuery.set(query);
  }

  setMerchantStatusFilter(status: string): void {
    this.merchantStatusFilter.set(status);
  }

  updateSettings(partial: Partial<AdminPlatformSettings>): void {
    this.settings.update((current) => ({ ...current, ...partial }));
  }

  saveSettings(): void {
    this.toastService.success('Parámetros de la plataforma guardados exitosamente.', 'Configuración Actualizada');
  }

  clearCache(): void {
    this.users.set([]);
    this.stores.set([]);
    this.analyticsSummary.set(null);
    this.auditLogs.set([]);
    this.invitations.set([]);
    this.storeDetailCache.clear();
    this.isUsersLoaded.set(false);
    this.isStoresLoaded.set(false);
    this.userSearchQuery.set('');
    this.userRoleFilter.set('ALL');
    this.userStatusFilter.set('ALL');
    this.storeSearchQuery.set('');
    this.storeStatusFilter.set('ALL');
    this.storeModalityFilter.set('ALL');
    this.merchantSearchQuery.set('');
    this.merchantStatusFilter.set('ALL');
  }
}

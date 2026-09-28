import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminDashboardMetrics,
  AdminPlatformSettings,
  AdminStoreSummary,
  AdminUserRole,
  AdminUserStatus,
  AdminUserSummary,
} from '../models/admin.models';
import { ToastService } from './toast.service';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly toastService = inject(ToastService);

  readonly users = signal<AdminUserSummary[]>([]);
  readonly stores = signal<AdminStoreSummary[]>([]);
  readonly isLoadingUsers = signal<boolean>(false);
  readonly isLoadingStores = signal<boolean>(false);
  private readonly isUsersLoaded = signal<boolean>(false);
  private readonly isStoresLoaded = signal<boolean>(false);

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
      // 1. Filtro por Rol
      if (role !== 'ALL' && !user.roles.includes(role as AdminUserRole)) {
        return false;
      }
      // 2. Filtro por Estado
      if (status !== 'ALL' && user.status !== status) {
        return false;
      }
      // 3. Filtro por Búsqueda de Texto
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

  // Comerciantes Especializados y Filtrados
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
      // 1. Filtro por Estado
      if (status !== 'ALL' && store.status !== status) {
        return false;
      }
      // 2. Filtro por Modalidad
      if (modality === 'DELIVERY' && !store.deliveryEnabled) {
        return false;
      }
      if (modality === 'PICKUP' && !store.pickupEnabled) {
        return false;
      }
      // 3. Filtro por Búsqueda
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

  loadUsers(forceRefresh = false): Observable<AdminUserSummary[]> {
    if (this.isUsersLoaded() && !forceRefresh) {
      return of(this.users());
    }

    this.isLoadingUsers.set(true);
    return this.http.get<AdminUserSummary[]>(`${environment.apiUrl}/admin/users`).pipe(
      tap({
        next: (data) => {
          this.users.set(data);
          this.isUsersLoaded.set(true);
          this.isLoadingUsers.set(false);
        },
        error: () => {
          this.isLoadingUsers.set(false);
        },
      }),
    );
  }

  loadStores(forceRefresh = false): Observable<AdminStoreSummary[]> {
    if (this.isStoresLoaded() && !forceRefresh) {
      return of(this.stores());
    }

    this.isLoadingStores.set(true);
    return this.http.get<AdminStoreSummary[]>(`${environment.apiUrl}/admin/stores`).pipe(
      tap({
        next: (data) => {
          this.stores.set(data);
          this.isStoresLoaded.set(true);
          this.isLoadingStores.set(false);
        },
        error: () => {
          this.isLoadingStores.set(false);
        },
      }),
    );
  }

  suspendUser(userId: string): Observable<AdminUserSummary> {
    return this.http.patch<AdminUserSummary>(`${environment.apiUrl}/admin/users/${userId}/suspend`, {}).pipe(
      tap({
        next: (updatedUser) => {
          this.users.update((list) => list.map((u) => (u.id === userId ? updatedUser : u)));
          this.toastService.warning(`Usuario ${updatedUser.fullName} suspendido exitosamente.`);
        },
        error: (err) => {
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
    return this.http.patch<AdminUserSummary>(`${environment.apiUrl}/admin/users/${userId}/activate`, {}).pipe(
      tap({
        next: (updatedUser) => {
          this.users.update((list) => list.map((u) => (u.id === userId ? updatedUser : u)));
          this.toastService.success(`Usuario ${updatedUser.fullName} reactivado.`);
        },
        error: () => {
          this.toastService.error('Error al reactivar el usuario.');
        },
      }),
    );
  }

  suspendStore(storeId: string): Observable<AdminStoreSummary> {
    return this.http.patch<AdminStoreSummary>(`${environment.apiUrl}/admin/stores/${storeId}/suspend`, {}).pipe(
      tap({
        next: (updatedStore) => {
          this.stores.update((list) => list.map((s) => (s.id === storeId ? updatedStore : s)));
          this.toastService.warning(`Tienda ${updatedStore.name} suspendida.`);
        },
        error: () => {
          this.toastService.error('Error al suspender la tienda.');
        },
      }),
    );
  }

  activateStore(storeId: string): Observable<AdminStoreSummary> {
    return this.http.patch<AdminStoreSummary>(`${environment.apiUrl}/admin/stores/${storeId}/activate`, {}).pipe(
      tap({
        next: (updatedStore) => {
          this.stores.update((list) => list.map((s) => (s.id === storeId ? updatedStore : s)));
          this.toastService.success(`Tienda ${updatedStore.name} reactivada.`);
        },
        error: () => {
          this.toastService.error('Error al reactivar la tienda.');
        },
      }),
    );
  }

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

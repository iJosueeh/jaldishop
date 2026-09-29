import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStoreSummary, AdminUserSummary } from '../../../core/models/admin.models';
import { AdminDashboardKpis } from './components/admin-dashboard-kpis/admin-dashboard-kpis';
import { AdminPlatformHealth } from './components/admin-platform-health/admin-platform-health';
import { AdminQuickUsers } from './components/admin-quick-users/admin-quick-users';
import { AdminQuickStores } from './components/admin-quick-stores/admin-quick-stores';
import { AdminUserDrawer } from '../users/components/admin-user-drawer/admin-user-drawer';
import { AdminStoreDrawer } from '../stores/components/admin-store-drawer/admin-store-drawer';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [
    AdminDashboardKpis,
    AdminPlatformHealth,
    AdminQuickUsers,
    AdminQuickStores,
    AdminUserDrawer,
    AdminStoreDrawer,
    NgIcon,
  ],
  providers: [provideIcons({ matRefreshOutline })],
  selector: 'app-admin-dashboard',
  styleUrl: './admin-dashboard.css',
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit {
  private readonly router = inject(Router);
  readonly adminService = inject(AdminService);

  readonly drawerUser = signal<AdminUserSummary | null>(null);
  readonly isUserDrawerOpen = signal<boolean>(false);

  readonly drawerStore = signal<AdminStoreSummary | null>(null);
  readonly isStoreDrawerOpen = signal<boolean>(false);

  readonly recentUsers = computed(() => {
    return this.adminService.users().slice(0, 5);
  });

  readonly recentStores = computed(() => {
    return this.adminService.stores().slice(0, 5);
  });

  ngOnInit(): void {
    this.adminService.loadUsers().subscribe();
    this.adminService.loadStores().subscribe();
  }

  refreshData(): void {
    this.adminService.loadUsers(true).subscribe();
    this.adminService.loadStores(true).subscribe();
  }

  onSelectUser(user: AdminUserSummary): void {
    this.drawerUser.set(user);
    this.isUserDrawerOpen.set(true);
  }

  onCloseUserDrawer(): void {
    this.isUserDrawerOpen.set(false);
    this.drawerUser.set(null);
  }

  onUserDrawerStatusChange(user: AdminUserSummary): void {
    if (user.status === 'ACTIVE') {
      this.adminService.suspendUser(user.id).subscribe({
        next: () => this.onCloseUserDrawer(),
      });
    } else {
      this.adminService.activateUser(user.id).subscribe({
        next: () => this.onCloseUserDrawer(),
      });
    }
  }

  onSelectStore(store: AdminStoreSummary): void {
    this.drawerStore.set(store);
    this.isStoreDrawerOpen.set(true);
  }

  onCloseStoreDrawer(): void {
    this.isStoreDrawerOpen.set(false);
    this.drawerStore.set(null);
  }

  onStoreDrawerStatusChange(store: AdminStoreSummary): void {
    if (store.status === 'ACTIVE') {
      this.adminService.suspendStore(store.id).subscribe({
        next: () => this.onCloseStoreDrawer(),
      });
    } else {
      this.adminService.activateStore(store.id).subscribe({
        next: () => this.onCloseStoreDrawer(),
      });
    }
  }

  onFilterSuspendedStores(): void {
    this.adminService.setStoreStatusFilter('SUSPENDED');
    this.router.navigate(['/admin/stores']);
  }
}

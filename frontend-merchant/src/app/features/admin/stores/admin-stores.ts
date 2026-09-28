import { Component, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStoreSummary } from '../../../core/models/admin.models';
import { AdminStoresHeader } from './components/admin-stores-header/admin-stores-header';
import { AdminStoresFilterBar } from './components/admin-stores-filter-bar/admin-stores-filter-bar';
import { AdminStoresTable } from './components/admin-stores-table/admin-stores-table';
import { StoreStatusModal } from './components/store-status-modal/store-status-modal';
import { AdminStoreDrawer } from './components/admin-store-drawer/admin-store-drawer';

@Component({
  imports: [
    AdminStoresHeader,
    AdminStoresFilterBar,
    AdminStoresTable,
    StoreStatusModal,
    AdminStoreDrawer,
  ],
  selector: 'app-admin-stores',
  styleUrl: './admin-stores.css',
  templateUrl: './admin-stores.html',
})
export class AdminStores implements OnInit {
  readonly adminService = inject(AdminService);

  readonly selectedStore = signal<AdminStoreSummary | null>(null);
  readonly isModalOpen = signal<boolean>(false);

  readonly drawerStore = signal<AdminStoreSummary | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

  ngOnInit(): void {
    if (this.adminService.stores().length === 0) {
      this.adminService.loadStores().subscribe();
    }
  }

  onRefresh(): void {
    this.adminService.loadStores(true).subscribe();
  }

  onSearchChange(query: string): void {
    this.adminService.setStoreSearchQuery(query);
  }

  onStatusChange(status: string): void {
    this.adminService.setStoreStatusFilter(status);
  }

  onOpenStatusModal(store: AdminStoreSummary): void {
    this.selectedStore.set(store);
    this.isModalOpen.set(true);
  }

  onCloseModal(): void {
    this.isModalOpen.set(false);
    this.selectedStore.set(null);
  }

  onSelectStore(store: AdminStoreSummary): void {
    this.drawerStore.set(store);
    this.isDrawerOpen.set(true);
  }

  onCloseDrawer(): void {
    this.isDrawerOpen.set(false);
    this.drawerStore.set(null);
  }

  onDrawerStatusChange(store: AdminStoreSummary): void {
    this.onCloseDrawer();
    this.onOpenStatusModal(store);
  }

  onConfirmStatusChange(store: AdminStoreSummary): void {
    if (store.status === 'ACTIVE') {
      this.adminService.suspendStore(store.id).subscribe({
        next: () => this.onCloseModal(),
      });
    } else {
      this.adminService.activateStore(store.id).subscribe({
        next: () => this.onCloseModal(),
      });
    }
  }
}

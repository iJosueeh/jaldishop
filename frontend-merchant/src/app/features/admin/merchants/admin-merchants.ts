import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { AdminMerchantsHeader } from './components/admin-merchants-header/admin-merchants-header';
import { AdminMerchantsFilterBar } from './components/admin-merchants-filter-bar/admin-merchants-filter-bar';
import { AdminMerchantsGrid } from './components/admin-merchants-grid/admin-merchants-grid';
import { UserStatusModal } from '../users/components/user-status-modal/user-status-modal';
import { AdminUserDrawer } from '../users/components/admin-user-drawer/admin-user-drawer';
import { Pagination } from '../../../shared/components/pagination/pagination';

@Component({
  imports: [
    AdminMerchantsHeader,
    AdminMerchantsFilterBar,
    AdminMerchantsGrid,
    UserStatusModal,
    AdminUserDrawer,
    Pagination,
  ],
  selector: 'app-admin-merchants',
  styleUrl: './admin-merchants.css',
  templateUrl: './admin-merchants.html',
})
export class AdminMerchants implements OnInit {
  readonly adminService = inject(AdminService);

  readonly selectedMerchant = signal<AdminUserSummary | null>(null);
  readonly isModalOpen = signal<boolean>(false);

  readonly drawerMerchant = signal<AdminUserSummary | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(9); // 3x3 grid layout

  readonly paginatedMerchants = computed(() => {
    const list = this.adminService.filteredMerchants();
    const start = (this.currentPage() - 1) * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    if (this.adminService.users().length === 0) {
      this.adminService.loadUsers().subscribe();
    }
  }

  onRefresh(): void {
    this.adminService.loadUsers(true).subscribe();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onSearchChange(query: string): void {
    this.currentPage.set(1);
    this.adminService.setMerchantSearchQuery(query);
  }

  onStatusChange(status: string): void {
    this.currentPage.set(1);
    this.adminService.setMerchantStatusFilter(status);
  }

  onOpenStatusModal(merchant: AdminUserSummary): void {
    this.selectedMerchant.set(merchant);
    this.isModalOpen.set(true);
  }

  onCloseModal(): void {
    this.isModalOpen.set(false);
    this.selectedMerchant.set(null);
  }

  onSelectMerchant(merchant: AdminUserSummary): void {
    this.drawerMerchant.set(merchant);
    this.isDrawerOpen.set(true);
  }

  onCloseDrawer(): void {
    this.isDrawerOpen.set(false);
    this.drawerMerchant.set(null);
  }

  onDrawerStatusChange(merchant: AdminUserSummary): void {
    this.onCloseDrawer();
    this.onOpenStatusModal(merchant);
  }

  onConfirmStatusChange(merchant: AdminUserSummary): void {
    if (merchant.status === 'ACTIVE') {
      this.adminService.suspendUser(merchant.id).subscribe({
        next: () => this.onCloseModal(),
      });
    } else {
      this.adminService.activateUser(merchant.id).subscribe({
        next: () => this.onCloseModal(),
      });
    }
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { AdminMerchantsHeader } from './components/admin-merchants-header/admin-merchants-header';
import { AdminMerchantsGrid } from './components/admin-merchants-grid/admin-merchants-grid';
import { UserStatusModal } from '../users/components/user-status-modal/user-status-modal';

@Component({
  imports: [
    AdminMerchantsHeader,
    AdminMerchantsGrid,
    UserStatusModal,
  ],
  selector: 'app-admin-merchants',
  styleUrl: './admin-merchants.css',
  templateUrl: './admin-merchants.html',
})
export class AdminMerchants implements OnInit {
  readonly adminService = inject(AdminService);

  readonly selectedMerchant = signal<AdminUserSummary | null>(null);
  readonly isModalOpen = signal<boolean>(false);

  ngOnInit(): void {
    if (this.adminService.users().length === 0) {
      this.adminService.loadUsers().subscribe();
    }
  }

  onRefresh(): void {
    this.adminService.loadUsers(true).subscribe();
  }

  onOpenStatusModal(merchant: AdminUserSummary): void {
    this.selectedMerchant.set(merchant);
    this.isModalOpen.set(true);
  }

  onCloseModal(): void {
    this.isModalOpen.set(false);
    this.selectedMerchant.set(null);
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

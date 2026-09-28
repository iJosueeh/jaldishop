import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { AdminUsersHeader } from './components/admin-users-header/admin-users-header';
import { AdminUsersFilterBar } from './components/admin-users-filter-bar/admin-users-filter-bar';
import { AdminUsersTable } from './components/admin-users-table/admin-users-table';
import { AdminUserDrawer } from './components/admin-user-drawer/admin-user-drawer';
import { UserStatusModal } from './components/user-status-modal/user-status-modal';
import { Pagination } from '../../../shared/components/pagination/pagination';

@Component({
  imports: [
    AdminUsersHeader,
    AdminUsersFilterBar,
    AdminUsersTable,
    AdminUserDrawer,
    UserStatusModal,
    Pagination,
  ],
  selector: 'app-admin-users',
  styleUrl: './admin-users.css',
  templateUrl: './admin-users.html',
})
export class AdminUsers implements OnInit {
  readonly adminService = inject(AdminService);

  readonly selectedUser = signal<AdminUserSummary | null>(null);
  readonly isModalOpen = signal<boolean>(false);
  readonly isDrawerOpen = signal<boolean>(false);

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  readonly paginatedUsers = computed(() => {
    const list = this.adminService.filteredUsers();
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
    this.adminService.setUserSearchQuery(query);
  }

  onRoleChange(role: string): void {
    this.currentPage.set(1);
    this.adminService.setUserRoleFilter(role);
  }

  onStatusChange(status: string): void {
    this.currentPage.set(1);
    this.adminService.setUserStatusFilter(status);
  }

  onOpenStatusModal(user: AdminUserSummary): void {
    this.selectedUser.set(user);
    this.isModalOpen.set(true);
  }

  onCloseModal(): void {
    this.isModalOpen.set(false);
    this.selectedUser.set(null);
  }

  onOpenUserDrawer(user: AdminUserSummary): void {
    this.selectedUser.set(user);
    this.isDrawerOpen.set(true);
  }

  onCloseUserDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedUser.set(null);
  }

  onConfirmStatusChange(user: AdminUserSummary): void {
    if (user.status === 'ACTIVE') {
      this.adminService.suspendUser(user.id).subscribe({
        next: () => this.onCloseModal(),
      });
    } else {
      this.adminService.activateUser(user.id).subscribe({
        next: () => this.onCloseModal(),
      });
    }
  }
}

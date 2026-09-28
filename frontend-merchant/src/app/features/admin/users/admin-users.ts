import { Component, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { AdminUsersHeader } from './components/admin-users-header/admin-users-header';
import { AdminUsersFilterBar } from './components/admin-users-filter-bar/admin-users-filter-bar';
import { AdminUsersTable } from './components/admin-users-table/admin-users-table';
import { AdminUserDrawer } from './components/admin-user-drawer/admin-user-drawer';
import { UserStatusModal } from './components/user-status-modal/user-status-modal';

@Component({
  imports: [
    AdminUsersHeader,
    AdminUsersFilterBar,
    AdminUsersTable,
    AdminUserDrawer,
    UserStatusModal,
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

  ngOnInit(): void {
    if (this.adminService.users().length === 0) {
      this.adminService.loadUsers().subscribe();
    }
  }

  onRefresh(): void {
    this.adminService.loadUsers(true).subscribe();
  }

  onSearchChange(query: string): void {
    this.adminService.setUserSearchQuery(query);
  }

  onRoleChange(role: string): void {
    this.adminService.setUserRoleFilter(role);
  }

  onStatusChange(status: string): void {
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

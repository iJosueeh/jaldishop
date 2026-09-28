import { Component, computed, inject, OnInit } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminDashboardKpis } from './components/admin-dashboard-kpis/admin-dashboard-kpis';
import { AdminQuickUsers } from './components/admin-quick-users/admin-quick-users';
import { AdminQuickStores } from './components/admin-quick-stores/admin-quick-stores';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [AdminDashboardKpis, AdminQuickUsers, AdminQuickStores, NgIcon],
  providers: [provideIcons({ matRefreshOutline })],
  selector: 'app-admin-dashboard',
  styleUrl: './admin-dashboard.css',
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit {
  readonly adminService = inject(AdminService);

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
}

import { Component, inject, input, OnInit, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matDashboardOutline,
  matGroupOutline,
  matStorefrontOutline,
  matPersonOutline,
  matSettingsOutline,
  matCloseOutline,
  matLogoutOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, RouterLinkActive, NgIcon],
  providers: [
    provideIcons({
      matDashboardOutline,
      matGroupOutline,
      matStorefrontOutline,
      matPersonOutline,
      matSettingsOutline,
      matCloseOutline,
      matLogoutOutline,
    }),
  ],
  selector: 'app-admin-sidebar',
  styleUrl: './admin-sidebar.css',
  templateUrl: './admin-sidebar.html',
})
export class AdminSidebar implements OnInit {
  readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);

  readonly isOpen = input<boolean>(false);
  readonly closeSidebar = output<void>();

  ngOnInit(): void {
    if (this.adminService.users().length === 0) {
      this.adminService.loadUsers().subscribe();
    }
    if (this.adminService.stores().length === 0) {
      this.adminService.loadStores().subscribe();
    }
  }

  logout(): void {
    this.authService.logout();
  }
}

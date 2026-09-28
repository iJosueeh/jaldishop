import { Component, computed, inject, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { ProfileService } from '../../../../core/services/profile.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matMenuOutline,
  matShieldOutline,
  matStorefrontOutline,
  matLogoutOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matMenuOutline,
      matShieldOutline,
      matStorefrontOutline,
      matLogoutOutline,
    }),
  ],
  selector: 'app-admin-header',
  styleUrl: './admin-header.css',
  templateUrl: './admin-header.html',
})
export class AdminHeader {
  readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);

  readonly toggleSidebar = output<void>();

  readonly adminName = computed(
    () => this.profileService.fullName() || this.authService.currentUser()?.fullName || 'Administrador',
  );

  readonly adminInitials = computed(() => {
    const name = this.adminName();
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  });

  logout(): void {
    this.authService.logout();
  }
}

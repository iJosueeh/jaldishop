import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLogoutOutline,
  matShieldOutline,
  matSwitchAccountOutline,
  matStorefrontOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matLogoutOutline,
      matShieldOutline,
      matSwitchAccountOutline,
      matStorefrontOutline,
    }),
  ],
  selector: 'app-unauthorized',
  styleUrl: './unauthorized.css',
  templateUrl: './unauthorized.html',
})
export class Unauthorized {
  private readonly authService = inject(AuthService);

  readonly isLogged = this.authService.isAuthenticated;

  handleLogout(): void {
    this.authService.logout();
  }
}


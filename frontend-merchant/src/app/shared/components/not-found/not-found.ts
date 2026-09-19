import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth-service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matTravelExploreOutline,
  matHomeOutline,
  matStorefrontOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matTravelExploreOutline,
      matHomeOutline,
      matStorefrontOutline,
    }),
  ],
  selector: 'app-not-found',
  styleUrl: './not-found.css',
  templateUrl: './not-found.html',
})
export class NotFound {
  private readonly authService = inject(AuthService);

  readonly isAuthenticated = computed(() => this.authService.isAuthenticated());
}


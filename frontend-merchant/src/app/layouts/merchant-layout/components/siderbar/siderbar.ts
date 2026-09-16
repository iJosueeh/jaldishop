import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { StoreService } from '../../../../features/store/services/store.service';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-siderbar',
  styleUrl: './siderbar.css',
  templateUrl: './siderbar.html',
})
export class Siderbar {
  private readonly authService = inject(AuthService);
  private readonly storeService = inject(StoreService);

  readonly userName = computed(() => this.authService.currentUser()?.fullName ?? 'Mi cuenta');

  readonly storeName = computed(() => this.storeService.storeName());

  handleLogout(): void {
    this.authService.logout();
  }
}

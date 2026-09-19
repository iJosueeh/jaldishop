import { Component, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { StoreService } from '../../../../features/store/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-siderbar',
  styleUrl: './siderbar.css',
  templateUrl: './siderbar.html',
})
export class Siderbar {
  private readonly authService = inject(AuthService);
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);

  readonly activeOrdersCount = signal<number>(0);
  readonly capacityOccupied = signal<number>(0);
  readonly capacityTotal = signal<number>(0);

  readonly isOpen = input<boolean>(false);
  readonly closeSidebar = output<void>();

  readonly userName = computed(
    () => this.profileService.fullName() || this.authService.currentUser()?.fullName || 'Mi cuenta',
  );
  readonly storeName = computed(() => this.storeService.storeName());
  readonly userInitials = computed(() => this.profileService.userInitials());

  readonly capacityBlocks = computed(() => {
    const total = this.capacityTotal();
    const occupied = this.capacityOccupied();
    const percentage = total > 0 ? (occupied / total) * 10 : 0;
    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.round(percentage),
    }));
  });

  handleLogout(): void {
    this.authService.logout();
  }
}

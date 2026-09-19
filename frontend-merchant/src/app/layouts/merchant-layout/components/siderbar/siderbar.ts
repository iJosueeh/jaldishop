import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { CapacityService } from '../../../../core/services/capacity.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matCloseOutline,
  matHomeOutline,
  matStoreOutline,
  matReceiptLongOutline,
  matViewWeekOutline,
  matBakeryDiningOutline,
  matGroupOutline,
  matTuneOutline,
  matLogoutOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, RouterLinkActive, NgIcon],
  providers: [
    provideIcons({
      matStorefrontOutline,
      matCloseOutline,
      matHomeOutline,
      matStoreOutline,
      matReceiptLongOutline,
      matViewWeekOutline,
      matBakeryDiningOutline,
      matGroupOutline,
      matTuneOutline,
      matLogoutOutline,
    }),
  ],
  selector: 'app-siderbar',
  styleUrl: './siderbar.css',
  templateUrl: './siderbar.html',
})
export class Siderbar implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);
  private readonly capacityService = inject(CapacityService);

  readonly activeOrdersCount = signal<number>(0);
  readonly capacityOccupied = signal<number>(0);

  readonly capacityTotal = computed(() => this.capacityService.todayTotalCapacity());

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

  ngOnInit(): void {
    if (this.capacityService.configurations().length === 0) {
      this.capacityService.getConfigurations().subscribe();
    }
  }

  handleLogout(): void {
    this.authService.logout();
  }
}

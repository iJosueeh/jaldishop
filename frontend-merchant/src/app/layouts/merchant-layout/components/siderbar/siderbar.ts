import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { CapacityService } from '../../../../core/services/capacity.service';
import { isValidImageUrl } from '../../../../core/utils/image.utils';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matCloseOutline,
  matHomeOutline,
  matStoreOutline,
  matReceiptLongOutline,
  matViewWeekOutline,
  matInventory2Outline,
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
      matInventory2Outline,
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
  readonly storeLogoUrl = computed(() => this.storeService.currentStore()?.logoUrl);
  readonly logoLoadError = signal<boolean>(false);
  readonly hasValidLogo = computed(() => {
    const url = this.storeLogoUrl();
    return isValidImageUrl(url) && !this.logoLoadError();
  });
  readonly userInitials = computed(() => this.profileService.userInitials());

  onLogoError(): void {
    this.logoLoadError.set(true);
  }

  readonly capacityBlocks = computed(() => {
    const total = this.capacityTotal();
    const occupied = this.capacityOccupied();
    const percentage = total > 0 ? (occupied / total) * 10 : 0;
    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.round(percentage),
    }));
  });

  readonly storeOperationalStatus = computed<{
    label: string;
    dotClass: string;
    footerLabel: string;
    footerClass: string;
  }>(() => {
    const store = this.storeService.currentStore();
    if (!store || store.status !== 'ACTIVE') {
      return {
        label: 'Pausada',
        dotClass: 'bg-rose-500',
        footerLabel: 'Tienda en pausa',
        footerClass: 'text-rose-400 bg-rose-500/15 border-rose-500/25',
      };
    }

    if (this.capacityService.isTodayClosed() || this.capacityService.todayEffectiveCapacity() === 0) {
      return {
        label: 'Cerrado hoy',
        dotClass: 'bg-stone-400',
        footerLabel: 'Solo pedidos programados',
        footerClass: 'text-stone-400 bg-white/5 border-white/10',
      };
    }

    const occupied = this.capacityOccupied();
    const capacity = this.capacityService.todayEffectiveCapacity();

    if (capacity > 0 && occupied >= capacity) {
      return {
        label: 'Lleno por hoy',
        dotClass: 'bg-amber-500',
        footerLabel: 'Cupos agotados hoy',
        footerClass: 'text-amber-400 bg-amber-500/15 border-amber-500/25',
      };
    }

    return {
      label: 'Abierto',
      dotClass: 'bg-emerald-400 animate-pulse',
      footerLabel: 'Recibiendo pedidos',
      footerClass: 'text-emerald-400 bg-emerald-500/15 border-emerald-500/25',
    };
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

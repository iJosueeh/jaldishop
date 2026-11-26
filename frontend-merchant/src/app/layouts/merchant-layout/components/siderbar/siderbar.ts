import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth-service';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { CapacityService } from '../../../../core/services/capacity.service';
import { OrderService } from '../../../../core/services/order.service';
import { InventoryService } from '../../../../core/services/inventory.service';
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
  matWarehouseOutline,
  matGroupOutline,
  matTuneOutline,
  matLogoutOutline,
  matPaymentsOutline,
  matSellOutline,
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
      matWarehouseOutline,
      matGroupOutline,
      matTuneOutline,
      matLogoutOutline,
      matPaymentsOutline,
      matSellOutline,
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
  readonly orderService = inject(OrderService);
  readonly inventoryService = inject(InventoryService);

  readonly lowStockAlertsCount = computed<number>(() => {
    return this.inventoryService.lowStock().length;
  });

  readonly activeOrdersCount = computed<number>(() => {
    return this.orderService.activeOrdersTodayCount();
  });
  readonly capacityOccupied = computed(() => this.capacityService.todayOccupied());
  readonly capacityTotal = computed(() => this.capacityService.todayEffectiveCapacityReal());
  readonly availableCapacity = computed(() => this.capacityService.todayAvailable());
  readonly isOverCapacity = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    return occupied !== null && total > 0 && occupied > total;
  });

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
    const percentage = occupied !== null && total > 0 ? (occupied / total) * 10 : 0;
    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: occupied !== null && i < Math.min(10, Math.round(percentage)),
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

    if (
      this.capacityService.isTodayClosedReal() ||
      this.capacityService.todayEffectiveCapacityReal() === 0
    ) {
      return {
        label: 'Cerrado hoy',
        dotClass: 'bg-stone-400',
        footerLabel: 'Solo pedidos programados',
        footerClass: 'text-stone-400 bg-white/5 border-white/10',
      };
    }

    const occupied = this.capacityOccupied();
    const capacity = this.capacityTotal();

    if (occupied !== null && capacity > 0 && occupied >= capacity) {
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
    this.capacityService.loadDailyOccupancy().subscribe();
    this.orderService.loadTodayOrderCounts().subscribe();
  }

  handleLogout(): void {
    this.authService.logout();
  }
}

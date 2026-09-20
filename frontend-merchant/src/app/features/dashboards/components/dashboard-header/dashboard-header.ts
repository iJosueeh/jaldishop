import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { ToastService } from '../../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matShareOutline,
  matStorefrontOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matShareOutline,
      matStorefrontOutline,
    }),
  ],
  selector: 'app-dashboard-header',
  styleUrl: './dashboard-header.css',
  templateUrl: './dashboard-header.html',
})
export class DashboardHeader {
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);
  private readonly toastService = inject(ToastService);

  readonly firstName = computed(() => {
    const profile = this.profileService.currentProfile();
    return profile?.firstName || 'Comerciante';
  });

  readonly storeStatus = computed(() => this.storeService.storeStatus());
  readonly isOpen = computed(() => this.storeStatus() === 'ACTIVE');

  readonly currentDate = computed(() => {
    const today = new Date();
    return today.toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    });
  });

  copyStoreLink(): void {
    const store = this.storeService.currentStore();
    const slug = store?.slug;
    const url = slug
      ? `${window.location.origin}/store/${slug}`
      : window.location.origin;

    if (navigator?.clipboard) {
      navigator.clipboard.writeText(url).then(() => {
        this.toastService.success('¡Enlace del catálogo copiado al portapapeles!');
      }).catch(() => {
        this.toastService.info(`Enlace: ${url}`);
      });
    } else {
      this.toastService.info(`Enlace: ${url}`);
    }
  }
}

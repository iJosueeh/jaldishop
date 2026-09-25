import { Component, computed, inject } from '@angular/core';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPublicOutline,
  matPauseCircleOutline,
  matPlayCircleOutline,
  matCreditCardOutline,
  matScheduleOutline,
  matPaymentsOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matPublicOutline,
      matPauseCircleOutline,
      matPlayCircleOutline,
      matCreditCardOutline,
      matScheduleOutline,
      matPaymentsOutline,
    }),
  ],
  selector: 'app-settings-general-tab',
  styleUrl: './settings-general-tab.css',
  templateUrl: './settings-general-tab.html',
})
export class SettingsGeneralTab {
  readonly settingsService = inject(SettingsService);
  readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);

  readonly general = computed(() => this.settingsService.settings().general);
  readonly currentStore = computed(() => this.storeService.currentStore());

  readonly isStoreActive = computed(() => {
    return this.currentStore()?.status === 'ACTIVE';
  });

  toggleStoreStatus(): void {
    const store = this.currentStore();
    if (!store) return;

    const nextStatus = store.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    // We update through storeService if available, otherwise notify
    this.toastService.info(
      nextStatus === 'ACTIVE'
        ? 'Tienda activada: Ya estás recibiendo pedidos.'
        : 'Tienda en pausa: No se aceptarán nuevos pedidos hasta que la reactives.'
    );
  }
}


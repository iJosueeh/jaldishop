import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matNotificationsOutline,
  matVolumeUpOutline,
  matVolumeMuteOutline,
  matPlayArrowOutline,
  matInventory2Outline,
  matSaveOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-settings-alerts-tab',
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matNotificationsOutline,
      matVolumeUpOutline,
      matVolumeMuteOutline,
      matPlayArrowOutline,
      matInventory2Outline,
      matSaveOutline,
    }),
  ],
  templateUrl: './settings-alerts-tab.html',
  styleUrl: './settings-alerts-tab.css',
})
export class SettingsAlertsTab {
  readonly settingsService = inject(SettingsService);
  private readonly storeService = inject(StoreService);

  readonly soundAlert = signal<boolean>(
    this.settingsService.settings().orderSoundAlert
  );
  readonly soundVolume = signal<number>(
    this.settingsService.settings().soundVolume ?? 80
  );
  readonly lowStockAlert = signal<boolean>(
    this.settingsService.settings().lowStockVisualAlert
  );

  constructor() {
    effect(() => {
      const s = this.settingsService.settings();
      this.soundAlert.set(s.orderSoundAlert);
      this.soundVolume.set(s.soundVolume ?? 80);
      this.lowStockAlert.set(s.lowStockVisualAlert);
    });
  }

  playTestSound(): void {
    this.settingsService.playNotificationSound();
  }

  saveAlerts(): void {
    const current = this.settingsService.settings();
    const storeId = this.storeService.currentStore()?.id;

    this.settingsService.saveSettings(
      {
        ...current,
        orderSoundAlert: this.soundAlert(),
        soundVolume: this.soundVolume(),
        lowStockVisualAlert: this.lowStockAlert(),
      },
      storeId
    );
  }
}

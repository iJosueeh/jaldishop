import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matNotificationsOutline,
  matVolumeUpOutline,
  matVolumeMuteOutline,
  matInventory2Outline,
  matMailOutline,
  matPlayArrowOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matNotificationsOutline,
      matVolumeUpOutline,
      matVolumeMuteOutline,
      matInventory2Outline,
      matMailOutline,
      matPlayArrowOutline,
    }),
  ],
  selector: 'app-settings-notifications-tab',
  styleUrl: './settings-notifications-tab.css',
  templateUrl: './settings-notifications-tab.html',
})
export class SettingsNotificationsTab {
  private readonly settingsService = inject(SettingsService);

  readonly notifications = computed(() => this.settingsService.settings().notifications);

  toggleOrderSound(): void {
    const current = this.notifications().orderSoundAlert;
    this.settingsService.updateNotifications({ orderSoundAlert: !current });
    if (!current) {
      this.playTestSound();
    }
  }

  toggleLowStock(): void {
    const current = this.notifications().lowStockVisualAlert;
    this.settingsService.updateNotifications({ lowStockVisualAlert: !current });
  }

  toggleDailyEmail(): void {
    const current = this.notifications().dailyEmailSummary;
    this.settingsService.updateNotifications({ dailyEmailSummary: !current });
  }

  updateVolume(volume: number): void {
    this.settingsService.updateNotifications({ soundVolume: volume });
  }

  playTestSound(): void {
    this.settingsService.playNotificationSound();
  }
}


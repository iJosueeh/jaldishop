import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matNotificationsOutline,
  matMailOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matNotificationsOutline,
      matMailOutline,
    }),
  ],
  selector: 'app-admin-settings-notifications-tab',
  styleUrl: './admin-settings-notifications-tab.css',
  templateUrl: './admin-settings-notifications-tab.html',
})
export class AdminSettingsNotificationsTab {
  readonly settings = input.required<AdminPlatformSettings>();
  readonly settingsChange = output<Partial<AdminPlatformSettings>>();

  toggleNotifyNewStore(): void {
    this.settingsChange.emit({
      notifyOnNewStoreRegistration: !this.settings().notifyOnNewStoreRegistration,
    });
  }

  toggleNotifySuspension(): void {
    this.settingsChange.emit({
      notifyOnStoreSuspension: !this.settings().notifyOnStoreSuspension,
    });
  }

  onEmailChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.settingsChange.emit({ adminAlertEmail: value });
  }
}

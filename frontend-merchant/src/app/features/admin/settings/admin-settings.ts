import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminPlatformSettings } from '../../../core/models/admin.models';
import { AdminSettingsHeader } from './components/admin-settings-header/admin-settings-header';
import { AdminSettingsGeneralTab } from './components/admin-settings-general-tab/admin-settings-general-tab';
import { AdminSettingsSecurityTab } from './components/admin-settings-security-tab/admin-settings-security-tab';
import { AdminSettingsNotificationsTab } from './components/admin-settings-notifications-tab/admin-settings-notifications-tab';
import { AdminSettingsSystemTab } from './components/admin-settings-system-tab/admin-settings-system-tab';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matTuneOutline,
  matShieldOutline,
  matNotificationsOutline,
  matInfoOutline,
} from '@ng-icons/material-symbols/outline';

export type AdminSettingsTabType = 'general' | 'security' | 'notifications' | 'system';

@Component({
  imports: [
    CommonModule,
    AdminSettingsHeader,
    AdminSettingsGeneralTab,
    AdminSettingsSecurityTab,
    AdminSettingsNotificationsTab,
    AdminSettingsSystemTab,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matTuneOutline,
      matShieldOutline,
      matNotificationsOutline,
      matInfoOutline,
    }),
  ],
  selector: 'app-admin-settings',
  styleUrl: './admin-settings.css',
  templateUrl: './admin-settings.html',
})
export class AdminSettings {
  readonly adminService = inject(AdminService);

  readonly currentTab = signal<AdminSettingsTabType>('general');

  setTab(tab: AdminSettingsTabType): void {
    this.currentTab.set(tab);
  }

  onUpdateSettings(partial: Partial<AdminPlatformSettings>): void {
    this.adminService.updateSettings(partial);
  }

  onSaveSettings(): void {
    this.adminService.saveSettings();
  }
}

import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matTuneOutline,
  matStorefrontOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matTuneOutline,
      matStorefrontOutline,
      matWarningOutline,
    }),
  ],
  selector: 'app-admin-settings-general-tab',
  styleUrl: './admin-settings-general-tab.css',
  templateUrl: './admin-settings-general-tab.html',
})
export class AdminSettingsGeneralTab {
  readonly settings = input.required<AdminPlatformSettings>();
  readonly settingsChange = output<Partial<AdminPlatformSettings>>();

  toggleMerchantRegistration(): void {
    this.settingsChange.emit({
      allowMerchantRegistration: !this.settings().allowMerchantRegistration,
    });
  }

  toggleStoreApproval(): void {
    this.settingsChange.emit({
      requireStoreApproval: !this.settings().requireStoreApproval,
    });
  }

  toggleMaintenanceMode(): void {
    this.settingsChange.emit({
      maintenanceMode: !this.settings().maintenanceMode,
    });
  }

  onNoticeChange(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.settingsChange.emit({ maintenanceNotice: value });
  }
}

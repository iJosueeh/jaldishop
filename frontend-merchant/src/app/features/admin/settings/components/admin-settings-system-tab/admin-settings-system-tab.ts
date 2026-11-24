import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matTuneOutline,
  matBoltOutline,
  matCheckCircleOutline,
  matStorefrontOutline,
  matPaymentsOutline,
  matShieldOutline,
  matInfoOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matTuneOutline,
      matBoltOutline,
      matCheckCircleOutline,
      matStorefrontOutline,
      matPaymentsOutline,
      matShieldOutline,
      matInfoOutline,
    }),
  ],
  selector: 'app-admin-settings-system-tab',
  styleUrl: './admin-settings-system-tab.css',
  templateUrl: './admin-settings-system-tab.html',
})
export class AdminSettingsSystemTab {
  readonly settings = input.required<AdminPlatformSettings>();
  readonly settingsChange = output<Partial<AdminPlatformSettings>>();

  onToggleMaintenance(): void {
    const nextVal = !this.settings().maintenanceMode;
    this.settingsChange.emit({ maintenanceMode: nextVal });
  }

  onToggleRegistration(): void {
    const nextVal = !this.settings().allowMerchantRegistration;
    this.settingsChange.emit({ allowMerchantRegistration: nextVal });
  }

  onToggleApproval(): void {
    const nextVal = !this.settings().requireStoreApproval;
    this.settingsChange.emit({ requireStoreApproval: nextVal });
  }

  onCommissionChange(event: Event): void {
    const val = parseFloat((event.target as HTMLInputElement).value);
    if (!isNaN(val) && val >= 0 && val <= 100) {
      this.settingsChange.emit({ marketplaceCommissionRate: val });
    }
  }

  onNoticeChange(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.settingsChange.emit({ maintenanceNotice: val });
  }
}

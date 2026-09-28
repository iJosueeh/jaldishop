import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matShieldOutline,
  matLockOutline,
  matTimerOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matShieldOutline,
      matLockOutline,
      matTimerOutline,
    }),
  ],
  selector: 'app-admin-settings-security-tab',
  styleUrl: './admin-settings-security-tab.css',
  templateUrl: './admin-settings-security-tab.html',
})
export class AdminSettingsSecurityTab {
  readonly settings = input.required<AdminPlatformSettings>();
  readonly settingsChange = output<Partial<AdminPlatformSettings>>();

  toggleEnforce2FA(): void {
    this.settingsChange.emit({
      enforce2FAForAdmins: !this.settings().enforce2FAForAdmins,
    });
  }

  onTimeoutChange(event: Event): void {
    const value = parseInt((event.target as HTMLSelectElement).value, 10);
    this.settingsChange.emit({ sessionTimeoutHours: value });
  }
}

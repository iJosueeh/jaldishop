import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matInfoOutline,
  matBoltOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matInfoOutline,
      matBoltOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-admin-settings-system-tab',
  styleUrl: './admin-settings-system-tab.css',
  templateUrl: './admin-settings-system-tab.html',
})
export class AdminSettingsSystemTab {
  readonly settings = input.required<AdminPlatformSettings>();
}

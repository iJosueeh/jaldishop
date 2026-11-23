import { Component, OnInit, inject, signal } from '@angular/core';
import { SettingsHeader } from './components/settings-header/settings-header';
import { SettingsPaymentsDeliveryTab } from './components/settings-payments-delivery-tab/settings-payments-delivery-tab';
import { SettingsPoliciesTab } from './components/settings-policies-tab/settings-policies-tab';
import { SettingsAlertsTab } from './components/settings-alerts-tab/settings-alerts-tab';
import { SettingsService } from '../../core/services/settings.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPaymentsOutline,
  matGavelOutline,
  matNotificationsOutline,
} from '@ng-icons/material-symbols/outline';

export type SettingsTabType = 'payments' | 'policies' | 'alerts';

@Component({
  imports: [
    SettingsHeader,
    SettingsPaymentsDeliveryTab,
    SettingsPoliciesTab,
    SettingsAlertsTab,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matPaymentsOutline,
      matGavelOutline,
      matNotificationsOutline,
    }),
  ],
  selector: 'app-settings',
  styleUrl: './settings.css',
  templateUrl: './settings.html',
})
export class Settings implements OnInit {
  private readonly settingsService = inject(SettingsService);
  readonly currentTab = signal<SettingsTabType>('payments');

  ngOnInit(): void {
    this.settingsService.loadSettings();
  }

  setTab(tab: SettingsTabType): void {
    this.currentTab.set(tab);
  }
}

import { Component, signal } from '@angular/core';
import { SettingsHeader } from './components/settings-header/settings-header';
import { SettingsWhatsappTab } from './components/settings-whatsapp-tab/settings-whatsapp-tab';
import { SettingsPaymentsTab } from './components/settings-payments-tab/settings-payments-tab';
import { SettingsOrdersTab } from './components/settings-orders-tab/settings-orders-tab';
import { SettingsNotificationsTab } from './components/settings-notifications-tab/settings-notifications-tab';
import { SettingsPoliciesTab } from './components/settings-policies-tab/settings-policies-tab';
import { SettingsGeneralTab } from './components/settings-general-tab/settings-general-tab';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matChatOutline,
  matPaymentsOutline,
  matShoppingBagOutline,
  matNotificationsOutline,
  matGavelOutline,
  matTuneOutline,
} from '@ng-icons/material-symbols/outline';

export type SettingsTabType =
  | 'whatsapp'
  | 'payments'
  | 'orders'
  | 'notifications'
  | 'policies'
  | 'general';

@Component({
  imports: [
    SettingsHeader,
    SettingsWhatsappTab,
    SettingsPaymentsTab,
    SettingsOrdersTab,
    SettingsNotificationsTab,
    SettingsPoliciesTab,
    SettingsGeneralTab,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matChatOutline,
      matPaymentsOutline,
      matShoppingBagOutline,
      matNotificationsOutline,
      matGavelOutline,
      matTuneOutline,
    }),
  ],
  selector: 'app-settings',
  styleUrl: './settings.css',
  templateUrl: './settings.html',
})
export class Settings {
  readonly currentTab = signal<SettingsTabType>('whatsapp');

  setTab(tab: SettingsTabType): void {
    this.currentTab.set(tab);
  }
}



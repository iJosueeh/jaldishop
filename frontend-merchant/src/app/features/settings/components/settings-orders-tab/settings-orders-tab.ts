import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matShoppingBagOutline,
  matTimerOutline,
  matPaymentsOutline,
  matChatOutline,
  matSaveOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matShoppingBagOutline,
      matTimerOutline,
      matPaymentsOutline,
      matChatOutline,
      matSaveOutline,
    }),
  ],
  selector: 'app-settings-orders-tab',
  styleUrl: './settings-orders-tab.css',
  templateUrl: './settings-orders-tab.html',
})
export class SettingsOrdersTab {
  private readonly settingsService = inject(SettingsService);

  readonly orderPreferences = computed(() => this.settingsService.settings().orderPreferences);

  readonly hasMinAmount = signal<boolean>(false);
  readonly minAmount = signal<number | null>(null);
  readonly postCheckoutMsg = signal<string>('');
  readonly prepTimeMinutes = signal<number>(30);

  constructor() {
    const prefs = this.orderPreferences();
    this.hasMinAmount.set(prefs.minDeliveryAmount !== null && prefs.minDeliveryAmount > 0);
    this.minAmount.set(prefs.minDeliveryAmount ?? 15);
    this.postCheckoutMsg.set(prefs.postCheckoutMessage);
    this.prepTimeMinutes.set(prefs.estimatedPreparationTimeMinutes);
  }

  toggleHasMinAmount(): void {
    const next = !this.hasMinAmount();
    this.hasMinAmount.set(next);
    if (!next) {
      this.minAmount.set(null);
    } else if (this.minAmount() === null || (this.minAmount() ?? 0) <= 0) {
      this.minAmount.set(15);
    }
  }

  onSave(): void {
    this.settingsService.updateOrderPreferences({
      minDeliveryAmount: this.hasMinAmount() ? this.minAmount() : null,
      postCheckoutMessage: this.postCheckoutMsg().trim(),
      estimatedPreparationTimeMinutes: this.prepTimeMinutes(),
    });
  }
}


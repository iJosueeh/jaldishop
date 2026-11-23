import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCreditCardOutline,
  matLocalShippingOutline,
  matAccountBalanceOutline,
  matTimerOutline,
  matPaymentsOutline,
  matSaveOutline,
  matInfoOutline,
  matCheckCircleOutline,
  matSpeedOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-settings-payments-delivery-tab',
  imports: [FormsModule, RouterLink, NgIcon],
  providers: [
    provideIcons({
      matCreditCardOutline,
      matLocalShippingOutline,
      matAccountBalanceOutline,
      matTimerOutline,
      matPaymentsOutline,
      matSaveOutline,
      matInfoOutline,
      matCheckCircleOutline,
      matSpeedOutline,
    }),
  ],
  templateUrl: './settings-payments-delivery-tab.html',
  styleUrl: './settings-payments-delivery-tab.css',
})
export class SettingsPaymentsDeliveryTab {
  readonly settingsService = inject(SettingsService);
  private readonly storeService = inject(StoreService);

  readonly onlinePayments = signal<boolean>(this.settingsService.settings().onlinePaymentsEnabled);
  readonly codEnabled = signal<boolean>(this.settingsService.settings().cashOnDeliveryEnabled);
  readonly acceptsPos = signal<boolean>(this.settingsService.settings().acceptsPosOnDelivery);
  readonly hasMinAmount = signal<boolean>(this.settingsService.settings().minDeliveryAmount !== null);
  readonly minAmount = signal<number | null>(this.settingsService.settings().minDeliveryAmount);
  readonly prepTimeMinutes = signal<number>(this.settingsService.settings().estimatedPrepTimeMinutes || 30);

  readonly noPaymentMethods = computed(() => !this.onlinePayments() && !this.codEnabled());
  readonly invalidMinAmount = computed(() => this.hasMinAmount() && (this.minAmount() === null || this.minAmount()! <= 0));
  readonly invalidPrepTime = computed(() => this.prepTimeMinutes() < 5 || this.prepTimeMinutes() > 480);
  readonly hasErrors = computed(() => this.noPaymentMethods() || this.invalidMinAmount() || this.invalidPrepTime());

  readonly prepTimeLabel = computed(() => {
    const mins = this.prepTimeMinutes();
    if (mins <= 20) return 'Ritmo Rápido';
    if (mins <= 40) return 'Ritmo Estándar';
    return 'Alta Demanda / Taller';
  });

  toggleMinAmount(): void {
    const next = !this.hasMinAmount();
    this.hasMinAmount.set(next);
    if (!next) {
      this.minAmount.set(null);
    } else if (this.minAmount() === null) {
      this.minAmount.set(15.0);
    }
  }

  selectMinAmountPreset(value: number): void {
    this.hasMinAmount.set(true);
    this.minAmount.set(value);
  }

  selectPrepTimePreset(minutes: number): void {
    this.prepTimeMinutes.set(minutes);
  }

  saveChanges(): void {
    if (this.hasErrors()) {
      return;
    }

    const current = this.settingsService.settings();
    const storeId = this.storeService.currentStore()?.id;

    this.settingsService.saveSettings(
      {
        ...current,
        onlinePaymentsEnabled: this.onlinePayments(),
        cashOnDeliveryEnabled: this.codEnabled(),
        acceptsPosOnDelivery: this.codEnabled() ? this.acceptsPos() : false,
        minDeliveryAmount: this.hasMinAmount() ? (this.minAmount() ?? null) : null,
        estimatedPrepTimeMinutes: this.prepTimeMinutes(),
      },
      storeId
    );
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCreditCardOutline,
  matAccountBalanceOutline,
  matPaymentsOutline,
  matSaveOutline,
  matLockOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matCreditCardOutline,
      matAccountBalanceOutline,
      matPaymentsOutline,
      matSaveOutline,
      matLockOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-settings-payments-tab',
  styleUrl: './settings-payments-tab.css',
  templateUrl: './settings-payments-tab.html',
})
export class SettingsPaymentsTab {
  private readonly settingsService = inject(SettingsService);

  readonly payments = computed(() => this.settingsService.settings().paymentMethods);

  // Form local signals
  readonly mpEnabled = signal<boolean>(true);
  readonly mpSandbox = signal<boolean>(true);
  readonly mpPublicKey = signal<string>('');

  readonly yapeEnabled = signal<boolean>(true);
  readonly yapePhone = signal<string>('');
  readonly yapeHolder = signal<string>('');
  readonly yapeInstructions = signal<string>('');

  readonly bankEnabled = signal<boolean>(false);
  readonly bankName = signal<string>('BCP');
  readonly bankAccount = signal<string>('');
  readonly bankCci = signal<string>('');
  readonly bankHolder = signal<string>('');

  readonly codEnabled = signal<boolean>(true);
  readonly codPos = signal<boolean>(false);

  constructor() {
    this.syncFromService();
  }

  private syncFromService(): void {
    const p = this.payments();
    this.mpEnabled.set(p.mercadoPago.enabled);
    this.mpSandbox.set(p.mercadoPago.sandboxMode);
    this.mpPublicKey.set(p.mercadoPago.publicKey || '');

    this.yapeEnabled.set(p.yapePlin.enabled);
    this.yapePhone.set(p.yapePlin.phoneNumber);
    this.yapeHolder.set(p.yapePlin.accountHolder);
    this.yapeInstructions.set(p.yapePlin.instructions);

    this.bankEnabled.set(p.bankTransfer.enabled);
    this.bankName.set(p.bankTransfer.bankName);
    this.bankAccount.set(p.bankTransfer.accountNumber);
    this.bankCci.set(p.bankTransfer.cci);
    this.bankHolder.set(p.bankTransfer.accountHolder);

    this.codEnabled.set(p.cashOnDelivery.enabled);
    this.codPos.set(p.cashOnDelivery.acceptsCardsOnDelivery);
  }

  onSave(): void {
    this.settingsService.updatePaymentMethods({
      mercadoPago: {
        enabled: this.mpEnabled(),
        sandboxMode: this.mpSandbox(),
        publicKey: this.mpPublicKey().trim(),
      },
      yapePlin: {
        enabled: this.yapeEnabled(),
        phoneNumber: this.yapePhone().trim(),
        accountHolder: this.yapeHolder().trim(),
        instructions: this.yapeInstructions().trim(),
      },
      bankTransfer: {
        enabled: this.bankEnabled(),
        bankName: this.bankName(),
        accountNumber: this.bankAccount().trim(),
        cci: this.bankCci().trim(),
        accountHolder: this.bankHolder().trim(),
      },
      cashOnDelivery: {
        enabled: this.codEnabled(),
        acceptsCardsOnDelivery: this.codPos(),
      },
    });
  }
}


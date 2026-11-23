import { Component, computed, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAccountBalanceWalletOutline,
  matPendingActionsOutline,
  matCheckCircleOutline,
  matAccountBalanceOutline,
  matInfoOutline,
  matArrowForwardOutline,
  matSyncOutline,
} from '@ng-icons/material-symbols/outline';
import { BillingService } from '../../../../core/services/billing.service';

@Component({
  selector: 'app-settlement-card',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matAccountBalanceWalletOutline,
      matPendingActionsOutline,
      matCheckCircleOutline,
      matAccountBalanceOutline,
      matInfoOutline,
      matArrowForwardOutline,
      matSyncOutline,
    }),
  ],
  templateUrl: './settlement-card.html',
})
export class SettlementCard {
  readonly billingService = inject(BillingService);

  readonly openRequestModal = output<void>();
  readonly openConfigDestination = output<void>();

  readonly availableBalance = computed(() => this.billingService.availableBalance());
  readonly lockedBalance = computed(() => this.billingService.lockedBalance());
  readonly totalPaidOut = computed(() => this.billingService.totalPaidOut());
  readonly minThreshold = computed(() => this.billingService.minThreshold());
  readonly destination = computed(() => this.billingService.payoutDestination());
  readonly canRequest = computed(() => this.billingService.canRequestPayout());
  readonly isRequesting = computed(() => this.billingService.isRequestingPayout());

  onRequestPayout(): void {
    if (this.canRequest()) {
      this.openRequestModal.emit();
    }
  }

  onConfigureAccount(): void {
    this.openConfigDestination.emit();
  }
}

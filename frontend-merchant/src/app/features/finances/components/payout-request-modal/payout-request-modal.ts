import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matAccountBalanceOutline,
  matCheckCircleOutline,
  matSyncOutline,
} from '@ng-icons/material-symbols/outline';
import { BillingService } from '../../../../core/services/billing.service';

@Component({
  selector: 'app-payout-request-modal',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matAccountBalanceOutline,
      matCheckCircleOutline,
      matSyncOutline,
    }),
  ],
  templateUrl: './payout-request-modal.html',
})
export class PayoutRequestModal {
  readonly billingService = inject(BillingService);

  readonly isOpen = input<boolean>(false);
  readonly closeModal = output<void>();

  readonly availableBalance = computed(() => this.billingService.availableBalance());
  readonly destination = computed(() => this.billingService.payoutDestination());
  readonly isSubmitting = computed(() => this.billingService.isRequestingPayout());

  onConfirm(): void {
    this.billingService.requestPayout().subscribe({
      next: () => {
        this.closeModal.emit();
      },
      error: () => {}, // Notificación gestionada en el servicio
    });
  }

  onClose(): void {
    if (!this.isSubmitting()) {
      this.closeModal.emit();
    }
  }
}

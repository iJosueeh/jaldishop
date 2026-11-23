import { Component, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matReceiptLongOutline,
  matCheckCircleOutline,
  matScheduleOutline,
  matErrorOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';
import { BillingService } from '../../../../core/services/billing.service';
import { PayoutBatchStatus } from '../../../../core/models/billing.models';

@Component({
  selector: 'app-payout-batches-table',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matReceiptLongOutline,
      matCheckCircleOutline,
      matScheduleOutline,
      matErrorOutline,
      matRefreshOutline,
    }),
  ],
  templateUrl: './payout-batches-table.html',
})
export class PayoutBatchesTable implements OnInit {
  readonly billingService = inject(BillingService);

  readonly batches = computed(() => this.billingService.batches());
  readonly isLoading = computed(() => this.billingService.isBatchesLoading());

  ngOnInit(): void {
    if (this.batches().length === 0) {
      this.billingService.loadBatches().subscribe();
    }
  }

  refresh(): void {
    this.billingService.loadBatches().subscribe();
  }

  getStatusBadgeClass(status: PayoutBatchStatus): string {
    switch (status) {
      case 'PAID':
        return 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20';
      case 'PROCESSING':
      case 'PENDING':
        return 'bg-blue-500/10 text-blue-600 border-blue-500/20';
      case 'FAILED':
      case 'CANCELLED':
        return 'bg-rose-500/10 text-rose-600 border-rose-500/20';
      default:
        return 'bg-stone-500/10 text-stone-600 border-stone-500/20';
    }
  }

  getStatusLabel(status: PayoutBatchStatus): string {
    switch (status) {
      case 'PAID':
        return 'Pagado';
      case 'PROCESSING':
        return 'En Proceso';
      case 'PENDING':
        return 'Pendiente';
      case 'FAILED':
        return 'Fallido';
      case 'CANCELLED':
        return 'Cancelado';
      default:
        return status;
    }
  }
}

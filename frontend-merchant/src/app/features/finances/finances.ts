import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matErrorOutline, matRefreshOutline } from '@ng-icons/material-symbols/outline';
import { AnalyticsService } from '../../core/services/analytics.service';
import { BillingService } from '../../core/services/billing.service';
import { FinancesHeader } from './components/finances-header/finances-header';
import { FinancesKpis } from './components/finances-kpis/finances-kpis';
import { TopProductsCard } from './components/top-products-card/top-products-card';
import { CommercialBreakdown } from './components/commercial-breakdown/commercial-breakdown';
import { SettlementCard } from './components/settlement-card/settlement-card';
import { PayoutBatchesTable } from './components/payout-batches-table/payout-batches-table';
import { PayoutRequestModal } from './components/payout-request-modal/payout-request-modal';
import { PayoutDestinationModal } from './components/payout-destination-modal/payout-destination-modal';

@Component({
  selector: 'app-finances',
  standalone: true,
  imports: [
    CommonModule,
    NgIcon,
    FinancesHeader,
    FinancesKpis,
    TopProductsCard,
    CommercialBreakdown,
    SettlementCard,
    PayoutBatchesTable,
    PayoutRequestModal,
    PayoutDestinationModal,
  ],
  providers: [
    provideIcons({
      matErrorOutline,
      matRefreshOutline,
    }),
  ],
  templateUrl: './finances.html',
  styleUrl: './finances.css',
})
export class Finances implements OnInit {
  private readonly router = inject(Router);
  readonly analyticsService = inject(AnalyticsService);
  readonly billingService = inject(BillingService);

  readonly isPayoutModalOpen = signal<boolean>(false);
  readonly isDestinationModalOpen = signal<boolean>(false);

  readonly isLoading = computed(() => this.analyticsService.isLoading());
  readonly error = computed(() => this.analyticsService.error());
  readonly summary = computed(() => this.analyticsService.summary());

  ngOnInit(): void {
    if (!this.summary() || this.analyticsService.isOutdated()) {
      this.analyticsService.loadSummary().subscribe({
        error: () => {},
      });
    }

    if (!this.billingService.summary()) {
      this.billingService.loadSummary().subscribe({
        error: () => {},
      });
    }
  }

  retry(): void {
    this.analyticsService.loadSummary().subscribe({ error: () => {} });
    this.billingService.loadSummary().subscribe({ error: () => {} });
  }

  onDestinationModalClosed(): void {
    this.isDestinationModalOpen.set(false);
    this.billingService.loadSummarySilently();
  }

  navigateToSettings(): void {
    this.router.navigate(['/settings'], { queryParams: { tab: 'payments' } });
  }
}

import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAccountBalanceWalletOutline,
  matPaymentsOutline,
  matAnalyticsOutline,
  matTrendingDownOutline,
} from '@ng-icons/material-symbols/outline';
import { AnalyticsService } from '../../../../core/services/analytics.service';

@Component({
  selector: 'app-finances-kpis',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matAccountBalanceWalletOutline,
      matPaymentsOutline,
      matAnalyticsOutline,
      matTrendingDownOutline,
    }),
  ],
  template: `
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 1. Ingresos Netos de Pasarela -->
      <div class="p-4 rounded-2xl bg-card border border-border shadow-2xs flex flex-col justify-between gap-3 relative overflow-hidden group card-interactive">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Ingresos Netos (Pasarela)</span>
          <div class="w-8 h-8 rounded-xl bg-emerald-500/10 text-emerald-800 flex items-center justify-center">
            <ng-icon name="matAccountBalanceWalletOutline" class="text-lg" />
          </div>
        </div>

        <div>
          <div class="text-2xl font-black tracking-tight text-foreground font-mono tabular-nums">
            {{ formatCurrency(summary()?.financial?.netFinancialInflow) }}
          </div>
          <div class="flex items-center gap-1.5 mt-1 text-xs text-muted-foreground">
            <span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 live-indicator"></span>
            <span class="tabular-nums">{{ summary()?.financial?.approvedPaymentsCount || 0 }} pagos aprobados</span>
          </div>
        </div>
      </div>

      <!-- 2. Ventas Brutas de Productos -->
      <div class="p-4 rounded-2xl bg-card border border-border shadow-2xs flex flex-col justify-between gap-3 relative overflow-hidden group card-interactive">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Ventas Brutas Productos</span>
          <div class="w-8 h-8 rounded-xl bg-blue-500/10 text-blue-800 flex items-center justify-center">
            <ng-icon name="matPaymentsOutline" class="text-lg" />
          </div>
        </div>

        <div>
          <div class="text-2xl font-black tracking-tight text-foreground font-mono tabular-nums">
            {{ formatCurrency(summary()?.commercial?.productsGrossSales) }}
          </div>
          <div class="flex items-center gap-1.5 mt-1 text-xs text-muted-foreground">
            <span class="inline-block w-1.5 h-1.5 rounded-full bg-blue-500"></span>
            <span class="tabular-nums">{{ summary()?.commercial?.completedOrdersCount || 0 }} pedidos completados</span>
          </div>
        </div>
      </div>

      <!-- 3. Ticket Promedio -->
      <div class="p-4 rounded-2xl bg-card border border-border shadow-2xs flex flex-col justify-between gap-3 relative overflow-hidden group card-interactive">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Ticket Promedio</span>
          <div class="w-8 h-8 rounded-xl bg-purple-500/10 text-purple-800 flex items-center justify-center">
            <ng-icon name="matAnalyticsOutline" class="text-lg" />
          </div>
        </div>

        <div>
          <div class="text-2xl font-black tracking-tight text-foreground font-mono tabular-nums">
            {{ formatCurrency(summary()?.commercial?.averageTicket) }}
          </div>
          <div class="flex items-center gap-1.5 mt-1 text-xs text-muted-foreground">
            <span class="inline-block w-1.5 h-1.5 rounded-full bg-purple-500"></span>
            <span>Por pedido entregado</span>
          </div>
        </div>
      </div>

      <!-- 4. Reembolsos Liquidados -->
      <div class="p-4 rounded-2xl bg-card border border-border shadow-2xs flex flex-col justify-between gap-3 relative overflow-hidden group card-interactive">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Reembolsos Liquidados</span>
          <div class="w-8 h-8 rounded-xl bg-rose-500/10 text-rose-800 flex items-center justify-center">
            <ng-icon name="matTrendingDownOutline" class="text-lg" />
          </div>
        </div>

        <div>
          <div class="text-2xl font-black tracking-tight text-foreground font-mono tabular-nums">
            {{ formatCurrency(summary()?.financial?.settledRefundsAmount) }}
          </div>
          <div class="flex items-center gap-1.5 mt-1 text-xs text-muted-foreground">
            <span class="inline-block w-1.5 h-1.5 rounded-full bg-rose-500"></span>
            <span class="tabular-nums">{{ summary()?.financial?.settledRefundsCount || 0 }} devoluciones resueltas</span>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class FinancesKpis {
  private readonly analyticsService = inject(AnalyticsService);

  readonly summary = computed(() => this.analyticsService.summary());

  formatCurrency(amount?: number | null): string {
    if (amount === undefined || amount === null) return 'S/ 0.00';
    return `S/ ${Number(amount).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}

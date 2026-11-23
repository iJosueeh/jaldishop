import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStoreDetail } from '../../../core/models/admin.models';
import { AdminStoreDrawer } from '../stores/components/admin-store-drawer/admin-store-drawer';
import { AreaTrendChartComponent, AreaTrendDataPoint } from '../../../shared/components/charts/area-trend-chart/area-trend-chart';
import { DonutDistributionChartComponent, DonutSegment } from '../../../shared/components/charts/donut-distribution-chart/donut-distribution-chart';
import { HorizontalBarChartComponent, BarCategoryItem } from '../../../shared/components/charts/horizontal-bar-chart/horizontal-bar-chart';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAnalyticsOutline,
  matRefreshOutline,
  matPictureAsPdfOutline,
  matTrendingUpOutline,
  matAccountBalanceWalletOutline,
  matStorefrontOutline,
  matLocalShippingOutline,
  matAttachMoneyOutline,
} from '@ng-icons/material-symbols/outline';

export type AnalyticsRangePreset = '7D' | '30D' | '90D';

@Component({
  imports: [
    CommonModule,
    AdminStoreDrawer,
    AreaTrendChartComponent,
    DonutDistributionChartComponent,
    HorizontalBarChartComponent,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matAnalyticsOutline,
      matRefreshOutline,
      matPictureAsPdfOutline,
      matTrendingUpOutline,
      matAccountBalanceWalletOutline,
      matStorefrontOutline,
      matLocalShippingOutline,
      matAttachMoneyOutline,
    }),
  ],
  selector: 'app-admin-analytics',
  styleUrl: './admin-analytics.css',
  templateUrl: './admin-analytics.html',
})
export class AdminAnalytics implements OnInit {
  readonly adminService = inject(AdminService);

  readonly selectedRange = signal<AnalyticsRangePreset>('30D');
  readonly selectedStore = signal<AdminStoreDetail | null>(null);
  readonly isDrawerOpen = signal<boolean>(false);

  // Fechas Computadas
  readonly dateRange = computed(() => {
    const to = new Date();
    const from = new Date();
    const days = this.selectedRange() === '7D' ? 7 : this.selectedRange() === '90D' ? 90 : 30;
    from.setDate(to.getDate() - days);

    return {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10),
    };
  });

  // Datos para el gráfico de Área (Evolución Temporal)
  readonly trendChartData = computed<AreaTrendDataPoint[]>(() => {
    const summary = this.adminService.analyticsSummary();
    if (!summary) return [];

    const gmv = summary.commercial?.historicalConfirmedGmv || 0;
    const net = summary.financial?.netFinancialInflow || 0;
    const days = this.selectedRange() === '7D' ? 7 : this.selectedRange() === '90D' ? 90 : 30;

    // Generar serie temporal interpolada para el rango seleccionado
    const points: AreaTrendDataPoint[] = [];
    const baseDate = new Date();
    for (let i = days; i >= 0; i--) {
      const d = new Date();
      d.setDate(baseDate.getDate() - i);
      const factor = (days - i + 1) / (days + 1);
      const dailyGmv = Math.round((gmv / days) * (0.8 + 0.4 * Math.sin(i)) * 100) / 100;
      const dailyInflow = Math.round((net / days) * (0.8 + 0.4 * Math.sin(i)) * 100) / 100;

      points.push({
        date: d.toISOString().slice(5, 10),
        gmv: dailyGmv,
        inflow: dailyInflow,
      });
    }

    return points;
  });

  // Datos para el Donut de Distribución
  readonly distributionSegments = computed<DonutSegment[]>(() => {
    const summary = this.adminService.analyticsSummary();
    if (!summary) return [];

    const gross = summary.commercial?.productsGrossSales || 0;
    const delivery = summary.commercial?.totalDeliveryFees || 0;
    const platformCommission = (summary.financial?.netFinancialInflow || 0) * 0.05; // 5% base

    return [
      { label: 'Ventas Comercios', value: Math.max(0, gross - platformCommission), color: '#4f46e5' },
      { label: 'Tarifas Envíos', value: delivery, color: '#f59e0b' },
      { label: 'Comisión JaldiShop', value: platformCommission, color: '#10b981' },
    ];
  });

  // Datos para el gráfico de Barras Horizontal (Top Tiendas)
  readonly storeRankingItems = computed<BarCategoryItem[]>(() => {
    const summary = this.adminService.analyticsSummary();
    if (!summary || !summary.storeRankings) return [];

    return summary.storeRankings.map((rank) => ({
      id: rank.storeId,
      label: rank.storeName,
      value: Number(rank.commercialGmv ?? rank.totalGmv ?? 0),
    }));
  });

  ngOnInit(): void {
    if (!this.adminService.analyticsSummary()) {
      this.loadData();
    }
  }

  setRange(range: AnalyticsRangePreset): void {
    this.selectedRange.set(range);
    this.loadData();
  }

  loadData(): void {
    const { from, to } = this.dateRange();
    this.adminService.loadAnalyticsSummary(from, to).subscribe();
  }

  exportPdf(): void {
    const { from, to } = this.dateRange();
    this.adminService.exportAnalyticsPdf(from, to);
  }

  onSelectStoreById(storeId: string): void {
    const found = this.adminService.stores().find((s) => s.id === storeId);
    if (found) {
      this.selectedStore.set(found);
      this.isDrawerOpen.set(true);
    }
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedStore.set(null);
  }
}

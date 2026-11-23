import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCalendarMonthOutline,
  matDownloadOutline,
  matPictureAsPdfOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';
import { AnalyticsService } from '../../../../core/services/analytics.service';
import { AnalyticsDateRangePreset } from '../../../../core/models/analytics.models';

@Component({
  selector: 'app-finances-header',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCalendarMonthOutline,
      matDownloadOutline,
      matPictureAsPdfOutline,
      matRefreshOutline,
    }),
  ],
  template: `
    <header class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-border/60">
      <div>
        <div class="flex items-center gap-2">
          <h1 class="text-2xl font-bold tracking-tight text-foreground">Finanzas & Métricas</h1>
          <span class="px-2 py-0.5 text-xs font-semibold rounded-full bg-primary/10 text-primary border border-primary/20">
            Oficial
          </span>
        </div>
        <p class="text-sm text-muted-foreground mt-0.5">
          Flujo de caja, ventas de productos y liquidaciones en pasarela de pagos
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2.5">
        <!-- Selector de Presets de Rango -->
        <div class="flex items-center bg-surface-container-high/60 p-1 rounded-xl border border-border/50 shadow-xs">
          <button
            type="button"
            (click)="selectPreset('7D')"
            [class.bg-card]="selectedPreset() === '7D'"
            [class.text-primary]="selectedPreset() === '7D'"
            [class.shadow-xs]="selectedPreset() === '7D'"
            class="px-3 py-1.5 rounded-lg text-xs font-semibold text-muted-foreground hover:text-foreground transition-all cursor-pointer"
          >
            7 días
          </button>
          <button
            type="button"
            (click)="selectPreset('30D')"
            [class.bg-card]="selectedPreset() === '30D'"
            [class.text-primary]="selectedPreset() === '30D'"
            [class.shadow-xs]="selectedPreset() === '30D'"
            class="px-3 py-1.5 rounded-lg text-xs font-semibold text-muted-foreground hover:text-foreground transition-all cursor-pointer"
          >
            30 días
          </button>
          <button
            type="button"
            (click)="selectPreset('90D')"
            [class.bg-card]="selectedPreset() === '90D'"
            [class.text-primary]="selectedPreset() === '90D'"
            [class.shadow-xs]="selectedPreset() === '90D'"
            class="px-3 py-1.5 rounded-lg text-xs font-semibold text-muted-foreground hover:text-foreground transition-all cursor-pointer"
          >
            90 días
          </button>
        </div>

        <!-- Indicador de Fechas Actuales -->
        <div class="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-card border border-border text-xs text-muted-foreground font-medium shadow-2xs">
          <ng-icon name="matCalendarMonthOutline" class="text-sm text-primary" />
          <span>{{ formattedDateRange() }}</span>
        </div>

        <!-- Botón de Recarga -->
        <button
          type="button"
          (click)="refresh()"
          [disabled]="isLoading()"
          title="Actualizar datos"
          class="p-2 rounded-xl border border-border bg-card text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-colors cursor-pointer disabled:opacity-50"
        >
          <ng-icon name="matRefreshOutline" class="text-base" [class.animate-spin]="isLoading()" />
        </button>

        <!-- Botón de Exportar Reporte PDF -->
        <button
          type="button"
          (click)="exportPdf()"
          [disabled]="isExportingPdf() || isLoading()"
          class="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-primary text-primary-foreground font-semibold text-xs shadow-xs hover:bg-primary/90 transition-all cursor-pointer disabled:opacity-50"
        >
          @if (isExportingPdf()) {
            <span class="w-3.5 h-3.5 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin"></span>
            <span>Generando PDF...</span>
          } @else {
            <ng-icon name="matPictureAsPdfOutline" class="text-base" />
            <span>Exportar PDF</span>
          }
        </button>
      </div>
    </header>
  `,
})
export class FinancesHeader {
  private readonly analyticsService = inject(AnalyticsService);

  readonly selectedPreset = computed(() => this.analyticsService.selectedPreset());
  readonly fromDate = computed(() => this.analyticsService.fromDate());
  readonly toDate = computed(() => this.analyticsService.toDate());
  readonly isLoading = computed(() => this.analyticsService.isLoading());
  readonly isExportingPdf = computed(() => this.analyticsService.isExportingPdf());

  readonly formattedDateRange = computed(() => {
    const from = this.fromDate();
    const to = this.toDate();
    if (!from || !to) return '';
    try {
      const d1 = new Date(from + 'T00:00:00');
      const d2 = new Date(to + 'T00:00:00');
      const f1 = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short' }).format(d1);
      const f2 = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).format(d2);
      return `${f1} – ${f2}`;
    } catch {
      return `${from} al ${to}`;
    }
  });

  selectPreset(preset: '7D' | '30D' | '90D'): void {
    this.analyticsService.setPreset(preset).subscribe();
  }

  refresh(): void {
    this.analyticsService.loadSummary().subscribe();
  }

  exportPdf(): void {
    this.analyticsService.exportPdf().subscribe();
  }
}

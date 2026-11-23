import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule, ApexNonAxisChartSeries, ApexChart, ApexResponsive, ApexLegend, ApexTooltip, ApexPlotOptions } from 'ng-apexcharts';

export interface DonutSegment {
  label: string;
  value: number;
  color?: string;
}

@Component({
  selector: 'app-donut-distribution-chart',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  template: `
    <div class="w-full h-full min-h-[260px] flex items-center justify-center relative">
      @if (series().length > 0 && totalValue() > 0) {
        <apx-chart
          [series]="series()"
          [chart]="chartOptions()"
          [labels]="labels()"
          [colors]="colors()"
          [plotOptions]="plotOptions()"
          [legend]="legendOptions()"
          [tooltip]="tooltipOptions()"
          [responsive]="responsiveOptions()"
        />
      } @else {
        <div class="flex items-center justify-center h-48 text-on-surface-variant text-xs">
          No hay datos para representar la distribución.
        </div>
      }
    </div>
  `,
})
export class DonutDistributionChartComponent {
  readonly segments = input<DonutSegment[]>([]);
  readonly title = input<string>('Total');
  readonly currencyPrefix = input<string>('S/ ');

  readonly labels = computed(() => this.segments().map((s) => s.label));
  readonly series = computed<number[]>(() => this.segments().map((s) => s.value));
  readonly colors = computed(() => {
    const custom = this.segments().map((s) => s.color).filter(Boolean) as string[];
    return custom.length === this.segments().length
      ? custom
      : ['#4f46e5', '#10b981', '#f59e0b', '#ec4899', '#6366f1'];
  });

  readonly totalValue = computed<number>(() => this.series().reduce((acc, curr) => acc + curr, 0));

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'donut',
    height: 260,
    fontFamily: 'inherit',
    background: 'transparent',
    animations: {
      enabled: true,
      speed: 350,
    },
  }));

  readonly plotOptions = computed<ApexPlotOptions>(() => ({
    pie: {
      donut: {
        size: '72%',
        labels: {
          show: true,
          name: {
            show: true,
            fontSize: '12px',
            fontFamily: 'inherit',
            color: '#64748b',
          },
          value: {
            show: true,
            fontSize: '16px',
            fontFamily: 'monospace',
            fontWeight: 800,
            color: '#0f172a',
            formatter: (val: string) =>
              `${this.currencyPrefix()}${parseFloat(val).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
          },
          total: {
            show: true,
            label: this.title(),
            color: '#64748b',
            formatter: () =>
              `${this.currencyPrefix()}${this.totalValue().toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
          },
        },
      },
    },
  }));

  readonly legendOptions = computed<ApexLegend>(() => ({
    position: 'bottom',
    horizontalAlign: 'center',
    labels: { colors: '#334155' },
    fontFamily: 'inherit',
    fontSize: '12px',
    fontWeight: 500,
    markers: { shape: 'circle' },
  }));

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: 'dark',
    y: {
      formatter: (val: number) =>
        `${this.currencyPrefix()}${val.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
  }));

  readonly responsiveOptions = computed<ApexResponsive[]>(() => [
    {
      breakpoint: 480,
      options: {
        chart: { width: 280 },
        legend: { position: 'bottom' },
      },
    },
  ]);
}

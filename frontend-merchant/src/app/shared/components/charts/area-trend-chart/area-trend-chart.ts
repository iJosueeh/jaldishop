import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule, ApexAxisChartSeries, ApexChart, ApexXAxis, ApexStroke, ApexTooltip, ApexDataLabels, ApexFill, ApexYAxis } from 'ng-apexcharts';

export interface AreaTrendDataPoint {
  date: string;
  gmv: number;
  inflow?: number;
}

@Component({
  selector: 'app-area-trend-chart',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  template: `
    <div class="w-full h-full min-h-[260px] relative">
      @if (series().length > 0 && categories().length > 0) {
        <apx-chart
          [series]="series()"
          [chart]="chartOptions()"
          [xaxis]="xaxisOptions()"
          [yaxis]="yaxisOptions()"
          [stroke]="strokeOptions()"
          [fill]="fillOptions()"
          [tooltip]="tooltipOptions()"
          [dataLabels]="dataLabelsOptions()"
        />
      } @else {
        <div class="flex items-center justify-center h-48 text-on-surface-variant text-xs">
          No hay datos suficientes para graficar el período seleccionado.
        </div>
      }
    </div>
  `,
})
export class AreaTrendChartComponent {
  readonly data = input<AreaTrendDataPoint[]>([]);
  readonly currencyPrefix = input<string>('S/ ');

  readonly categories = computed(() => this.data().map((d) => d.date));

  readonly series = computed<ApexAxisChartSeries>(() => {
    const d = this.data();
    if (!d || d.length === 0) return [];

    const seriesList: ApexAxisChartSeries = [
      {
        name: 'GMV Bruto',
        data: d.map((item) => item.gmv),
        color: '#4f46e5', // indigo-600
      },
    ];

    if (d.some((item) => item.inflow !== undefined)) {
      seriesList.push({
        name: 'Ingreso Plataforma',
        data: d.map((item) => item.inflow ?? 0),
        color: '#10b981', // emerald-500
      });
    }

    return seriesList;
  });

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'area',
    height: 260,
    fontFamily: 'inherit',
    toolbar: { show: false },
    animations: {
      enabled: true,
      speed: 350,
      animateGradually: { enabled: true },
    },
    background: 'transparent',
  }));

  readonly strokeOptions = computed<ApexStroke>(() => ({
    curve: 'smooth',
    width: 2.5,
  }));

  readonly fillOptions = computed<ApexFill>(() => ({
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.35,
      opacityTo: 0.05,
      stops: [0, 95, 100],
    },
  }));

  readonly dataLabelsOptions = computed<ApexDataLabels>(() => ({
    enabled: false,
  }));

  readonly xaxisOptions = computed<ApexXAxis>(() => ({
    categories: this.categories(),
    labels: {
      style: {
        fontSize: '11px',
        colors: '#64748b',
        fontFamily: 'monospace',
      },
    },
    axisBorder: { show: false },
    axisTicks: { show: false },
  }));

  readonly yaxisOptions = computed<ApexYAxis>(() => ({
    labels: {
      formatter: (val: number) => `${this.currencyPrefix()}${val.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`,
      style: {
        fontSize: '11px',
        colors: '#64748b',
        fontFamily: 'monospace',
      },
    },
  }));

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: 'dark',
    x: { show: true },
    y: {
      formatter: (val: number) => `${this.currencyPrefix()}${val.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
  }));
}
